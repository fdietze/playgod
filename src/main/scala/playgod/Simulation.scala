package playgod

import org.lwjgl.opengl.GL11._
import org.lwjgl.opengl.Display
import org.jbox2d.common.Vec2
import org.lwjgl.input.Mouse

object Simulation {
  import Main.renderArea.{r,t,translation,zoom,fps}
  var subSteps = 10
  var generation = 0

  var arrowDirection = 0
  var autoArrowDirections = true
  def generationLifeTime = creature.maxSimulationSteps
  def simulationTimeStep = creature.simulationTimeStep
  def arrowChangeInterval = generationLifeTime / 6

  val creature = new Box2DCreature
  val population = new Population(creature)


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
  }
}
