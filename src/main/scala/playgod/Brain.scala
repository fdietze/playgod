package playgod

import org.encog.neural.networks.BasicNetwork
import org.encog.neural.networks.layers.BasicLayer
import org.encog.engine.network.activation.ActivationTANH
import org.encog.neural.neat.NEATNetwork
import collection.JavaConversions._
import org.encog.ml.data.MLData
import org.encog.ml.data.basic.BasicMLData
import org.encog.ml.MLMethod

abstract class Brain {
  type Network = MLMethod { def compute(input:MLData):MLData }
  val network:Network
  val inputs:Array[Sensor]
  val outputs:Array[Effector]
  def think() {
    if (network == null) return
    val inputValues = new BasicMLData(inputs.map(_.getValue))
    val outputValues = network.compute(inputValues).getData
    for( (effector,param) <- outputs zip outputValues )
      effector.act(param)
  }
  def update(newNetwork:Network) = {
    def in = inputs
    def out = outputs
    new Brain {
      val inputs = in
      val outputs = out
      val network = newNetwork
    }
  }
}

class NeatBrain(val inputs:Array[Sensor], val outputs:Array[Effector]) extends Brain {
  val network:NEATNetwork = null
}


class FeedForwardBrain(val inputs:Array[Sensor], val outputs:Array[Effector], val initialWeights:Option[Array[Double]] = None) extends Brain {

  val network = new BasicNetwork
  network.addLayer(new BasicLayer(null,false, inputs.size))
  network.addLayer(new BasicLayer(new ActivationTANH,false,inputs.size/8))
  //network.addLayer(new BasicLayer(new ActivationTANH,true,inputs.size))
  network.addLayer(new BasicLayer(new ActivationTANH,false,outputs.size))
  network.getStructure.finalizeStructure()

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
