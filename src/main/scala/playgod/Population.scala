package playgod

import org.jbox2d.common._
import org.jbox2d.dynamics._
import org.jbox2d.dynamics.joints._
import Box2DTools._

import collection.mutable
import math._

class Population(val creatureDef:CreatureDefinition, val count:Int) {
  val creatures = Array.fill(count){new CreatureSimulation(creatureDef)}
  def brains = creatures map (_.creature.brain)

  def rInt = util.Random.nextInt.abs
  def rGaussian = util.Random.nextGaussian
  def rDouble = util.Random.nextDouble

  val survivorCount = (count * 0.3).ceil.toInt

  def update() {
    Physics.update(creatures)
    Physics.step(creatures)
  }

  def draw() {
    Physics.debugDraw(creatures)
  }

  def nextGeneration() {
    val brainData = brains map (_.getData)

    val sortedData = brainData.sortBy(_.score).reverse
    val best = sortedData.take(survivorCount)
    val worst = sortedData.drop(survivorCount)

    for( (BrainData(weights,_),i) <- worst zipWithIndex ) {
      val newWeights = best(i % best.size).weights
      
      for( j <- 0 until weights.size )
        weights(j) += newWeights(j) + rGaussian*0.0001
    }

    for( (BrainData(weights,_), i) <- brainData zipWithIndex )
      creatures(i) = new CreatureSimulation(creatureDef, Some(weights))
  }
}
