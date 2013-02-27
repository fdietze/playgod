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

object Main extends SimpleSwingApplication {
  
  val width = 800
  val height = 600
  
  var running = true
  val fps = 60
  
  var zoom = 1f/10f
  def r = width * 0.5f * zoom
  def t = height * 0.5f * zoom
  val n = -1f
  val f = 1f
  var translation = new Vec2(0,0)
  def mousePos = new Vec2(Mouse.getX,Mouse.getY)
  def box2dMousePos = mousePos.sub(translation).mul(zoom).add(new Vec2(-r, -t))

  var subSteps = 1
  val obstacleCount = 10
  var generationLifeTime = 2000
  var generation = 0

  var arrowDirection = 0
  var autoArrowDirections = true
  def arrowChangeInterval = generationLifeTime / 4

  val population = new Population(CreatureFactory.forky)
  val bestScoreStats = new mutable.ArrayBuffer[Double]
  val worstScoreStats = new mutable.ArrayBuffer[Double]

  val statsHeight = 0.1f
  val statsRange = 30


  val renderArea = new LWJGLComponent(new Dimension(width, height))
  val drawBestCheckBox = new CheckBox("draw best")
  val top = new swing.MainFrame {
    val panel = new BoxPanel(Orientation.Vertical) {
      contents += new BoxPanel(Orientation.Horizontal) {
        contents += drawBestCheckBox
        /*contents += new Button("Random Box") {
          action = new Action("Random Box") {
            override def apply() {
              createBox(Physics.world, new Vec2(util.Random.nextGaussian().toFloat, 10), hx = 1f, hy = 1f)
           }
          }
        }*/

        contents += new Label("substeps: ")
        contents += new TextField {
          maximumSize = new Dimension(50,50)
          text = subSteps.toString
          listenTo(this)
          reactions += {
            case e:EditDone =>
              subSteps = this.text.toInt
          }
        }

        contents += new Label("age: ")
        contents += new TextField {
          maximumSize = new Dimension(50,50)
          text = generationLifeTime.toString
          listenTo(this)
          reactions += {
            case e:EditDone =>
              generationLifeTime = this.text.toInt
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
    if( bestScoreStats.size <= 1 ) return
    def bestData = bestScoreStats.takeRight(statsRange)
    def worstData = worstScoreStats.takeRight(statsRange)
    
    val min = worstData.min
    val max = bestData.max
    val range = max - min
    
    val padding = 0.01f

    glPushMatrix()


    glLoadIdentity()
    glTranslatef(-1, -1, 0)
    glScalef(2f, 2f, 1f)

    glBegin(GL_LINES)
      glColor3f(91f/255f, 177f/255f, 189f/255f)
      glVertex2f(0, (1-2*padding-statsHeight).toFloat)
      glVertex2f(1, (1-2*padding-statsHeight).toFloat)
    glEnd()

    glBegin(GL_LINE_STRIP)
      glColor3f(148f/255f, 232f/255f, 242f/255f)
      for( (score, i) <- bestData zipWithIndex )
      glVertex2f(i.toFloat/(bestData.size-1), (1-padding-statsHeight+(score - min) / range * statsHeight).toFloat)
    glEnd()

    glBegin(GL_LINE_STRIP)
      glColor3f(242f/255f, 200f/255f, 148f/255f)
      for( (score, i) <- worstData zipWithIndex )
      glVertex2f(i.toFloat/(worstData.size-1), (1-padding-statsHeight+(score - min) / range * statsHeight).toFloat)
    glEnd()

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
    def updateFps() {
      frameCount += 1
      val timeDiff = (now - lastFpsUpdate) / 1000.0
      if( timeDiff > 1.0 ) {
        val fps = frameCount / timeDiff
        top.title = "%8.3ffps" format fps
        frameCount = 0
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
            val tolerance = new Vec2(0.01f, 0.01f)
            val toleranceArea = new AABB(box2dMousePos.sub(tolerance), box2dMousePos.add(tolerance))
            /*Physics.world.queryAABB(new QueryCallback {
              def reportFixture(fixture:Fixture):Boolean = {
                val body = fixture.getBody
                val mouseJointDef = new MouseJointDef
                //mouseJointDef.bodyA = Physics.ground
                mouseJointDef.bodyB = body
                mouseJointDef.target.set(box2dMousePos)
                mouseJointDef.maxForce = 1000f * body.getMass
                //mouseJoint = Some(Physics.world.createJoint(mouseJointDef).asInstanceOf[MouseJoint])
                body.setAwake(true)
                
                return false // cancel iteration
              }
            }, toleranceArea)*/
            if( !mouseJoint.isDefined ) {
              dragging = true
            }
          case (0 , false) => // left up
            if( mouseJoint.isDefined ) {
              //Physics.world.destroyJoint(mouseJoint.get)
              mouseJoint = None
            }
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
          //if( key == Keyboard.KEY_E) Physics.population.evolution()
          //if( key == Keyboard.KEY_R) Physics.population.creatures.foreach(_.reset())
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
          val t = i % (2 * arrowChangeInterval)
          if( t < arrowChangeInterval ) arrowDirection = -1
          else /*if( t < arrowChangeInterval*2 )*/ arrowDirection = 1
          //else /*if( t < arrowChangeInterval*3 )*/ arrowDirection = 0
          //println("i: %d arrowDirection: " + arrowDirection)
        }
        
        
        population.update()
        
        i += 1
        if( i % generationLifeTime == 0 ) {
          val bestScore = population.brains.maxBy(_.score).score
          val worstScore = population.brains.minBy(_.score).score
          if( bestScore != lastBestScore ) {
            println("generation: %d, maxScore: %s" format (generation, bestScore))
            lastBestScore = bestScore
          }
          bestScoreStats += bestScore
          worstScoreStats += worstScore
          population.nextGeneration()
          generation += 1
        }
      }

      glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT)
      glLoadIdentity()
      glScalef(1 / r, 1 / t, 1f)
      glTranslatef(translation.x*zoom, translation.y*zoom, 0)

      population.draw(drawBestCheckBox.selected)
      drawStats()

      Display.update()
      Display.sync(fps)
      updateFps()
    }

    Display.destroy()
  }
}
