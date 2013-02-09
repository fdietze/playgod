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
        val newAngleField = new TextField {
          maximumSize = new Dimension(50,30)
        }
        contents += newAngleField
        contents += new Button("Change Angle") {
          action = new Action("Change Angle") {
            override def apply() {
              Physics.angleTarget = newAngleField.text.toFloat.toRadians
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

    var ii = 0
    val array = new Array[Long](64)
    while(running) {
      glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT)

      Physics.update()
      Physics.world.step(1 / 60.0f, 10, 10)
      Physics.world.drawDebugData()

      array(ii) = System.currentTimeMillis()
      ii = (ii + 1) & 63
      val time_s = 1 / ((array((ii - 1) & 63) - array(ii)) / 64000f)
      top.title = "%8.3ffps" format time_s

      Display.update()
      Display.sync(60)
    }

    Display.destroy()
  }
}
