package playgod

import org.lwjgl.opengl.GL11._
import org.lwjgl.opengl.Display
import org.lwjgl.opengl.DisplayMode
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse

import org.jbox2d.common._
import org.jbox2d.collision.AABB
import org.jbox2d.callbacks.QueryCallback
import org.jbox2d.dynamics.Fixture
import org.jbox2d.dynamics.joints.{MouseJoint, MouseJointDef}

import swing._
import event._
import Box2DTools._
import collection.mutable
import org.encog.util.concurrency.{EngineConcurrency, TaskGroup}


object Main extends SimpleSwingApplication {

  def genetic() {
    import org.encog.ml.genetic.genome.Genome
    import org.encog.ml.genetic.population.BasicPopulation
    import org.encog.ml.genetic.BasicGeneticAlgorithm
    import org.encog.ml.genetic.mutate.MutateShuffle
    import org.encog.ml.genetic.crossover.SpliceNoRepeat
    import org.encog.ml.genetic.genome.{CalculateGenomeScore, Chromosome, BasicGenome}
    import org.encog.ml.genetic.genes.IntegerGene

    class TestGenome extends BasicGenome {

      val pathChromosome = new Chromosome
      val initialOrganism = new Array[Int](10)


      getChromosomes.add(this.pathChromosome)

      for( i <- 0 until initialOrganism.length ) {
        val gene = new IntegerGene()
        gene.setValue(initialOrganism(i))
        pathChromosome.getGenes.add(gene)
      }
      setOrganism(initialOrganism)

      encode()

      override def decode() {
        val chromosome = this.getChromosomes().get(0)
        val organism = new Array[Int](10)

        for( i <- 0 until chromosome.size )
        {
          val gene = chromosome.get(i).asInstanceOf[IntegerGene]
          organism(i) = gene.getValue()
        }

        setOrganism(organism)
      }

      override def encode() {
        val chromosome = this.getChromosomes().get(0)

        val organism = getOrganism.asInstanceOf[Array[Int]]

        for( i <- 0 until chromosome.size )
        {
          val gene = chromosome.get(i).asInstanceOf[IntegerGene]
          gene.setValue(organism(i))
        }
      }

    }

    class TestScore extends CalculateGenomeScore {
      def calculateScore(genome:Genome):Double = {
        var result = 0.0

        val organism = genome.getOrganism.asInstanceOf[Array[Int]]

        for ( i <- 0 until organism.size) {
          result += (math.pow(i,2) - organism(i)).abs
        }

        return result
      }

      def shouldMinimize = false
    }


    val genetic = new BasicGeneticAlgorithm()

    val score =  new TestScore
    genetic.setCalculateScore(score)

    val populationSize = 20
    val population = new BasicPopulation(populationSize)
    genetic.setPopulation(population)

    for( i <- 0 until populationSize ) {
      val genome = new TestGenome
      genetic.getPopulation.add(genome)
      genetic.calculateScore(genome)
    }

    population.claim(genetic)
    population.sort()



    genetic.setMutationPercent(0.1)
    genetic.setPercentToMate(0.24)
    genetic.setMatingPopulation(0.5)
    genetic.setCrossover(new SpliceNoRepeat(3))
    genetic.setMutate(new MutateShuffle())

    for ( i <- 0 until 100 ) {
      genetic.iteration()
      val thisSolution = genetic.getPopulation().getBest().getScore()
      println(s"$i: $thisSolution")
    }
  }

  genetic()
  System.exit(0)








  val width = 800
  val height = 600
  
  var running = true
  val fps = 60
  
  var zoom = 1f/10f
  def r = width * 0.5f * zoom
  def t = height * 0.5f * zoom
  val n = -1f
  val f = 1f
  var translation = new Vec2(0,-200)
  def mousePos = new Vec2(Mouse.getX,Mouse.getY)
  def box2dMousePos = mousePos.sub(translation).mul(zoom).add(new Vec2(-r, -t))

  var subSteps = 10
  val obstacleCount = 10
  var generation = 0

  var arrowDirection = 0
  var autoArrowDirections = true
  def generationLifeTime = creature.maxSimulationSteps
  def simulationTimeStep = creature.simulationTimeStep
  def arrowChangeInterval = generationLifeTime / 6

  val creature = new Box2DCreature
  val population = new Population(creature)
  val bestScoreStats = new mutable.ArrayBuffer[Double]
  val avgScoreStats = new mutable.ArrayBuffer[Double]
  val worstScoreStats = new mutable.ArrayBuffer[Double]

  val statsHeight = 0.2f
  val statsRange = width
  
  val renderArea = new LWJGLComponent(new Dimension(width, height))
  val drawBestCheckBox = new CheckBox("elite")
  val top = new swing.MainFrame {
    val panel = new BoxPanel(Orientation.Vertical) {
      contents += new BoxPanel(Orientation.Horizontal) {
        contents += drawBestCheckBox
        contents += new Label("sub: ")
        contents += new TextField {
          maximumSize = new Dimension(50,50)
          text = subSteps.toString
          listenTo(this)
          reactions += {
            case e:EditDone =>
              subSteps = this.text.toInt
          }
        }

        contents += new Label("time: ")
        contents += new TextField {
          maximumSize = new Dimension(50,50)
          text = creature.simulationTimeStep.toString.take(5)
          listenTo(this)
          reactions += {
            case e:EditDone =>
              creature.simulationTimeStep = this.text.toFloat
          }
        }

        contents += new Label("age: ")
        contents += new TextField {
          maximumSize = new Dimension(50,50)
          text = creature.maxSimulationSteps.toString
          listenTo(this)
          reactions += {
            case e:EditDone =>
              creature.maxSimulationSteps = this.text.toInt
          }
        }

        contents += new Label("popsize: ")
        contents += new TextField {
          maximumSize = new Dimension(50,50)
          text = population.populationSize.toString
          listenTo(this)
          reactions += {
            case e:EditDone =>
              population.populationSize = this.text.toInt
          }
        }

        contents += new Label("cross %: ")
        contents += new TextField {
          maximumSize = new Dimension(50,50)
          text = population.crossoverProbability.toString
          listenTo(this)
          reactions += {
            case e:EditDone =>
              population.crossoverProbability = this.text.toDouble
          }
        }

        contents += new Label("mut %: ")
        contents += new TextField {
          maximumSize = new Dimension(50,50)
          text = population.mutationProbability.toString
          listenTo(this)
          reactions += {
            case e:EditDone =>
              population.mutationProbability = this.text.toDouble
          }
        }

        contents += new Label("mut: ")
        contents += new TextField {
          maximumSize = new Dimension(50,50)
          text = population.mutationStrength.toString
          listenTo(this)
          reactions += {
            case e:EditDone =>
              population.mutationStrength = this.text.toDouble
          }
        }

        contents += new Label("elitism: ")
        contents += new TextField {
          maximumSize = new Dimension(50,50)
          text = population.elitism.toString
          listenTo(this)
          reactions += {
            case e:EditDone =>
              population.elitism = this.text.toDouble
          }
        }
      }
      contents += renderArea
      
      requestFocus()
      listenTo(keys)
      reactions += {
        case KeyPressed(_, Key.Escape, _, _) =>
          quit()
      }

    }
    contents = panel
    panel.requestFocus() // be able to listen to key events
  }

  Display.setParent(renderArea.canvas)
  Display.setVSyncEnabled(false)
  Display.create()

  override def main(args:Array[String]) {
    super.main(args)
    start()
  }
  
  def drawStats() {
    val padding = 0.01f

    glPushMatrix()

    glLoadIdentity()
    glTranslatef(-1, -1, 0)
    glScalef(2f, 2f, 1f)

    class Color(r:Float, g:Float, b:Float) {
      def this(r:Int, g:Int, b:Int) = this(r/255f, g/255f, b/255f)
      def this(c:Int) = this((c & 0xFF0000) >> 16, (c & 0xFF00) >> 8, c & 0xFF)
      def glColor() = glColor3f(r,g,b)
    }


    glBegin(GL_LINES)
      glColor3f(91f/255f, 177f/255f, 189f/255f)
      glVertex2f(0, (1-2*padding-statsHeight).toFloat)
      glVertex2f(1, (1-2*padding-statsHeight).toFloat)
      
      (new Color(0x7AECFC)).glColor // age
      glVertex2f(0, (1-2*padding-statsHeight).toFloat)
      glVertex2f(population.organisms.head.asInstanceOf[Box2DSimulationOrganism].age.toFloat/population.organisms.head.asInstanceOf[Box2DSimulationOrganism].maxSteps, (1-2*padding-statsHeight).toFloat)
    glEnd()

    if( bestScoreStats.size == 0 ) {glPopMatrix(); return}
    def bestData = bestScoreStats.takeRight(statsRange)
    def avgData = avgScoreStats.takeRight(statsRange)
    def worstData = worstScoreStats.takeRight(statsRange)
    
    val min = worstData.min
    val max = bestData.max
    val range = max - min
    

    for( (data,color) <- List((worstData,new Color(0xFFB4B0)), (avgData,new Color(242,200,148)), (bestData,new Color(0xF5FFA3))) ) {
      glBegin(GL_LINE_STRIP)
        color.glColor()
        if( bestScoreStats.size == 1 ) {
          val score = data(0)
          glVertex2f(0f, (1-padding-statsHeight+(score - min) / range * statsHeight).toFloat)
          glVertex2f(1f, (1-padding-statsHeight+(score - min) / range * statsHeight).toFloat)
        }
        else
          for( (score, i) <- data.zipWithIndex )
            glVertex2f(i.toFloat/(data.size-1), (1-padding-statsHeight+(score - min) / range * statsHeight).toFloat)
      glEnd()
    }

    glPopMatrix()
    
  }


  def start() {
    import Box2DTools._

    glClearColor(0.3f, 0.3f, 0.3f, 1f)

    glMatrixMode(GL_PROJECTION_MATRIX)
    glLoadIdentity()

    glMatrixMode(GL_MODELVIEW_MATRIX)
    
    
    
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
        top.title = "%8.0fsteps/s  %5.2fs/generation  %8.3ffps" format (stepsps, generationTime, fps)
        frameCount = 0
        stepCount = 0
        lastFpsUpdate = now
      }
    }
    
    var mouseJoint:Option[MouseJoint] = None
    var dragging = false
    def processEvents() {
      import Mouse._
      while( Mouse.next ) {
        ( getEventButton, getEventButtonState ) match {
          case (0 , true) => // left down
            /*for( organism <- population.organisms.map(_.asInstanceOf[Box2DSimulationOrganism]) ) {
              val world = organism.world
              
              val tolerance = new Vec2(0.01f, 0.01f)
              val toleranceArea = new AABB(box2dMousePos.sub(tolerance), box2dMousePos.add(tolerance))
                world.queryAABB(new QueryCallback {
                  def reportFixture(fixture:Fixture):Boolean = {
                    if( fixture.getDensity == 0f ) return true
                    
                    val body = fixture.getBody
                    val mouseJointDef = new MouseJointDef
                    mouseJointDef.bodyA = body
                    mouseJointDef.bodyB = body
                    mouseJointDef.target.set(box2dMousePos)
                    mouseJointDef.maxForce = 1000f * body.getMass
                    mouseJoint = Some(world.createJoint(mouseJointDef).asInstanceOf[MouseJoint])
                    body.setAwake(true)
                    
                    return false // cancel iteration
                  }
                }, toleranceArea)
            }*/
            if( !mouseJoint.isDefined ) {
              dragging = true
            }
          case (0 , false) => // left up
            /*if( mouseJoint.isDefined ) {
              for( organism <- population.organisms.map(_.asInstanceOf[Box2DSimulationOrganism]) ) {
                val world = organism.world
                world.destroyJoint(mouseJoint.get)
              }
              mouseJoint = None
            }*/
            dragging = false
          case (1 , true) => // right down
          case (1 , false) => // right up
          case (-1, false) => // wheel
            val delta = Mouse.getDWheel
            if( delta > 0 )
              zoom /= 1.1f
            else if( delta < 0 )
              zoom *= 1.1f
          case _ =>
        }
      }
      
      if( mouseJoint.isDefined ) {
        mouseJoint.get.setTarget(box2dMousePos)
      }
      if( dragging ) {
        translation.x += getDX
        translation.y += getDY
      }
      
      while(Keyboard.next) {
        val key = Keyboard.getEventKey
        if( Keyboard.getEventKeyState ) { // Key down
          if( key == Keyboard.KEY_R) population.reset()
          if( key == Keyboard.KEY_LEFT) arrowDirection = -1
          if( key == Keyboard.KEY_RIGHT) arrowDirection = 1
          if( key == Keyboard.KEY_A) autoArrowDirections = !autoArrowDirections
        } else { // Key up
          if( key == Keyboard.KEY_LEFT) arrowDirection = 0
          if( key == Keyboard.KEY_RIGHT) arrowDirection = 0
        }
        println("arrowDirection: " + arrowDirection)
      }
    }

    var i = 0
    var lastBestScore = Double.MinValue
    while(running) {
      processEvents()
      
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
          bestScoreStats += bestScore
          avgScoreStats += avgScore
          worstScoreStats += worstScore
          population.nextGeneration()
          generation += 1
        }
      }

      glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT)
      glLoadIdentity()
      glScalef(1 / r, 1 / t, 1f)
      glTranslatef(translation.x*zoom, translation.y*zoom, 0)

      if( drawBestCheckBox.selected )
        population.organisms.filter(_.genome.isElite).foreach(_.asInstanceOf[Box2DSimulationOrganism].debugDraw())
      else
        population.organisms.foreach(_.asInstanceOf[Box2DSimulationOrganism].debugDraw())
      drawStats()

      Display.update()
      Display.sync(fps)
      updateFps()
    }

    Display.destroy()
  }
}
