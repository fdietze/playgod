package playgod

import org.jbox2d.dynamics._
import org.jbox2d.dynamics.joints._
import Box2DTools._
import Box2DTools.MathHelpers._

import collection.mutable
import math._
import org.jbox2d.collision.shapes.PolygonShape

object CreatureFactory {
  def forky:CreatureDefinition = {
    val hipBone = new RootBoneDefinition(pos = Vec2(0, 7.5f), width = 5f, height = 1f)
    val backBone = new JointBoneDefinition(pos = Vec2(0, 9.5f), width = 1f, height = 5f,
      parentBone = hipBone, jointPos = Vec2(0,7.5f))
    val leftLeg = new JointBoneDefinition(pos = Vec2(-2f, 5.5f), width = 1f, height = 5f,
      parentBone = hipBone, jointPos = Vec2(-2f,7.5f))
    val leftLowerLeg = new JointBoneDefinition(pos = Vec2(-2f, 2.5f), width = 1f, height = 3f,
      parentBone = leftLeg, jointPos = Vec2(-2f,3.5f))
    val rightLeg = new JointBoneDefinition(pos = Vec2(2f, 5.5f), width = 1f, height = 5f,
      parentBone = hipBone, jointPos = Vec2(2f,7.5f))
    val rightLowerLeg = new JointBoneDefinition(pos = Vec2(2f, 2.5f), width = 1f, height = 3f,
      parentBone = rightLeg, jointPos = Vec2(2f,3.5f))

    def outToAngle(out:Double) = ((out * 2 - 1)*Pi*0.5).toFloat
    val brain = new BrainDefinition(
      inputs = Array(hipBone, backBone, leftLeg, leftLowerLeg, rightLeg, rightLowerLeg).flatMap( bone => Array(
        new BoneSensorDefinition(bone, (b) => b.body.getLinearVelocity.x),
        new BoneSensorDefinition(bone, (b) => b.body.getLinearVelocity.y),
        new BoneSensorDefinition(bone, (b) => sin(b.body.getAngularVelocity)),
        new BoneSensorDefinition(bone, (b) => cos(b.body.getAngularVelocity)),
        new BoneSensorDefinition(bone, (b) => sin(b.body.getAngle)),
        new BoneSensorDefinition(bone, (b) => cos(b.body.getAngle)),
        new BoneSensorDefinition(bone, (b) => b.body.getPosition.y/20f)
      ) ) /*++ Array(
        new ClosureSensor( Main.arrowDirection.toDouble )
      )*/,
      outputs = Array(backBone, leftLeg, leftLowerLeg, rightLeg, rightLowerLeg).flatMap( bone => Array(
        new BoneEffectorDefinition(bone, (b, a) => {b.asInstanceOf[JointBone].angleTarget = outToAngle(a)}))
      ),
      bonus = { c =>
        c.boneMap(hipBone).body.getPosition.x


        //val error = (c.boneMap(hipBone).body.getAngle - Pi).abs
        //-error


        /*var error = 0.0
        var bonus = 0.0
        error += (Main.arrowDirection*3 - c.boneMap(hipBone).body.getLinearVelocity.x).abs
        error += c.boneMap(hipBone).body.getAngle.abs
        bonus += c.boneMap(hipBone).body.getPosition.y/30f
        
        bonus-error*/
      }
    )

    val creatureDef = new CreatureDefinition(brain, hipBone,
      backBone, leftLeg, rightLeg, leftLowerLeg, rightLowerLeg)
    
    return creatureDef
  }
}

class Creature(
    val brain:Brain,
    val boneMap:mutable.Map[BoneDefinition, Bone],
    val rootBone:RootBone,
    val jointBones:JointBone*
    ) {

  def bodies = (rootBone +: jointBones).map(_.body)
  
  def update() {
    brain.update(this)
    jointBones.foreach(_.update())
  }

  def reset() {
    rootBone.reset()
    jointBones.foreach(_.reset())
    brain.update(this)
    brain.score = 0
  }
}

class CreatureDefinition(
    val brainDefinition:BrainDefinition,
    val rootBoneDefinition:RootBoneDefinition,
    val jointBoneDefinitions:JointBoneDefinition*
    ) {

  def create(world:World, initialBrainWeights:Option[Array[Double]] = None) = {
    val boneMap = new mutable.HashMap[BoneDefinition,Bone]
    val rootBone = rootBoneDefinition.createBone(world, boneMap)
    val jointBones = jointBoneDefinitions.map(_.createBone(world, boneMap))
    val creature = new Creature(
      brainDefinition.create(boneMap, initialBrainWeights),
      boneMap,
      rootBone,
      jointBones:_*
    )
    creature
  }
}

class BoneDefinition(pos: Vec2,
                     width: Float = 1f,
                     height: Float = 1f,
                     angle: Float = 0f) extends ObjectDefinition {
  val density = 1f
  val friction = 1f
  val center = Vec2(0,0)
  val collisionGroupIndex = 0

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
}

class RootBoneDefinition( pos: Vec2,
                          width: Float = 1f,
                          height: Float = 1f,
                          angle: Float = 0f) extends BoneDefinition(pos, width, height, angle) {
  def createBone(world:World, boneMap:mutable.Map[BoneDefinition,Bone]) = {
    val newBone = new RootBone(super.create(world))
    boneMap += (this -> newBone)
    newBone
  }
}

class JointBoneDefinition(pos: Vec2,
                     width: Float = 1f,
                     height: Float = 1f,
                     angle: Float = 0f,
                     parentBone:BoneDefinition,
                     jointPos: Vec2) extends BoneDefinition(pos, width, height, angle) {
  val jointDef = new RevoluteJointDef
  jointDef.motorSpeed = 0f
  jointDef.maxMotorTorque = 5000.0f
  jointDef.enableMotor = true
  //jointDef.collideConnected = false

  def createBone(world:World, boneMap:mutable.Map[BoneDefinition,Bone]) = {
    val body = super.create(world)
    jointDef.initialize(body, boneMap(parentBone).body, jointPos)
    val joint = world.createJoint(jointDef).asInstanceOf[RevoluteJoint]

    val newBone = new JointBone(body, joint)
    boneMap += (this -> newBone)
    newBone
  }
}

abstract class Bone {
  val body:Body
  val initialPosition = body.getPosition.clone
  val initialAngle = body.getAngle
  def reset() {
    body.setLinearVelocity(new Vec2(0,0))
    body.setAngularVelocity(0)
    body.setTransform(initialPosition.clone, initialAngle)
    body.setAwake(true)
  }
}

class RootBone(val body:Body) extends Bone

class JointBone(val body:Body, val joint:RevoluteJoint ) extends Bone {
  val maxMotorSpeed = 3f
  var angleTarget = joint.getJointAngle
  def counterSpeed(error:Float) = math.tanh(error).toFloat*maxMotorSpeed
  def update() {
    val angleError = angleTarget - joint.getJointAngle
    // only set motorSpeed when necessary
    // => allows deactivation
    if( angleError.abs > 0.001f )
      joint.setMotorSpeed(counterSpeed(angleError))
  }
  
  override def reset() {
    angleTarget = 0
    joint.setMotorSpeed(0f)
    super.reset()
  }
}
