package playgod

import org.lwjgl.opengl.GL11._
import org.lwjgl.opengl.Display
import org.jbox2d.common.Vec2
import org.lwjgl.input.Mouse
import org.encog.ml.ea.population.{BasicPopulation, Population}
import org.encog.ml.ea.species.BasicSpecies
import org.encog.ml.genetic.genome.{ArrayGenome, DoubleArrayGenome, DoubleArrayGenomeFactory}
import org.encog.ml.ea.genome.{BasicGenome, GenomeFactory}
import org.encog.ml.{MLMethod, CalculateScore}
import org.encog.ml.ea.train.basic.TrainEA
import org.encog.ml.genetic.crossover.Splice
import org.encog.ml.genetic.mutate.MutatePerturb
import collection.mutable
import scala.collection.JavaConversions._
import org.encog.util.EngineArray
import concurrent.Await
import concurrent.duration.Duration
import concurrent.ExecutionContext.Implicits.global
import org.encog.neural.hyperneat.substrate.SubstrateFactory
import org.encog.neural.neat.{NEATUtil, NEATPopulation, NEATNetwork}
import org.encog.ml.data.basic.BasicMLData
import org.encog.neural.neat.training.species.OriginalNEATSpeciation

case object Reset
case class BrainUpdate(brain:Brain)

object LiveSimulation extends SimpleActor {
  import Main.renderArea.{r,t,translation,zoom,fps}
  val creature = new Box2DCreature
  val organism = creature.create
  val initialBoneStates = organism.boneStates
  organism.maxSteps = Int.MaxValue
  private var running = true
  def start() {
    running = true
    while( running ) {
      organism.step()

      glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT)
      glLoadIdentity()
      glScalef(1 / r, 1 / t, 1f)
      glTranslatef(translation.x*zoom, translation.y*zoom, 0)

      organism.debugDraw()
      //Stats.draw()

      Display.update()
      Display.sync(fps)
      //updateFps()

      InputEvents.processEvents()
      processMessages()
    }
  }

  def stop() { running = false }
  def receive = {
    case BrainUpdate(brain) =>
      organism ! BrainUpdate(brain)
      //this ! Reset

    case Reset =>
      organism.setBoneStates(initialBoneStates)
  }
}

object NeatSimulation {
  def start() {
    val creature = new Box2DCreature
    val dummyBrain = creature.create.brain
    def currentBoneStates = LiveSimulation.organism.boneStates
    var scoreCalculations = 0

    val score = new CalculateScore {

      def calculateScore(phenotype:MLMethod) = {
        scoreCalculations += 1
        val network = phenotype.asInstanceOf[NEATNetwork]
        val organism = creature.create

        organism ! BrainUpdate(organism.brain.update(network))
        organism.maxSteps = 120
        organism.setBoneStates(currentBoneStates)
        organism.finish()
        -organism.score
      }

      def shouldMinimize(): Boolean = true
      def requireSingleThreaded(): Boolean = false
    }
    val pop = new NEATPopulation(dummyBrain.inputs.size,dummyBrain.outputs.size,100)
    //pop.setActivationCycles(5)
    pop.setInitialConnectionDensity(0.5)
    pop.setSurvivalRate(0.0)
    pop.reset()

    val train = NEATUtil.constructNEATTrainer(pop,score)

    val speciation = new OriginalNEATSpeciation()
    //speciation.setCompatibilityThreshold(1)
    //speciation.setMaxNumberOfSpecies(1)
    train.setSpeciation(speciation)

    var lastBest = Double.MaxValue
    for( i <- 0 until 100000 ) {
      scoreCalculations = 0
      //currentBoneStates = LiveSimulation.organism.boneStates
      train.iteration()
      val bestGenome = train.getBestGenome
      val best = train.getCODEC().decode(bestGenome).asInstanceOf[NEATNetwork]
      val bestScore = bestGenome.getScore
      //if( bestScore < lastBest ) {
        lastBest = bestScore

      for( species <- pop.getSpecies.par ) {
        val leader = species.getLeader
        val leaderNet = train.getCODEC().decode(leader).asInstanceOf[NEATNetwork]
        leader.setScore(score.calculateScore(leaderNet))
      }
      bestGenome.setScore(score.calculateScore(best))

      val currentBestGenome = train.getBestGenome
      val currentBest = train.getCODEC().decode(currentBestGenome).asInstanceOf[NEATNetwork]
      val currentBestScore = currentBestGenome.getScore
      println("i: %d best: %5.3f, links: %d, scored: %d, species: %d" format (
        i, currentBestScore, currentBest.getLinks.size, scoreCalculations, pop.getSpecies.size))
      LiveSimulation ! BrainUpdate(LiveSimulation.organism.brain.update(currentBest))
      //}
    }
  }
}

object GeneticSimulation {
  val creature = new Box2DCreature
  val populationSize = 50
  var generation = 0

  var arrowDirection = 0
  var autoArrowDirections = true
  def generationLifeTime = creature.maxSimulationSteps
  def simulationTimeStep = creature.simulationTimeStep
  def arrowChangeInterval = generationLifeTime / 6

  def start() {
    import org.encog.ml.ea.genome.Genome

    import Main.renderArea.{r,t,translation,zoom,fps}
    val genomeSize = creature.genome.genes.size
    var currentBoneStates = LiveSimulation.organism.boneStates

    val genomeFactory = new GenomeFactory() {
      def factor = new DoubleArrayGenome(genomeSize)
      def factor(genome:Genome):Genome = ???
      def factorRandom = {
        val newGenome = factor
        val genome = creature.randomGenome
        genome.genes.copyToArray(newGenome.getData)
        newGenome
      }
    }

    val population:Population = new BasicPopulation(populationSize, genomeFactory)
    val defaultSpecies = new BasicSpecies()
    defaultSpecies.setPopulation(population)

    for( i <- 0 until populationSize ) {
      val newGenome = genomeFactory.factorRandom
      newGenome.setPopulation(population)
      defaultSpecies.getMembers.add(newGenome)
    }
    population.getSpecies.add(defaultSpecies)

    val GenomeScore = new CalculateScore {
      override def calculateScore(phenotype:MLMethod) = {
        val genome = phenotype.asInstanceOf[DoubleArrayGenome]
        creature.genome = creature.genome.update(genome.getData)
        val organism = creature.create
        organism.maxSteps = 500
        organism.setBoneStates(currentBoneStates)
        organism.finish()
        -organism.score
      }
      def shouldMinimize = true
      def requireSingleThreaded = false
    }

    val genetic = new TrainEA(population, GenomeScore)
    genetic.addOperation(0.9, new Splice(genomeSize / 3))
    genetic.addOperation(0.1, new MutatePerturb(0.2))

    //genetic.iteration()
    println("running:")
    for ( i <- 0 until 50000 ) {
      currentBoneStates = LiveSimulation.organism.boneStates
      genetic.iteration()

      val all = genetic.getPopulation.flatten.map(_.getScore).sorted.map("%5.2f" format _)
      //println(s"$i: $all")
      val best = genetic.getBestGenome.asInstanceOf[DoubleArrayGenome]
      println(f"$i%d: best: ${best.getScore}%5.2f")

      creature.dummyBrain.replaceWeights(best.getData)
      LiveSimulation ! BrainUpdate(LiveSimulation.organism.brain.update(creature.dummyBrain.network))
      best.setScore(-LiveSimulation.organism.currentScore)
    }

    println("done")
    Display.destroy()

  }


/*  val population = new Population(creature)

  def start() {
    var running = true
    def now = System.currentTimeMillis
    var lastFpsUpdate = now
    var frameCount = 0
    var stepCount = 0

    def updateFps() {
      frameCount += 1
      //stepcount is updated in while loop
      val timeDiff = (now - lastFpsUpdate) / 1000.0
      if( timeDiff > 1.0 ) {
        val fps = frameCount / timeDiff
        val stepsps = stepCount / timeDiff
        val generationTime = (generationLifeTime * population.populationSize) / stepsps
        Main.top.title = "%8.0fsteps/s  %5.2fs/generation  %8.3ffps" format (stepsps, generationTime, fps)
        frameCount = 0
        stepCount = 0
        lastFpsUpdate = now
      }
    }

    var i = 0
    var lastBestScore = Double.MinValue
    while(running) {
      InputEvents.processEvents()

      for( _ <- 0 until subSteps ) {
        if( autoArrowDirections ) {
          val t = i % (3 * arrowChangeInterval)
          if( t < arrowChangeInterval ) arrowDirection = 0
          else if( t < arrowChangeInterval*2 ) arrowDirection = -1
          else /*if( t < arrowChangeInterval*3 )*/ arrowDirection = 1
          //println("i: %d arrowDirection: " + arrowDirection)
        }


        //population.update()
        population.organisms.par.foreach(_.asInstanceOf[Box2DSimulationOrganism].step())
        stepCount += population.populationSize

        i += 1
        if( i % generationLifeTime == 0 ) {
          population.organisms.par.foreach(_.asInstanceOf[Box2DSimulationOrganism].finish())
          val bestScore = population.organisms.maxBy(_.fitness).fitness
          val avgScore = population.organisms.map(_.fitness).sum / population.populationSize
          val worstScore = population.organisms.minBy(_.fitness).fitness

          if( bestScore != lastBestScore ) {
            println("generation: %d, maxScore: %s" format (generation, bestScore))
            lastBestScore = bestScore
          }
          Stats.bestScore += bestScore
          Stats.avgScore += avgScore
          Stats.worstScore += worstScore
          population.nextGeneration()
          generation += 1
        }
      }

      glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT)
      glLoadIdentity()
      glScalef(1 / r, 1 / t, 1f)
      glTranslatef(translation.x*zoom, translation.y*zoom, 0)

      if( Main.top.drawBestCheckBox.selected )
        population.organisms.filter(_.genome.isElite).foreach(_.asInstanceOf[Box2DSimulationOrganism].debugDraw())
      else
        population.organisms.foreach(_.asInstanceOf[Box2DSimulationOrganism].debugDraw())
      Stats.draw()

      Display.update()
      Display.sync(fps)
      updateFps()
    }

    Display.destroy()
  }*/
}
