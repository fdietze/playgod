package playgod

import org.jbox2d.common._
import org.jbox2d.dynamics._
import org.jbox2d.dynamics.joints._
import Box2DTools._

import collection.mutable
import math._

class Population {
  var creatures = new mutable.ArrayBuffer[Creature]
  def brains = creatures map (_.brain)
  
  def evolution() {
    val n = (creatures.size * 0.1).toInt
    val newBrains = brains.sortBy(_.score).reverse
    val best = newBrains.take(n)
    val worst = newBrains.drop(n)
    for( brain <- worst ) {
      def rInt = util.Random.nextInt.abs
      def rGaussian = util.Random.nextGaussian
      val newWeights = best(rInt % best.size).getWeights
      for( i <- 0 until newWeights.size )
        newWeights(i) += rGaussian*0.2

      brain.replaceWeights(newWeights)
    }
    creatures.foreach(_.reset())
 }
}
