package playgod

import org.encog.neural.networks.BasicNetwork
import org.encog.neural.networks.layers.BasicLayer
import org.encog.engine.network.activation.ActivationTANH

class Brain(val inputs:Array[Sensor], val outputs:Array[Effector], val initialWeights:Option[Array[Double]] = None) {

  val network = new BasicNetwork
  network.addLayer(new BasicLayer(null,true, inputs.size))
  // network.addLayer(new BasicLayer(new ActivationTANH,true,inputs.size))
  network.addLayer(new BasicLayer(new ActivationTANH,true,outputs.size))
  network.addLayer(new BasicLayer(new ActivationTANH,true,outputs.size))
  network.getStructure.finalizeStructure()
  /*val pattern = new ElmanPattern
  pattern.setActivationFunction(new ActivationTANH)
  pattern.setInputNeurons(inputs.size)
  pattern.addHiddenLayer(inputs.size)
  pattern.setOutputNeurons(outputs.size)
  val network = pattern.generate().asInstanceOf[BasicNetwork]*/

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
