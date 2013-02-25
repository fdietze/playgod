package playgod

import collection.mutable

class BrainDefinition(
                       val inputs:Array[BoneSensorDefinition],
                       val outputs:Array[BoneEffectorDefinition],
                       val bonus:Creature => Double) {
  def create(boneMap:mutable.Map[BoneDefinition,Bone], initialWeights:Option[Array[Double]] = None) = {
    val brain = new Brain(inputs.map(_.create(boneMap)), outputs.map(_.create(boneMap)), bonus)
    if( initialWeights.isDefined ) {
      brain.replaceWeights(initialWeights.get)
    }
    brain
  }
}

case class BrainData(val weights:Array[Double], val score:Double)

class Brain(val inputs:Array[Sensor], val outputs:Array[Effector], bonus:Creature => Double) {
  import org.encog.neural.networks.BasicNetwork
  import org.encog.neural.networks.layers.BasicLayer
  import org.encog.engine.network.activation.ActivationSigmoid

  var score:Double = 0

  val network = new BasicNetwork()
  network.addLayer(new BasicLayer(null,false, inputs.size))
  network.addLayer(new BasicLayer(new ActivationSigmoid(),true,inputs.size))
  network.addLayer(new BasicLayer(new ActivationSigmoid(),true,inputs.size))
  network.addLayer(new BasicLayer(new ActivationSigmoid(),true,outputs.size))
  network.getStructure.finalizeStructure()
  val randomizer = new org.encog.mathutil.randomize.GaussianRandomizer(0,1)
  randomizer.randomize(network)

  def weightCount = network.encodedArrayLength
  def getWeights = {
    val weights = new Array[Double](weightCount)
    network.encodeToArray(weights)
    weights.clone()
  }

  def getData = BrainData(getWeights, score)

  def replaceWeights(weights:Array[Double]) {
    network.decodeFromArray(weights)
  }

  def update(creature:Creature) {
    val inputValues = inputs.map(_.getValue)
    val outputValues = new Array[Double](outputs.size)
    network.compute(inputValues, outputValues)
    //println(inputValues.mkString(",") +  " => " + outputValues.mkString(","))
    for( (effector,param) <- outputs zip outputValues )
      effector.act(param)
    score += bonus(creature)
    //print("\rScore: %8.3f, Bonus: %8.3f" format(score, bonus) )
  }
}

abstract class Sensor {
  def getValue:Double
}

class BoneSensorDefinition( val boneDefinition:BoneDefinition, val extractValue:Bone => Double ) {
  def create(boneMap:mutable.Map[BoneDefinition,Bone]) = new BoneSensor(boneMap(boneDefinition), extractValue)
}

class BoneSensor(val bone:Bone, val extractValue:Bone => Double) extends Sensor {
  def getValue = extractValue(bone)
}

class ClosureSensor( f: => Double ) extends Sensor {
  override def getValue = f
}

abstract class Effector {
  def act(param:Double)
}

class BoneEffectorDefinition( val boneDefinition:BoneDefinition, val effect:(Bone,Double) => Unit ) {
  def create(boneMap:mutable.Map[BoneDefinition,Bone]) = new BoneEffector(boneMap(boneDefinition), effect)
}

class BoneEffector(val bone:Bone, val effect:(Bone,Double) => Unit) extends Effector {
  def act(param:Double) = effect(bone, param)
}
