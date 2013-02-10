package playgod

import org.lwjgl.opengl.GL11._

import org.lwjgl.opengl.Display
import org.lwjgl.opengl.DisplayMode
import org.lwjgl.input.Keyboard._
import org.jbox2d.common._

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
        contents += new Button("Random Angles") {
          action = new Action("Random Angles") {
            override def apply() {
              for( bone <- Physics.sample.jointBones )
                bone.angleTarget = (util.Random.nextFloat()*2f-1f) * math.Pi.toFloat * 0.5f
           }
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
  def start() {
    import Box2DTools._

    glClearColor(0.3f, 0.3f, 0.3f, 1f)

    glMatrixMode(GL_PROJECTION_MATRIX)
    glLoadIdentity()

    glMatrixMode(GL_MODELVIEW_MATRIX)
    glLoadIdentity()
    val r = 400 / 16f
    val t = 300 / 16f
    val n = -1f
    val f = 1f
    glScalef(1 / r, 1 / t, 1f)
    
    
    val fps = 60
    
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
    
    while(running) {
      glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT)

      Physics.update()
      Physics.world.step(1f / fps, 10, 10)
      Physics.world.drawDebugData()

      Display.update()
      Display.sync(fps)
      updateFps()
    }

    Display.destroy()
  }
}
