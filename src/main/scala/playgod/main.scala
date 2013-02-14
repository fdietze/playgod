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

object Main extends SimpleSwingApplication {

  val renderArea = new LWJGLComponent(new Dimension(800,600))
  val top = new swing.MainFrame {
    val panel = new BoxPanel(Orientation.Vertical) {
      contents += new BoxPanel(Orientation.Horizontal) {
        contents += new Button("Random Box") {
          action = new Action("Random Box") {
            override def apply() {
              createBox(Physics.world, new Vec2(util.Random.nextGaussian().toFloat, 10), hx = 1f, hy = 1f)
           }
          }
        }
        
        contents += new Label("Substeps: ")
        contents += new TextField {
          maximumSize = new Dimension(50,50)
          text = "1"
          listenTo(this)
          reactions += {
            case e:EditDone =>
              subSteps = this.text.toInt
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



  var running = true
  val fps = 60
  val box2dStep = 1f / 60f
  var subSteps = 1
  var zoom = 1f/10f
  def r = 400 * zoom
  def t = 300 * zoom
  val n = -1f
  val f = 1f
  var translation = new Vec2(0,0)
  def mousePos = new Vec2(Mouse.getX,Mouse.getY)
  def box2dMousePos = mousePos.sub(translation).mul(zoom).add(new Vec2(-r, -t))
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
            Physics.world.queryAABB(new QueryCallback {
              def reportFixture(fixture:Fixture):Boolean = {
                val body = fixture.getBody
                val mouseJointDef = new MouseJointDef
                mouseJointDef.bodyA = Physics.ground
                mouseJointDef.bodyB = body
                mouseJointDef.target.set(box2dMousePos)
                mouseJointDef.maxForce = 1000f * body.getMass
                mouseJoint = Some(Physics.world.createJoint(mouseJointDef).asInstanceOf[MouseJoint])
                body.setAwake(true)
                
                return false // cancel iteration
              }
            }, toleranceArea)
            if( !mouseJoint.isDefined ) {
              dragging = true
            }
          case (0 , false) => // left up
            if( mouseJoint.isDefined ) {
              Physics.world.destroyJoint(mouseJoint.get)
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
        if( Keyboard.getEventKeyState ) {
          if( key == Keyboard.KEY_E) Physics.population.evolution()
          if( key == Keyboard.KEY_R) Physics.population.creatures.foreach(_.reset())
        }
      }
    }

    
    var i = 0
    while(running) {
      processEvents()
      
      for( _ <- 0 until subSteps ) {
        Physics.world.step(box2dStep, 10, 10)
        Physics.update()
        
        i += 1
        if( i % 3000 == 0 ) Physics.population.evolution()
      }

      glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT)
      glLoadIdentity()
      glScalef(1 / r, 1 / t, 1f)
      glTranslatef(translation.x*zoom, translation.y*zoom, 0)
      Physics.world.drawDebugData()

      Display.update()
      Display.sync(fps)
      updateFps()
    }

    Display.destroy()
  }
}
