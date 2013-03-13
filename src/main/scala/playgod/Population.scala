package playgod

import collection.mutable
import playgod.RandomTools._

/*class Population(val creature:Creature) {
  import math._

  var populationSize = 30
  val parentCount = 2 //TODO: crossover with more parents
  var crossoverProbability = 0.85
  var mutationProbability = 0.01
  var mutationStrength = 0.2
  var elitism = 0.00

  //TODO: Array[O <: Organism]
  var organisms:Array[Organism] = creature.create(populationSize).toArray
  val genome = creature.genome

  def nextGeneration() {
    val sortedOrganisms = organisms.sortBy(_.fitness).reverse
    val sortedGenomes = sortedOrganisms map (_.genome)
    var newGenomes = new mutable.ArrayBuffer[Genome]

    sortedGenomes.foreach(_.isElite = false)
    newGenomes ++= sortedGenomes.take(ceil(elitism*populationSize).toInt)
    newGenomes.foreach(_.isElite = true)
    while( newGenomes.size < populationSize ) {
      if( sortedGenomes.size - newGenomes.size >= 2 ) {
        val parentA = rankSelection(sortedOrganisms, (e:Organism) => e.fitness).genome
        val parentB = rankSelection(sortedOrganisms, (e:Organism) => e.fitness).genome
        val (childA,childB) = if( inCase(crossoverProbability) ) {
          parentA crossover parentB
        } else {
          (parentA, parentB)
        }
        newGenomes += childA.mutate(mutationProbability, mutationStrength)
        newGenomes += childB.mutate(mutationProbability, mutationStrength)
      } else {
        newGenomes += sortedGenomes(rInt % sortedGenomes.size).mutate(mutationProbability, mutationStrength)
      }
    }

    // if lowering populationSize, take only the best ones
    if( populationSize < organisms.size )
      newGenomes = new mutable.ArrayBuffer[Genome] ++ sortedGenomes.take(populationSize)

    // if raising populationSize, fill the new positions with fresh random organisms
    if( organisms.size != populationSize )
      reset()

    for( (genome,i) <- newGenomes zip (0 until populationSize) ) {
      creature.genome = genome
      organisms(i) = creature.create
    }
  }

  def reset() {
    organisms = creature.create(populationSize).toArray
  }
}*/
