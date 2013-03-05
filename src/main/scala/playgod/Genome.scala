package playgod.refactoring

import concurrent.{Await, Promise}
import concurrent.duration.Duration
import org.jbox2d.dynamics._
import joints.{RevoluteJoint, RevoluteJointDef}
import playgod.Box2DTools._
import playgod.Box2DTools.MathHelpers._
import org.jbox2d.collision.shapes.PolygonShape
import org.jbox2d.common.MathUtils._
import collection.{mutable, SortedMap, MapProxy, SeqProxy}
import org.encog.neural.networks.BasicNetwork
import org.encog.neural.networks.layers.BasicLayer
import org.encog.engine.network.activation.ActivationTANH
import playgod.RandomTools._

class GenomeBuilder extends mutable.MapProxy[String, ChromosomeBuilder] {
  val self = new mutable.HashMap[String,ChromosomeBuilder] {
    override def default(key:String) = {
      val newChromosome = new NamedChromosomeBuilder()
      this += (key -> newChromosome)
      newChromosome
    }
  }

  def update(key:String, value:IndexedSeq[Double]) = {
    val newChromosome = new PlainChromosomeBuilder()
    newChromosome.genes = value
    this += (key -> newChromosome)
  }

  def apply[C <: ChromosomeBuilder](key:String) = super.apply(key).asInstanceOf[C]
  def toGenome = new Genome(SortedMap.empty[String,Chromosome] ++ self.map{ case (k,v) => (k, v.toChromosome) } )
}

abstract class Chromosome {
  def genes:IndexedSeq[Double]
  def size:Int
  def update(newGenes:IndexedSeq[Double]):Chromosome
}

abstract class ChromosomeBuilder {
  def toChromosome:Chromosome
}

class PlainChromosomeBuilder extends ChromosomeBuilder {
  var genes:IndexedSeq[Double] = null
  def toChromosome = new PlainChromosome(genes)
}

class PlainChromosome(val genes:IndexedSeq[Double]) extends Chromosome with SeqProxy[Double] {
  def self = genes
  def update(newGenes:IndexedSeq[Double]) = {
    require(newGenes.size == genes.size)
    new PlainChromosome(newGenes)
  }
}

class NamedChromosomeBuilder extends ChromosomeBuilder with mutable.MapProxy[String,Double] {
  val self = new mutable.HashMap[String,Double]
  def toChromosome = new NamedChromosome(SortedMap.empty[String,Double] ++ self)
}

class NamedChromosome(val namedGenes:SortedMap[String,Double]) extends Chromosome with MapProxy[String,Double] {
  def self = namedGenes
  def genes = namedGenes.values.toIndexedSeq
  def update(newGenes:IndexedSeq[Double]) = {
    require(newGenes.size == namedGenes.size)
    val newNamedGenes = SortedMap.newBuilder[String,Double]
    for ( ((name,oldGene),i) <- namedGenes.zipWithIndex )
      newNamedGenes += (name -> newGenes(i))
    new NamedChromosome(newNamedGenes.result())
  }
}

class Genome(val chromosomes:SortedMap[String,Chromosome]) extends MapProxy[String,Chromosome] {
  def self = chromosomes
  lazy val genes:IndexedSeq[Double] = chromosomes.values.flatMap(_.genes).toIndexedSeq

  def apply[C <: Chromosome](key:String) = super.apply(key).asInstanceOf[C]
  def update(newGenes:IndexedSeq[Double]):Genome = {
    require(newGenes.size == chromosomes.values.map(_.size).sum)

    val newChromosomes = SortedMap.newBuilder[String,Chromosome]
    var i = 0
    for( (key,chromosome) <- chromosomes ) {
      newChromosomes += (key -> chromosome.update(newGenes.slice(i,i+chromosome.size)))
      i += chromosome.size
    }
    new Genome(newChromosomes.result())
  }

  def mutate(probability:Double, strength:Double) = {
    val newGenes = genes.toArray
    for( (gene,i) <- newGenes.zipWithIndex ) {
      if( inCase(probability) )
        newGenes(i) = gene + rGaussian*strength
    }
    update(newGenes)
  }

  def crossover(that:Genome) = {
    val offspringGenesA = this.genes.toArray
    val offspringGenesB = that.genes.toArray
    for( i <- 0 until size )
      if( inCase(0.5) ) {
        offspringGenesA(i) = that.genes(i)
        offspringGenesB(i) = this.genes(i)
      }
    (update(offspringGenesA), update(offspringGenesB))
  }

  var isElite = false
}

abstract class Organism {
  def genome:Genome
  private val fitnessPromise = Promise[Double]()
  def fitness:Double = Await.result(fitnessPromise.future, Duration.Inf)
  def fitness_=(value:Double) = fitnessPromise.success(value)
}

abstract class Creature {
  var genome:Genome
  def create:Organism
  def create(n:Int):Seq[Organism]
}


abstract class SimulationOrganism extends Organism {
  def reward = 0.0
  def penalty = 0.0
  def simulationStep()

  val maxSteps = 500
  private var age = 0
  private var score = 0.0

  def step() {
    if ( age < maxSteps ) {
      simulationStep()
      age += 1
      score += reward - penalty

      if( age >= maxSteps )
        fitness = score / age
    }
  }
  
  def finish() {
    while( age < maxSteps )
      step()
  }
}

abstract class Box2DSimulationOrganism extends SimulationOrganism {
  val timeStep = 1f / 60f
  val world = new World(new Vec2(0, -9.81f), true)
  world.setDebugDraw(DebugDrawer)

  def simulationStep() { world.step(timeStep, 10, 10) }
  def debugDraw() { world.drawDebugData() }
}


abstract class Bone(val world:World,
                    val length:Float,
                    val thickness:Float,
                    val angle:Float,
                    val pos:Vec2) {
  val width = length
  val height = thickness

  val endA = pos - Vec2(cos(angle),sin(angle))*(width*0.5f)
  val endB = pos + Vec2(cos(angle),sin(angle))*(width*0.5f)
  //assert(((endA-endB).length - width).abs < 0.1, (endA-endB).length + " == " + width)
  def pos(t:Float):Vec2 = endA*(1-t) + endB*t

  val density = 1f
  val friction = 1f
  val center = Vec2(0,0)
  val collisionGroupIndex = -1

  val bodyDef = new BodyDef
  if (density != 0f) bodyDef.`type` = BodyType.DYNAMIC
  bodyDef.active = true
  bodyDef.position.set(pos)

  val fixtureDef = new FixtureDef
  private val dynamicBox = new PolygonShape
  dynamicBox.setAsBox(width*0.5f, height*0.5f, center, angle)
  fixtureDef.shape = dynamicBox
  fixtureDef.density = density
  fixtureDef.friction = friction
  fixtureDef.filter.groupIndex = collisionGroupIndex

  val body = world.createBody(bodyDef)
  body.createFixture(fixtureDef)
}

class RootBone(world:World, length:Float, thickness:Float, pos:Vec2, angle:Float)
    extends Bone(world, length, thickness, angle, pos) {
  def this(world:World, length:Double, thickness:Double, pos:Vec2, angle:Double) = {
    this(world, length.toFloat, thickness.toFloat, pos, angle.toFloat)
  }
}
class JointBone(world:World, length:Float, thickness:Float, parent:Bone, jointAttach:Float, restAngle:Float,
                maxMotorTorque:Float = 5000f, maxMotorSpeed:Float = 3f)
    extends Bone(world, length, thickness, (parent.angle + restAngle),
      parent.pos(jointAttach) + Vec2(cos(parent.angle + restAngle),
                                     sin(parent.angle + restAngle))*(length*0.5f)) {
  def this(world:World, length:Double, thickness:Double, parent:Bone, jointAttach:Double, restAngle:Double,
           maxMotorTorque:Double, maxMotorSpeed:Double) = {
    this(world, length.toFloat, thickness.toFloat, parent, jointAttach.toFloat, restAngle.toFloat,
         maxMotorTorque.toFloat, maxMotorSpeed.toFloat)
  }

  val jointPos = parent.pos(jointAttach.toFloat)
  //assert((endA - jointPos).length < 0.1f, endA.toString + " == " + jointPos)

  val jointDef = new RevoluteJointDef
  jointDef.motorSpeed = 0f
  jointDef.maxMotorTorque = maxMotorTorque
  jointDef.enableMotor = true
  //assert(body != null)
  jointDef.initialize(body, parent.body, jointPos)
  val joint = world.createJoint(jointDef).asInstanceOf[RevoluteJoint]

  var angleTarget = joint.getJointAngle
  def counterSpeed(error:Float) = math.tanh(error).toFloat*maxMotorSpeed
  def update() {
    val angleError = angleTarget - joint.getJointAngle
    // only set motorSpeed when necessary
    // => allows deactivation
    if( angleError.abs > 0.001f )
      joint.setMotorSpeed(counterSpeed(angleError))
  }
}

class Brain(val inputs:Array[Sensor], val outputs:Array[Effector], val initialWeights:Option[Array[Double]] = None) {
  val network = new BasicNetwork()
  network.addLayer(new BasicLayer(null,false, inputs.size))
  network.addLayer(new BasicLayer(new ActivationTANH(),true,inputs.size))
  network.addLayer(new BasicLayer(new ActivationTANH(),true,inputs.size))
  network.addLayer(new BasicLayer(new ActivationTANH(),true,outputs.size))
  network.getStructure.finalizeStructure()
  val randomizer = new org.encog.mathutil.randomize.GaussianRandomizer(0,1)

  if( initialWeights.isDefined )
    replaceWeights(initialWeights.get)

  def weightCount = network.encodedArrayLength
  def getWeights = {
    val weights = new Array[Double](weightCount)
    network.encodeToArray(weights)
    weights.clone()
  }

  def replaceWeights(weights:Array[Double]) {
    network.decodeFromArray(weights)
  }

  def randomizeWeights() {
    randomizer.randomize(network)
  }

  def update() {
    val inputValues = inputs.map(_.getValue)
    val outputValues = new Array[Double](outputs.size)
    network.compute(inputValues, outputValues)
    for( (effector,param) <- outputs zip outputValues )
      effector.act(param)
  }
}

abstract class Sensor {
  def getValue:Double
}

object Sensor {
  def apply( f: => Double ) = new Sensor {
    override def getValue = f
  }
}

abstract class Effector {
  def act(param:Double)
}

object Effector {
  def apply( f: Double => Unit ) = new Effector {
    override def act(param:Double) { f(param) }
  }
}


class Box2DCreature extends Creature {
  val gb = new GenomeBuilder
  /*gb[NamedChromosomeBuilder]("skeleton")("hipLength") = 0.5
  gb[NamedChromosomeBuilder]("skeleton")("backLength") = 0.5
  gb[NamedChromosomeBuilder]("skeleton")("legLength") = 0.5
  gb[NamedChromosomeBuilder]("skeleton")("footLength") = 0.5
  gb[NamedChromosomeBuilder]("skeleton")("backMotorTorque") = 1
  gb[NamedChromosomeBuilder]("skeleton")("legMotorTorque") = 1
  gb[NamedChromosomeBuilder]("skeleton")("footMotorTorque") = 1
  gb[NamedChromosomeBuilder]("skeleton")("backMotorSpeed") = 1
  gb[NamedChromosomeBuilder]("skeleton")("legMotorSpeed") = 1
  gb[NamedChromosomeBuilder]("skeleton")("footMotorSpeed") = 1
  //gb[NamedChromosomeBuilder]("skeleton")("backRestAngle") = 0.5
  //gb[NamedChromosomeBuilder]("skeleton")("legRestAngle") = 0.5 //TODO: implement jointbones with inverse angle
  gb[NamedChromosomeBuilder]("skeleton")("footRestAngle") = 0*/
  
  val sensorCount = 7
  val boneCount = 12
  val dummyBrain = new Brain(new Array[Sensor](sensorCount*boneCount), new Array[Effector](boneCount -1))
  gb("brain") = dummyBrain.getWeights

  var genome = gb.toGenome
  var maxSimulationSteps = 500
  def currentGenome = genome
  
  println("genes: %d (%d synapses)" format(genome.genes.size, genome[PlainChromosome]("brain").size))
  
  def create(n:Int) = {
    for( _ <- 0 until n) yield {
      dummyBrain.randomizeWeights()
      gb("brain") = dummyBrain.getWeights
      genome = gb.toGenome
      create
    }
  }

  def create = new Box2DSimulationOrganism {
    override val maxSteps = maxSimulationSteps
    val genome = currentGenome
    //val sk = genome[NamedChromosome]("skeleton")
    val brainGenes = genome[PlainChromosome]("brain")

    val ground = createBox(world, pos = new Vec2(0, -3), width = 1000, height = 2, density = 0f, friction = 1f)
    val obstacle = createBox(world, pos = new Vec2(50, -3), width = 2, height = 8, density = 0f, friction = 1f)

/*    val hipBone   = new RootBone(world, length = sk("hipLength").abs*10, thickness = 1, pos = Vec2(0,(sk("hipLength")+sk("backLength")+sk("legLength")+sk("footLength"))*10), angle = 0.0)
    val backBone  = new JointBone(world, length = sk("backLength").abs*10, thickness = 1, parent = hipBone, jointAttach = 0.5f, restAngle = PI*0.5, maxMotorTorque = sk("backMotorTorque").abs*5000, maxMotorSpeed = sk("backMotorSpeed").abs*3)
    val leftLeg   = new JointBone(world, length = sk("legLength").abs*10, thickness = 1, parent = hipBone, jointAttach = 0f, restAngle = -PI*0.5, maxMotorTorque = sk("legMotorTorque").abs*5000, maxMotorSpeed = sk("legMotorSpeed").abs*3)
    val leftFoot  = new JointBone(world, length = sk("footLength").abs*10, thickness = 1, parent = leftLeg, jointAttach = 1f, restAngle = -PI*sk("footRestAngle"), maxMotorTorque = sk("footMotorTorque").abs*5000, maxMotorSpeed = sk("footMotorSpeed").abs*3)
    val rightLeg  = new JointBone(world, length = sk("legLength").abs*10, thickness = 1, parent = hipBone, jointAttach = 1f, restAngle = -PI*0.5, maxMotorTorque = sk("legMotorTorque").abs*5000, maxMotorSpeed = sk("legMotorSpeed").abs*3)
    val rightFoot = new JointBone(world, length = sk("footLength").abs*10, thickness = 1, parent = rightLeg, jointAttach = 1f, restAngle = PI*sk("footRestAngle"), maxMotorTorque = sk("footMotorTorque").abs*5000, maxMotorSpeed = sk("footMotorSpeed").abs*3)
    val bones = Array(hipBone, backBone, leftLeg, leftFoot, rightLeg, rightFoot)
    val jointBones = Array(backBone, leftLeg, leftFoot, rightLeg, rightFoot)*/
    val head      = new RootBone(world, length = 2, thickness = 2, pos = Vec2(0,3), angle = 0)
    val back      = new JointBone(world, length = 7, thickness = 1, parent = head, jointAttach = 1, restAngle = 0, maxMotorTorque = 5000, maxMotorSpeed = 3f)
    val leftArm   = new JointBone(world, length = 4, thickness = 1, parent = back, jointAttach = 0.2, restAngle = 0, maxMotorTorque = 5000, maxMotorSpeed = 3f)
    val leftLowerArm   = new JointBone(world, length = 3, thickness = 1, parent = leftArm, jointAttach = 1, restAngle = 0, maxMotorTorque = 5000, maxMotorSpeed = 3f)
    val rightArm   = new JointBone(world, length = 4, thickness = 1, parent = back, jointAttach = 0.2, restAngle = 0, maxMotorTorque = 5000, maxMotorSpeed = 3f)
    val rightLowerArm   = new JointBone(world, length = 3, thickness = 1, parent = rightArm, jointAttach = 1, restAngle = 0, maxMotorTorque = 5000, maxMotorSpeed = 3f)

    val leftLeg   = new JointBone(world, length = 5, thickness = 1, parent = back, jointAttach = 1, restAngle = 0, maxMotorTorque = 5000, maxMotorSpeed = 3f)
    val leftLowerLeg   = new JointBone(world, length = 4, thickness = 1, parent = leftLeg, jointAttach = 1, restAngle = 0, maxMotorTorque = 5000, maxMotorSpeed = 3f)
    val leftFoot   = new JointBone(world, length = 2, thickness = 1, parent = leftLowerLeg, jointAttach = 1, restAngle = 0.5*PI, maxMotorTorque = 5000, maxMotorSpeed = 3f)
    val rightLeg   = new JointBone(world, length = 5, thickness = 1, parent = back, jointAttach = 1, restAngle = 0, maxMotorTorque = 5000, maxMotorSpeed = 3f)
    val rightLowerLeg   = new JointBone(world, length = 4, thickness = 1, parent = rightLeg, jointAttach = 1, restAngle = 0, maxMotorTorque = 5000, maxMotorSpeed = 3f)
    val rightFoot   = new JointBone(world, length = 2, thickness = 1, parent = rightLowerLeg, jointAttach = 1, restAngle = 0.5*PI, maxMotorTorque = 5000, maxMotorSpeed = 3f)

    val bones = Array(head, back, leftArm, leftLowerArm, rightArm, rightLowerArm, leftLeg, leftLowerLeg, leftFoot, rightLeg, rightLowerLeg, rightFoot)
    val jointBones = Array( back, leftArm, leftLowerArm, rightArm, rightLowerArm, leftLeg, leftLowerLeg, leftFoot, rightLeg, rightLowerLeg, rightFoot)

    val bodies = bones map (_.body)
    def outToAngle(out:Double) = (out*0.5*PI).toFloat
    val brain = new Brain(
      inputs = bodies.flatMap( body => Array(
        Sensor(body.getLinearVelocity.x),
        Sensor(body.getLinearVelocity.y),
        Sensor(sin(body.getAngularVelocity)),
        Sensor(cos(body.getAngularVelocity)),
        Sensor(sin(body.getAngle)),
        Sensor(cos(body.getAngle)),
        Sensor(body.getPosition.y/20f)/*,
        Sensor(playgod.Main.arrowDirection)*/
      ) ),
      outputs = jointBones.map( bone =>
        Effector{param => bone.angleTarget = outToAngle(param)}
      ),
      initialWeights = Some(brainGenes.genes.toArray)
    )
    assert( brain.inputs.size == dummyBrain.inputs.size )
    assert( brain.outputs.size == dummyBrain.outputs.size )

    override def reward = {
      //val vel = hipBone.body.getLinearVelocity.y
      //if( vel > 0 ) vel*2 else vel
      //val height = bones.map(_.body.getPosition.y).sum/bones.size
      //height
      val straightBody =
        (head.body.getPosition.y - back.body.getPosition.y) +
        (back.body.getPosition.y - leftLeg.body.getPosition.y) +
        (leftLeg.body.getPosition.y - leftLowerLeg.body.getPosition.y) +
        (leftLowerLeg.body.getPosition.y - leftFoot.body.getPosition.y) +
        (back.body.getPosition.y - rightLeg.body.getPosition.y) +
        (rightLeg.body.getPosition.y - rightLowerLeg.body.getPosition.y) +
        (rightLowerLeg.body.getPosition.y - rightFoot.body.getPosition.y)
      val vel = back.body.getLinearVelocity.x
      // back.body.getLinearVelocity.x * 
      
      var sum = 0.0
      sum += straightBody
      if( straightBody > 27 && vel > 0 )
        sum += vel

      sum
    }
    override def penalty = {
      /*val vel = bones.map(_.body.getLinearVelocity.x).sum/bones.size//hipBone.body.getLinearVelocity.x
      (playgod.Main.arrowDirection*5 - vel).abs*/
      //back.body.getAngle.abs
      //back.body.getPosition.x.abs / 20
      0
    }

    override def step() {
      brain.update()
      jointBones.foreach(_.update())
      super.step()
    }
  }
}

class Population(val creature:Creature) {
  import math._

  var populationSize = 30
  val parentCount = 2 //TODO: crossover with more parents
  var crossoverProbability = 0.85
  var mutationProbability = 0.05
  var mutationStrength = 0.2
  var elitism = 0.001

  //TODO: Array[O <: Organism]
  var organisms:Array[Organism] = creature.create(populationSize).toArray
  val genome = creature.genome

  def nextGeneration() {
    val sortedOrganisms = organisms.sortBy(_.fitness).reverse
    val sortedGenomes = sortedOrganisms map (_.genome)
    var newGenomes = new mutable.ArrayBuffer[Genome]

    sortedGenomes.foreach(_.isElite = false)
    newGenomes ++= sortedGenomes.take(ceil(elitism*populationSize).toInt)
    newGenomes.foreach(_.isElite = true)
    while( newGenomes.size < populationSize ) {
      if( sortedGenomes.size - newGenomes.size >= 2 ) {
        val parentA = rankSelection(sortedOrganisms, (e:Organism) => e.fitness).genome
        val parentB = rankSelection(sortedOrganisms, (e:Organism) => e.fitness).genome
        val (childA,childB) = if( inCase(crossoverProbability) ) {
            parentA crossover parentB
          } else {
            (parentA, parentB)
          }
        newGenomes += childA.mutate(mutationProbability, mutationStrength)
        newGenomes += childB.mutate(mutationProbability, mutationStrength)
      } else {
        newGenomes += sortedGenomes(rInt % sortedGenomes.size).mutate(mutationProbability, mutationStrength)
      }
    }

    // if lowering populationSize, take only the best ones
    if( populationSize < organisms.size )
      newGenomes = new mutable.ArrayBuffer[Genome] ++ sortedGenomes.take(populationSize)
    
    // if raising populationSize, fill the new positions with fresh random organisms
    if( organisms.size != populationSize )
      organisms = creature.create(populationSize).toArray

    for( (genome,i) <- newGenomes zip (0 until populationSize) ) {
      creature.genome = genome
      organisms(i) = creature.create
    }
  }
}
