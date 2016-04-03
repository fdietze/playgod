package playgod

import collection.mutable
import playgod.RandomTools._

class Population(val creature:Creature) {
  import math._

  var populationSize = 30
  val parentCount = 2 //TODO: crossover with more parents
  var crossoverProbability = 0.3
  var mutationStrength = 0.005
  var elitism = 0.01

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
    while( newGenomes.size < populationSize )  {
      if( inCase(crossoverProbability) ) {
        val parentA = tournamentSelection(sortedOrganisms, (e:Organism) => e.fitness).genome
        val parentB = tournamentSelection(sortedOrganisms, (e:Organism) => e.fitness).genome
        newGenomes += (parentA crossover parentB)
      } else {
        newGenomes += tournamentSelection(sortedOrganisms, (e:Organism) => e.fitness).genome.mutate(mutationStrength)
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
}
