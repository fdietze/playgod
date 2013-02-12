package playgod

import org.jbox2d.common._
import org.jbox2d.dynamics._
import org.jbox2d.dynamics.joints._
import Box2DTools._

import collection.mutable

//TODO: disable self collision

object Skeleton {
  def forky:Skeleton = {
    val hipBone = new RootBone(createBox(Physics.world, new Vec2(0, 6.5f), hx = 2.5f, hy = 0.5f))
    
    val backBone = new JointBone(
      createBox(Physics.world, new Vec2(0, 8.5f), hx = 0.5f, hy = 2.5f),
      parentBone = hipBone,
      jointPos = new Vec2(0,6.5f)
    )

    val leftLeg = new JointBone(
      createBox(Physics.world, new Vec2(-2f, 4.5f), hx = 0.5f, hy = 2.5f),
      parentBone = hipBone,
      jointPos = new Vec2(-2f,6.5f)
    )

    val rightLeg = new JointBone(
      createBox(Physics.world, new Vec2(2f, 4.5f), hx = 0.5f, hy = 2.5f),
      parentBone = hipBone,
      jointPos = new Vec2(2f,6.5f)
    )
    
    val skeleton = new Skeleton
    skeleton.rootBone = hipBone
    skeleton.jointBones += backBone
    skeleton.jointBones += leftLeg
    skeleton.jointBones += rightLeg
    skeleton.brain = Some(new Brain {
      val inputs = Array(
        new Sensor { def getValue = hipBone.body.getLinearVelocity.x },
        new Sensor { def getValue = hipBone.body.getLinearVelocity.y },
        new Sensor { def getValue = hipBone.body.getAngularVelocity },
        new Sensor { def getValue = math.cos(hipBone.body.getAngle) },
        new Sensor { def getValue = math.sin(hipBone.body.getAngle) },
        new Sensor { def getValue = hipBone.body.getPosition.y/20f }
        //TODO: contact points
      )
      def outToAngle(out:Double) = ((out * 2 - 1)*math.Pi*0.5).toFloat
      val outputs = Array(
        new Effector { def act(param:Double) { backBone.angleTarget = outToAngle(param) } },
        new Effector { def act(param:Double) { leftLeg.angleTarget = outToAngle(param) } },
        new Effector { def act(param:Double) { rightLeg.angleTarget = outToAngle(param) } }
      )
      
      init()
    })
    
    return skeleton
  }
}

class Skeleton {
  val collisionGroupIndex = Physics.nextCollisionGroupIndex

  var brain:Option[Brain] = None

  private var _rootBone:RootBone = null
  def rootBone = _rootBone
  def rootBone_=(newBone:RootBone) {
    setCollisionGroup(newBone)
    _rootBone = newBone
  }
  
  val jointBones = new mutable.ArrayBuffer[JointBone] {
    override def += (newBone:JointBone) = {
      setCollisionGroup(newBone)
      super.+=(newBone)
    }
  }
  
  def bodies = (rootBone +: jointBones).map(_.body)
  
  private def setCollisionGroup(bone:Bone) {
    // dont let bones collide in one skeleton
    val filter = bone.body.getFixtureList.getFilterData
    filter.groupIndex = collisionGroupIndex
    bone.body.getFixtureList.setFilterData(filter)
  }
  
  def addPosition(delta:Vec2) {
    for( body <- bodies ) {
      val transform = body.getTransform
      body.setTransform(transform.position.add(delta), transform.getAngle)
    }
  }
  
  def update() {
    if( brain.isDefined )
      brain.get.compute()
    jointBones.foreach(_.update())
  }
}

abstract class Bone { val body:Body }
class RootBone(val body:Body) extends Bone
class JointBone(val body:Body, parentBone:Bone, val jointPos:Vec2 ) extends Bone {
  val jointDef = new RevoluteJointDef
  jointDef.initialize(body, parentBone.body, jointPos)
  jointDef.motorSpeed = 0f
  jointDef.maxMotorTorque = 10000.0f
  jointDef.enableMotor = true
  //jointDef.collideConnected = false
  val maxMotorSpeed = 5f
  val joint = Physics.world.createJoint(jointDef).asInstanceOf[RevoluteJoint]
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

abstract class Brain {
  import org.encog.neural.networks.BasicNetwork
  import org.encog.neural.networks.layers.BasicLayer
  import org.encog.engine.network.activation.ActivationSigmoid

  def inputs:Array[Sensor]
  def outputs:Array[Effector]
  
  val network = new BasicNetwork()
  
  def init() {
    network.addLayer(new BasicLayer(null,false, inputs.size))
    network.addLayer(new BasicLayer(new ActivationSigmoid(),true,inputs.size))
    network.addLayer(new BasicLayer(new ActivationSigmoid(),true,outputs.size))
    network.getStructure().finalizeStructure()
    network.reset()
  }
  
  def compute() {
    val inputValues = inputs.map(_.getValue)
    val outputValues = new Array[Double](outputs.size)
    network.compute(inputValues, outputValues)
    //println(inputValues.mkString(",") +  " => " + outputValues.mkString(","))
    for( (effector,param) <- outputs zip outputValues )
      effector.act(param)
  }
}

abstract class Sensor {
  def getValue:Double
}

abstract class Effector {
  def act(param:Double)
}
