package playgod

import concurrent.{Await, Promise}
import concurrent.duration.Duration
import org.jbox2d.dynamics._
import joints.{RevoluteJoint, RevoluteJointDef}
import playgod.Box2DTools._
import playgod.Box2DTools.MathHelpers._
import org.jbox2d.collision.shapes.PolygonShape
import org.jbox2d.common.MathUtils._
import collection.{mutable, SortedMap, MapProxy, SeqProxy}
import org.encog.neural.networks.BasicNetwork
import org.encog.neural.pattern.ElmanPattern
import org.encog.neural.networks.layers.BasicLayer
import org.encog.engine.network.activation.ActivationTANH
import playgod.RandomTools._

class Genome(val chromosomes:SortedMap[String,Chromosome]) extends MapProxy[String,Chromosome] {
  def self = chromosomes
  lazy val genes:IndexedSeq[Double] = chromosomes.values.flatMap(_.genes).toIndexedSeq

  def apply[C <: Chromosome](key:String) = super.apply(key).asInstanceOf[C]
  def update(newGenes:IndexedSeq[Double]):Genome = {
    require(newGenes.size == chromosomes.values.map(_.size).sum)

    val newChromosomes = SortedMap.newBuilder[String,Chromosome]
    var i = 0
    for( (key,chromosome) <- chromosomes ) {
      newChromosomes += (key -> chromosome.update(newGenes.slice(i,i+chromosome.size)))
      i += chromosome.size
    }
    new Genome(newChromosomes.result())
  }

  def mutate(strength:Double) = {
    val newGenes = genes.toArray
    update(newGenes.map(x => x + rGaussian*strength))
  }

  def crossover(that:Genome) = {
    val crossoverPoint = rDouble

    val offspringGenes = (this.genes zip that.genes) map {case (a,b) => a*crossoverPoint + b*(1-crossoverPoint)}
    update(offspringGenes)
  }

  var isElite = false
}


class GenomeBuilder extends mutable.MapProxy[String, ChromosomeBuilder] {
  val self = new mutable.HashMap[String,ChromosomeBuilder] {
    override def default(key:String) = {
      val newChromosome = new NamedChromosomeBuilder()
      this += (key -> newChromosome)
      newChromosome
    }
  }

  def update(key:String, value:IndexedSeq[Double]) = {
    val newChromosome = new PlainChromosomeBuilder()
    newChromosome.genes = value
    this += (key -> newChromosome)
  }

  def apply[C <: ChromosomeBuilder](key:String) = super.apply(key).asInstanceOf[C]
  def toGenome = new Genome(SortedMap.empty[String,Chromosome] ++ self.map{ case (k,v) => (k, v.toChromosome) } )
}


abstract class Chromosome {
  def genes:IndexedSeq[Double]
  def size:Int
  def update(newGenes:IndexedSeq[Double]):Chromosome
}

abstract class ChromosomeBuilder {
  def toChromosome:Chromosome
}

class PlainChromosomeBuilder extends ChromosomeBuilder {
  var genes:IndexedSeq[Double] = null
  def toChromosome = new PlainChromosome(genes)
}

class PlainChromosome(val genes:IndexedSeq[Double]) extends Chromosome with SeqProxy[Double] {
  def self = genes
  def update(newGenes:IndexedSeq[Double]) = {
    require(newGenes.size == genes.size)
    new PlainChromosome(newGenes)
  }
}

class NamedChromosomeBuilder extends ChromosomeBuilder with mutable.MapProxy[String,Double] {
  val self = new mutable.HashMap[String,Double]
  def toChromosome = new NamedChromosome(SortedMap.empty[String,Double] ++ self)
}

class NamedChromosome(val namedGenes:SortedMap[String,Double]) extends Chromosome with MapProxy[String,Double] {
  def self = namedGenes
  def genes = namedGenes.values.toIndexedSeq
  def update(newGenes:IndexedSeq[Double]) = {
    require(newGenes.size == namedGenes.size)
    val newNamedGenes = SortedMap.newBuilder[String,Double]
    for ( ((name,oldGene),i) <- namedGenes.zipWithIndex )
      newNamedGenes += (name -> newGenes(i))
    new NamedChromosome(newNamedGenes.result())
  }
}






