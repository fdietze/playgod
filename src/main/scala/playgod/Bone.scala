package playgod

import org.jbox2d.dynamics.{FixtureDef, BodyType, BodyDef, World}
import playgod.Box2DTools.MathHelpers._
import org.jbox2d.common.MathUtils._
import org.jbox2d.collision.shapes.PolygonShape
import org.jbox2d.dynamics.joints.{RevoluteJoint, RevoluteJointDef}

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

  val density = 1.4f
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