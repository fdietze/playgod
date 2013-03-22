package playgod

import playgod.Box2DTools._
import playgod.Box2DTools.MathHelpers.Vec2
import org.jbox2d.common.MathUtils._

abstract class Creature {
  var genome:Genome
  def create:Organism
  //def create(n:Int):Seq[Organism]
}

class Box2DCreature extends Creature {
  val gb = new GenomeBuilder
  gb[NamedChromosomeBuilder]("skeleton")("hipLength") = 0.5
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
  gb[NamedChromosomeBuilder]("skeleton")("footRestAngle") = 0

  val sensorCount = 6
  val sensorBoneCount = 7
  val effectorBoneCount = 5
  val dummyBrain = new FeedForwardBrain(new Array[Sensor](sensorCount*sensorBoneCount), new Array[Effector](effectorBoneCount))
  gb("brain") = dummyBrain.getWeights

  var genome = gb.toGenome
  var maxSimulationSteps = 1000
  var simulationTimeStep = 1/60f
  def currentGenome = genome

  //println("genes: %d (%d synapses)" format(genome.genes.size, genome[PlainChromosome]("brain").size))

  def randomGenome = {
    dummyBrain.randomizeWeights()
    gb("brain") = dummyBrain.getWeights
    gb.toGenome
  }

  /*def create(n:Int) = {
    for( _ <- 0 until n) yield {
      genome = randomGenome
      create
    }
  }*/

 class Orga extends Box2DSimulationOrganism with SimpleActor {
    maxSteps = maxSimulationSteps
    override val timeStep = simulationTimeStep
    val genome = currentGenome
    val sk = genome[NamedChromosome]("skeleton")
    val brainGenes = genome[PlainChromosome]("brain")

    val ground = createBox(world, pos = new Vec2(0, -5), width = 20000, height = 10, density = 0f, friction = 1f)
    //val obstacle = createBox(world, pos = new Vec2(50, -3), width = 2, height = 8, density = 0f, friction = 1f)

        /*val hipBone   = new RootBone(world, length = sk("hipLength").abs*10, thickness = 1, pos = Vec2(0,(sk("hipLength")+sk("backLength")+sk("legLength")+sk("footLength"))*10), angle = 0.0)
        val backBone  = new JointBone(world, length = sk("backLength").abs*10, thickness = 1, parent = hipBone, jointAttach = 0.5f, restAngle = PI*0.5, maxMotorTorque = sk("backMotorTorque").abs*5000, maxMotorSpeed = sk("backMotorSpeed").abs*3)
        val leftLeg   = new JointBone(world, length = sk("legLength").abs*10, thickness = 1, parent = hipBone, jointAttach = 0f, restAngle = -PI*0.5, maxMotorTorque = sk("legMotorTorque").abs*5000, maxMotorSpeed = sk("legMotorSpeed").abs*3)
        val leftFoot  = new JointBone(world, length = sk("footLength").abs*10, thickness = 1, parent = leftLeg, jointAttach = 1f, restAngle = -PI*sk("footRestAngle"), maxMotorTorque = sk("footMotorTorque").abs*5000, maxMotorSpeed = sk("footMotorSpeed").abs*3)
        val rightLeg  = new JointBone(world, length = sk("legLength").abs*10, thickness = 1, parent = hipBone, jointAttach = 1f, restAngle = -PI*0.5, maxMotorTorque = sk("legMotorTorque").abs*5000, maxMotorSpeed = sk("legMotorSpeed").abs*3)
        val rightFoot = new JointBone(world, length = sk("footLength").abs*10, thickness = 1, parent = rightLeg, jointAttach = 1f, restAngle = PI*sk("footRestAngle"), maxMotorTorque = sk("footMotorTorque").abs*5000, maxMotorSpeed = sk("footMotorSpeed").abs*3)
        val bones = Array(hipBone, backBone, leftLeg, leftFoot, rightLeg, rightFoot)
        val jointBones = Array(    backBone, leftLeg, leftFoot, rightLeg, rightFoot)
        val sensorBones = bones
        val effectorBones = jointBones // */
    val maxMotorSpeed = 2f
    val maxMotorTorque = 5000f
    val head      = new RootBone(world, length = 0.2, thickness = 0.2, pos = Vec2(0,3), angle = 0.0)
    val back      = new JointBone(world, length = 0.6, thickness = 0.15, parent = head, jointAttach = 1, restAngle = 0, maxMotorTorque = maxMotorTorque, maxMotorSpeed = maxMotorSpeed)

    val leftArm   = new JointBone(world, length = 0.3, thickness = 0.1, parent = back, jointAttach = 0.2, restAngle = 0, maxMotorTorque = maxMotorTorque, maxMotorSpeed = maxMotorSpeed)
    val leftLowerArm   = new JointBone(world, length = 0.4, thickness = 0.1, parent = leftArm, jointAttach = 1, restAngle = 0, maxMotorTorque = maxMotorTorque, maxMotorSpeed = maxMotorSpeed)
    val rightArm   = new JointBone(world, length = 0.3, thickness = 0.1, parent = back, jointAttach = 0.2, restAngle = 0, maxMotorTorque = maxMotorTorque, maxMotorSpeed = maxMotorSpeed)
    val rightLowerArm   = new JointBone(world, length = 0.4, thickness = 0.1, parent = rightArm, jointAttach = 1, restAngle = 0, maxMotorTorque = maxMotorTorque, maxMotorSpeed = maxMotorSpeed)

    val leftLeg   = new JointBone(world, length = 0.5, thickness = 0.15, parent = back, jointAttach = 1, restAngle = 0, maxMotorTorque = maxMotorTorque, maxMotorSpeed = maxMotorSpeed)
    val leftLowerLeg   = new JointBone(world, length = 0.5, thickness = 0.1, parent = leftLeg, jointAttach = 1, restAngle = 0, maxMotorTorque = maxMotorTorque, maxMotorSpeed = maxMotorSpeed)
    val leftFoot   = new JointBone(world, length = 0.3, thickness = 0.05, parent = leftLowerLeg, jointAttach = 1, restAngle = 0, maxMotorTorque = maxMotorTorque, maxMotorSpeed = maxMotorSpeed)
    val rightLeg   = new JointBone(world, length = 0.5, thickness = 0.15, parent = back, jointAttach = 1, restAngle = 0, maxMotorTorque = maxMotorTorque, maxMotorSpeed = maxMotorSpeed)
    val rightLowerLeg   = new JointBone(world, length = 0.5, thickness = 0.1, parent = rightLeg, jointAttach = 1, restAngle = 0, maxMotorTorque = maxMotorTorque, maxMotorSpeed = maxMotorSpeed)
    val rightFoot   = new JointBone(world, length = 0.3, thickness = 0.05, parent = rightLowerLeg, jointAttach = 1, restAngle = 0, maxMotorTorque = maxMotorTorque, maxMotorSpeed = maxMotorSpeed)

    val bones = Array(head, back, leftArm, leftLowerArm, rightArm, rightLowerArm, leftLeg, leftLowerLeg, leftFoot, rightLeg, rightLowerLeg, rightFoot)
    val jointBones = Array( back, leftArm, leftLowerArm, rightArm, rightLowerArm, leftLeg, leftLowerLeg, leftFoot, rightLeg, rightLowerLeg, rightFoot)
    val sensorBones = bones//Array(back, leftLeg, leftLowerLeg, leftFoot, rightLeg, rightLowerLeg, rightFoot)
    val effectorBones = jointBones // */

    def boneStates = bones map {b => import b.body._; BoneState(getPosition.clone, getLinearVelocity.clone, getAngle, getAngularVelocity)}
    def setBoneStates(states:IndexedSeq[BoneState]) {
      for( (bone,state) <- bones zip states ) {
        import bone.body._
        import state._
        setTransform(pos.clone,angle)
        setLinearVelocity(vel.clone)
        setAngularVelocity(angleVel)
      }
    }

    val sensorBodies = sensorBones map (_.body)
    def outToAngle(out:Double) = ((out*2-1)*0.9*PI).toFloat
    var brain:Brain = new FeedForwardBrain(
      inputs = sensorBodies.flatMap( body => Array(
        Sensor(body.getLinearVelocity.x),
        Sensor(body.getLinearVelocity.y),
        // Sensor(body.getAngularVelocity),
        Sensor(sin(body.getAngularVelocity)),
        Sensor(cos(body.getAngularVelocity)),
        Sensor(sin(body.getAngle)),
        Sensor(cos(body.getAngle)),
        Sensor(body.getPosition.y/10f)/*,
        Sensor(playgod.Main.arrowDirection)*/
      ) ),
      outputs = jointBones.map( bone =>
        Effector{param => bone.angleTarget = outToAngle(param)}
      )
    )
    //assert( brain.inputs.size == dummyBrain.inputs.size )
    //assert( brain.outputs.size == dummyBrain.outputs.size )

    def straightBody = //(2*head.body.getPosition.y - leftFoot.body.getPosition.y - rightFoot.body.getPosition.y) +
        (head.body.getPosition.y - back.body.getPosition.y) +
        (back.body.getPosition.y - leftLeg.body.getPosition.y) +
        (leftLeg.body.getPosition.y - leftLowerLeg.body.getPosition.y) +
        (leftLowerLeg.body.getPosition.y - leftFoot.body.getPosition.y) +
        (back.body.getPosition.y - rightLeg.body.getPosition.y) +
        (rightLeg.body.getPosition.y - rightLowerLeg.body.getPosition.y) +
        (rightLowerLeg.body.getPosition.y - rightFoot.body.getPosition.y)// */

    override def reward = {
      //val vel = hipBone.body.getLinearVelocity.y
      //if( vel > 0 ) vel*2 else vel
      //val height = bones.map(_.body.getPosition.y).sum/bones.size
      //height

      val vel = back.body.getLinearVelocity.x
      // back.body.getLinearVelocity.x *

      var sum = 0.0
      sum += straightBody min 1.7f
      /*sum += backBone.body.getPosition.y - hipBone.body.getPosition.y
      sum += hipBone.body.getPosition.y - leftFoot.body.getPosition.y
      sum += hipBone.body.getPosition.y - rightFoot.body.getPosition.y*/
      if( straightBody >= 1.7 && vel > 0 ) {
        sum += vel
        //sum += (leftFoot.body.getPosition.y - rightFoot.body.getPosition.y).abs
      }

      sum
    }
    override def penalty = {
      /*val vel = bones.map(_.body.getLinearVelocity.x).sum/bones.size//hipBone.body.getLinearVelocity.x
      (playgod.Main.arrowDirection*5 - vel).abs*/
      //back.body.getAngle.abs
      //back.body.getPosition.x.abs / 20
      var sum = 0.0
      //sum += hipBone.body.getAngle.abs * 10
      if( straightBody < 1.7 )
        sum += back.body.getLinearVelocity.x.abs * 0.5f

      sum
    }

    override def step() {
      processMessages()
      brain.think()
      jointBones.foreach(_.update())
      super.step()
    }

    def receive = {
      //case _ =>
      case BrainUpdate(newBrain) =>
        brain = newBrain
    }
  }
  def create = new Orga
}
