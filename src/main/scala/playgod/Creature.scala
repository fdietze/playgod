package playgod

import org.jbox2d.common._
import org.jbox2d.dynamics._
import org.jbox2d.dynamics.joints._
import Box2DTools._

import collection.mutable
import math._

//TODO: disable self collision

object CreatureFactory {
  def forky:Creature = {
    val collisionGroupIndex = -1 //Physics.nextCollisionGroupIndex
    val hipBone = new RootBone(createBox(Physics.world, new Vec2(0, 7.5f), hx = 2.5f, hy = 0.5f,
                                         collisionGroupIndex = collisionGroupIndex))
    
    val backBone = new JointBone(
      createBox(Physics.world, new Vec2(0, 9.5f), hx = 0.5f, hy = 2.5f,
                collisionGroupIndex = collisionGroupIndex),
      parentBone = hipBone,
      jointPos = new Vec2(0,7.5f)
    )

    val leftLeg = new JointBone(
      createBox(Physics.world, new Vec2(-2f, 5.5f), hx = 0.5f, hy = 2.5f,
                collisionGroupIndex = collisionGroupIndex),
      parentBone = hipBone,
      jointPos = new Vec2(-2f,7.5f)
    )

    val leftLowerLeg = new JointBone(
      createBox(Physics.world, new Vec2(-2f, 2.5f), hx = 0.5f, hy = 1.5f,
                collisionGroupIndex = collisionGroupIndex),
      parentBone = leftLeg,
      jointPos = new Vec2(-2f,3.5f)
    )

    val rightLeg = new JointBone(
      createBox(Physics.world, new Vec2(2f, 5.5f), hx = 0.5f, hy = 2.5f,
                collisionGroupIndex = collisionGroupIndex),
      parentBone = hipBone,
      jointPos = new Vec2(2f,7.5f)
    )

    val rightLowerLeg = new JointBone(
      createBox(Physics.world, new Vec2(2f, 2.5f), hx = 0.5f, hy = 1.5f,
                collisionGroupIndex = collisionGroupIndex),
      parentBone = rightLeg,
      jointPos = new Vec2(2f,3.5f)
    )

    
    val creature = new Creature
    creature.rootBone = hipBone
    creature.jointBones += backBone
    creature.jointBones += leftLeg
    creature.jointBones += rightLeg
    creature.jointBones += leftLowerLeg
    creature.jointBones += rightLowerLeg
    
    creature.brain = new Brain {
      val inputs:Array[Sensor] = creature.bodies.flatMap( body => Array(
        new ClosureSensor(body.getLinearVelocity.x),
        new ClosureSensor(body.getLinearVelocity.y),
        new ClosureSensor(sin(body.getAngularVelocity)),
        new ClosureSensor(cos(body.getAngularVelocity)),
        new ClosureSensor(sin(body.getAngle)),
        new ClosureSensor(cos(body.getAngle)),
        new ClosureSensor(body.getPosition.y/20f)
      ) ).toArray
      
      def outToAngle(out:Double) = ((out * 2 - 1)*Pi*0.5).toFloat
      val outputs = creature.jointBones.map(
        bone => new Effector { def act(param:Double) { bone.angleTarget = outToAngle(param) } }
      ).toArray
      
      def bonus = {
        hipBone.body.getPosition.x
      }
      
      init()
    }
    
    return creature
  }
}

class Creature {

  var brain:Brain = null

  private var _rootBone:RootBone = null
  def rootBone = _rootBone
  def rootBone_=(newBone:RootBone) {
    _rootBone = newBone
  }
  
  val jointBones = new mutable.ArrayBuffer[JointBone] {
    override def += (newBone:JointBone) = {
      super.+=(newBone)
    }
  }
  
  def bodies = (rootBone +: jointBones).map(_.body)
  
  def addPosition(delta:Vec2) {
    for( body <- bodies ) {
      val transform = body.getTransform
      body.setTransform(transform.position.add(delta), transform.getAngle)
    }
  }
  
  def update() {
    brain.compute()
    jointBones.foreach(_.update())
  }

  def reset() {
    brain.score = 0
    rootBone.reset()
    jointBones.foreach(_.reset())
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
  def counterSpeed(error:Float) = tanh(error).toFloat*maxMotorSpeed
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

abstract class Brain {
  import org.encog.neural.networks.BasicNetwork
  import org.encog.neural.networks.layers.BasicLayer
  import org.encog.engine.network.activation.ActivationSigmoid

  def inputs:Array[Sensor]
  def outputs:Array[Effector]
  def bonus:Double

  var score:Double = 0
  
  
  val network = new BasicNetwork()
  
  def init() {
    network.addLayer(new BasicLayer(null,false, inputs.size))
    network.addLayer(new BasicLayer(new ActivationSigmoid(),true,inputs.size))
    network.addLayer(new BasicLayer(new ActivationSigmoid(),true,inputs.size))
    network.addLayer(new BasicLayer(new ActivationSigmoid(),true,outputs.size))
    network.getStructure().finalizeStructure()
    val randomizer = new org.encog.mathutil.randomize.GaussianRandomizer(0,1)
    randomizer.randomize(network)
  }
  
  def weightCount = network.encodedArrayLength
  def getWeights = {
    val weights = new Array[Double](weightCount)
    network.encodeToArray(weights)
    weights.clone
  }
  
  def replaceWeights(weights:Array[Double]) {
    network.decodeFromArray(weights)
  }
  
  def compute() {
    val inputValues = inputs.map(_.getValue)
    val outputValues = new Array[Double](outputs.size)
    network.compute(inputValues, outputValues)
    //println(inputValues.mkString(",") +  " => " + outputValues.mkString(","))
    for( (effector,param) <- outputs zip outputValues )
      effector.act(param)
    score += bonus
    //print("\rScore: %8.3f, Bonus: %8.3f" format(score, bonus) )
  }
}

abstract class Sensor {
  def getValue:Double
}

class ClosureSensor( f: => Double ) extends Sensor {
  override def getValue = f
}

abstract class Effector {
  def act(param:Double)
}
