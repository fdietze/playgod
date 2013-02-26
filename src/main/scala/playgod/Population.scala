package playgod

import org.jbox2d.common._
import org.jbox2d.dynamics._
import org.jbox2d.dynamics.joints._
import Box2DTools._

import collection.mutable
import math._

class Population(val creatureDef:CreatureDefinition) {
  val populationSize = 30
  val parentCount = 2 //TODO: crossover with more parents
  val crossOverProbability = 0.8
  val mutationProbability = 0.1
  val mutationStrength = 0.1
  val elitism = 0.1

  val creatures = Array.fill(populationSize){new CreatureSimulation(creatureDef)}
  def brains = creatures map (_.creature.brain)

  def rInt = util.Random.nextInt.abs
  def rGaussian = util.Random.nextGaussian
  def rDouble = util.Random.nextDouble
  def inCase(probability:Double) = rDouble < probability
  def rouletteWheelSelection[T](seq:IndexedSeq[T], score:(T) => Double) = {
    val min = score(seq.minBy(score))
    val offset = if( min < 0.0 ) min.abs else 0.0
    def offsetScore(a:T) = offset + score(a)
    val sum = seq.map(offsetScore).sum
    val r = rDouble * sum

    var i = 0
    var currentSum = offsetScore(seq(i))
    while( currentSum < r ) {
      i += 1
      currentSum += offsetScore(seq(i))
    }
    seq(i)
  }

  def nextGeneration() {
    val brainData = brains map (_.getData)

    val sortedData = brainData.sortBy(_.score).reverse
    val sortedWeights = sortedData map (_.weights)

    val newWeights = new mutable.ArrayBuffer[Array[Double]]
    def addWeights(weights:Array[Double]) {
      for( (weight,i) <- weights zipWithIndex ) {
        if( inCase(mutationProbability) )
          weights(i) = weight + rGaussian*mutationStrength
      }
      newWeights += weights
    }

    newWeights ++= sortedWeights.take(ceil(elitism*populationSize).toInt)
    while( newWeights.size < populationSize ) {
      if( populationSize - newWeights.size >= 2 ) {
        val parentA = rouletteWheelSelection(sortedData, (e:BrainData) => e.score)
        val parentB = rouletteWheelSelection(sortedData, (e:BrainData) => e.score)
        val childWeights = if( inCase(crossOverProbability) ) {
          val crossOverPoint = rInt % parentA.weights.size
          val childA = parentA.weights.clone
          val childB = parentB.weights.clone
          for( i <- 0 until childA.size )
            if( inCase(0.5) ) {
              childA(i) = parentB.weights(i)
              childB(i) = parentA.weights(i)
            }
          /*Array(
           parentA.weights.take(crossOverPoint) ++ parentB.weights.drop(crossOverPoint),
           parentB.weights.take(crossOverPoint) ++ parentA.weights.drop(crossOverPoint)
          )*/
          Array(childA, childB)
        } else {
          Array(parentA.weights.clone, parentB.weights.clone)
        }
        childWeights.foreach(addWeights)
      } else {
        addWeights(sortedWeights(rInt % populationSize).clone)
      }
    }

    for( (weights,i) <- newWeights zipWithIndex )
      creatures(i) = new CreatureSimulation(creatureDef, Some(weights))
  }

  def update() {
    Physics.update(creatures)
    Physics.step(creatures)
  }

  def draw() {
    Physics.debugDraw(creatures)
  }
}
