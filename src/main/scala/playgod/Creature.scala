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
    
    return skeleton
  }
}

class Skeleton {
  var rootBone:RootBone = null
  val jointBones = new mutable.ArrayBuffer[JointBone]
  
  def update() { jointBones.foreach(_.update()) }
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

