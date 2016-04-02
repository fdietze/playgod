package playgod

import playgod.Box2DTools._
import playgod.Box2DTools.MathHelpers.Vec2
import org.jbox2d.common.MathUtils._
import playgod.RandomTools._

abstract class Creature {
  var genome:Genome
  def create:Organism
  def create(n:Int):Seq[Organism]
}

class Box2DCreature extends Creature {
  val gb = new GenomeBuilder
  gb[NamedChromosomeBuilder]("skeleton")("hipLength") = 0.05
  gb[NamedChromosomeBuilder]("skeleton")("backLength") = 0.05
  gb[NamedChromosomeBuilder]("skeleton")("legLength") = 0.05
  gb[NamedChromosomeBuilder]("skeleton")("footLength") = 0.05
  gb[NamedChromosomeBuilder]("skeleton")("backMotorTorque") = 1
  gb[NamedChromosomeBuilder]("skeleton")("legMotorTorque") = 1
  gb[NamedChromosomeBuilder]("skeleton")("footMotorTorque") = 1
  gb[NamedChromosomeBuilder]("skeleton")("backMotorSpeed") = 1
  gb[NamedChromosomeBuilder]("skeleton")("legMotorSpeed") = 1
  gb[NamedChromosomeBuilder]("skeleton")("footMotorSpeed") = 1
  //gb[NamedChromosomeBuilder]("skeleton")("backRestAngle") = 0.5
  //gb[NamedChromosomeBuilder]("skeleton")("legRestAngle") = 0.5 //TODO: implement jointbones with inverse angle
  gb[NamedChromosomeBuilder]("skeleton")("footRestAngle") = 0

  val sensorCount = 7
  val boneCount = 6
  val dummyBrain = new Brain(new Array[Sensor](sensorCount*boneCount), new Array[Effector](boneCount -1))
  gb("brain") = dummyBrain.getWeights

  var genome = gb.toGenome
  var maxSimulationSteps = 3000
  var simulationTimeStep = 1/60f
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
    override val timeStep = simulationTimeStep
    val genome = currentGenome
    val sk = genome[NamedChromosome]("skeleton")
    val brainGenes = genome[PlainChromosome]("brain")

    val ground = createBox(world, pos = new Vec2(0, -50), width = 2000, height = 100, density = 0f, friction = 1f)
    // val obstacle = createBox(world, pos = new Vec2(80, -3), width = 2, height = 8, density = 0f, friction = 1f)
    // val obstacle2 = createBox(world, pos = new Vec2(90, -2), width = 2, height = 8, density = 0f, friction = 1f)
    // val ground2 = createBox(world, pos = new Vec2(205, -50), width = 200, height = 100, density = 0f, friction = 1f)
    for( i <- 1 to 20 ) {
      createBox(world, pos = new Vec2(50+i*20, -5+i*i/4), width = 20, height = 10, density = 0f, friction = 1f)
    }

        val hipBone   = new RootBone(world, length = sk("hipLength").abs*100, thickness = 1, pos = Vec2(0,(sk("hipLength")+sk("backLength")+sk("legLength")+sk("footLength"))*100), angle = 0.0)
        val backBone  = new JointBone(world, length = sk("backLength").abs*100, thickness = 1, parent = hipBone, jointAttach = 0.5f, restAngle = PI*0.5, maxMotorTorque = sk("backMotorTorque").abs*5000, maxMotorSpeed = sk("backMotorSpeed").abs*3)
        val leftLeg   = new JointBone(world, length = sk("legLength").abs*100, thickness = 1, parent = hipBone, jointAttach = 0f, restAngle = -PI*0.5, maxMotorTorque = sk("legMotorTorque").abs*5000, maxMotorSpeed = sk("legMotorSpeed").abs*3)
        val leftFoot  = new JointBone(world, length = sk("footLength").abs*100, thickness = 1, parent = leftLeg, jointAttach = 1f, restAngle = -PI*sk("footRestAngle"), maxMotorTorque = sk("footMotorTorque").abs*5000, maxMotorSpeed = sk("footMotorSpeed").abs*3)
        val rightLeg  = new JointBone(world, length = sk("legLength").abs*100, thickness = 1, parent = hipBone, jointAttach = 1f, restAngle = -PI*0.5, maxMotorTorque = sk("legMotorTorque").abs*5000, maxMotorSpeed = sk("legMotorSpeed").abs*3)
        val rightFoot = new JointBone(world, length = sk("footLength").abs*100, thickness = 1, parent = rightLeg, jointAttach = 1f, restAngle = PI*sk("footRestAngle"), maxMotorTorque = sk("footMotorTorque").abs*5000, maxMotorSpeed = sk("footMotorSpeed").abs*3)

        val bones = Array(hipBone, backBone, leftLeg, leftFoot, rightLeg, rightFoot)
        val jointBones = Array(backBone, leftLeg, leftFoot, rightLeg, rightFoot)
    // val maxMotorSpeed = 5f
    // val maxMotorTorque = 5000f
    // val head      = new RootBone(world, length = 0.2, thickness = 0.2, pos = Vec2(0,2), angle = -0.5*PI)
    // val back      = new JointBone(world, length = 0.6, thickness = 0.15, parent = head, jointAttach = 1, restAngle = 0, maxMotorTorque = maxMotorTorque, maxMotorSpeed = maxMotorSpeed)

    // val leftArm   = new JointBone(world, length = 0.3, thickness = 0.1, parent = back, jointAttach = 0.2, restAngle = 0, maxMotorTorque = maxMotorTorque, maxMotorSpeed = maxMotorSpeed)
    // val leftLowerArm   = new JointBone(world, length = 0.4, thickness = 0.1, parent = leftArm, jointAttach = 1, restAngle = 0, maxMotorTorque = maxMotorTorque, maxMotorSpeed = maxMotorSpeed)
    // val rightArm   = new JointBone(world, length = 0.3, thickness = 0.1, parent = back, jointAttach = 0.2, restAngle = 0, maxMotorTorque = maxMotorTorque, maxMotorSpeed = maxMotorSpeed)
    // val rightLowerArm   = new JointBone(world, length = 0.4, thickness = 0.1, parent = rightArm, jointAttach = 1, restAngle = 0, maxMotorTorque = maxMotorTorque, maxMotorSpeed = maxMotorSpeed)

    // val leftLeg   = new JointBone(world, length = 0.5, thickness = 0.15, parent = back, jointAttach = 1, restAngle = 0, maxMotorTorque = maxMotorTorque, maxMotorSpeed = maxMotorSpeed)
    // val leftLowerLeg   = new JointBone(world, length = 0.5, thickness = 0.1, parent = leftLeg, jointAttach = 1, restAngle = 0, maxMotorTorque = maxMotorTorque, maxMotorSpeed = maxMotorSpeed)
    // val leftFoot   = new JointBone(world, length = 0.3, thickness = 0.05, parent = leftLowerLeg, jointAttach = 1, restAngle = 0.5*PI, maxMotorTorque = maxMotorTorque, maxMotorSpeed = maxMotorSpeed)
    // val rightLeg   = new JointBone(world, length = 0.5, thickness = 0.15, parent = back, jointAttach = 1, restAngle = 0, maxMotorTorque = maxMotorTorque, maxMotorSpeed = maxMotorSpeed)
    // val rightLowerLeg   = new JointBone(world, length = 0.5, thickness = 0.1, parent = rightLeg, jointAttach = 1, restAngle = 0, maxMotorTorque = maxMotorTorque, maxMotorSpeed = maxMotorSpeed)
    // val rightFoot   = new JointBone(world, length = 0.3, thickness = 0.05, parent = rightLowerLeg, jointAttach = 1, restAngle = 0.5*PI, maxMotorTorque = maxMotorTorque, maxMotorSpeed = maxMotorSpeed)

    // val bones = Array(head, back, leftArm, leftLowerArm, rightArm, rightLowerArm, leftLeg, leftLowerLeg, leftFoot, rightLeg, rightLowerLeg, rightFoot)
    // val jointBones = Array( back, leftArm, leftLowerArm, rightArm, rightLowerArm, leftLeg, leftLowerLeg, leftFoot, rightLeg, rightLowerLeg, rightFoot)

    val bodies = bones map (_.body)
    def outToAngle(out:Double) = (out*0.5*PI).toFloat
    val brain = new Brain(
      inputs = bodies.flatMap( body => Array(
        Sensor(body.getLinearVelocity.x),
        Sensor(body.getLinearVelocity.y),
        // Sensor(body.getAngularVelocity),
        Sensor(sin(body.getAngularVelocity)),
        Sensor(cos(body.getAngularVelocity)),
        Sensor(sin(body.getAngle)),
        Sensor(cos(body.getAngle)),
        Sensor(body.getPosition.y/10f)
        // Sensor(playgod.Main.arrowDirection)
      ) ),
      outputs = jointBones.map( bone =>
        Effector{param => bone.angleTarget = outToAngle(param)}
      ),
      initialWeights = Some(brainGenes.genes.toArray)
    )
    assert( brain.inputs.size == dummyBrain.inputs.size )
    assert( brain.outputs.size == dummyBrain.outputs.size )

    override def reward = {
      backBone.body.getLinearVelocity.x * backBone.body.getPosition.y*backBone.body.getPosition.y

    }
    override def penalty = {
      var sum = 0.0

      sum
    }

    override def step() {
      //println(straightBody)
      brain.update()
      jointBones.foreach(_.update())
      super.step()
    }
  }
}
