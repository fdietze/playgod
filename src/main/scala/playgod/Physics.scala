package playgod

import org.jbox2d.dynamics._
import org.jbox2d.dynamics.joints._
import org.jbox2d.common._
import Box2DTools._

object Physics {
  val world = new World(new Vec2(0, -9.81f), true)
  world.setDebugDraw(DebugDrawer)
  
  createBox(world, new Vec2(0, -10), hx = 50, hy = 3, density = 0f)

  val box0 = createBox(world, new Vec2(0, 2f), hx = 1f, hy = 1f)

  val boxA = createBox(world, new Vec2(-4.5f, 5f), hx = 5f, hy = 0.5f)
  val boxB = createBox(world, new Vec2(4.5f, 5f), hx = 5f, hy = 0.5f)
  
  val jointDef = new RevoluteJointDef
  jointDef.initialize(boxA, boxB, boxA.getPosition.add(new Vec2(4.5f,0)));
  jointDef.motorSpeed = 2f;
  jointDef.maxMotorTorque = 10000.0f
  jointDef.enableMotor = true;
  val joint = world.createJoint(jointDef).asInstanceOf[RevoluteJoint]
  
  var angleTarget = math.Pi.toFloat / 2f
  
  def sigmoid(x:Float, maxGain:Float) = math.tanh(x).toFloat*maxGain
  
  def update() {
    val angleError = angleTarget - joint.getJointAngle
    joint.setMotorSpeed(sigmoid(angleError,5))
  }
}
