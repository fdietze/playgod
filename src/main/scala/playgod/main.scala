package playgod

import org.lwjgl.opengl.GL11._
import org.lwjgl.opengl.Display


import swing._
import event._
import org.jbox2d.common.Vec2
import org.lwjgl.input.Mouse
import concurrent.Future
import concurrent.ExecutionContext.Implicits.global


object Main extends SimpleSwingApplication {
  val width = 800
  val height = 600

  val renderArea = new LWJGLComponent(new Dimension(width, height)) with Stretch {
    val fps = 60
    var zoom = 1f/100f
    var translation = new Vec2(0,-200)
    def componentMousePos = new Vec2(Mouse.getX, Mouse.getY)
    def mousePos = componentMousePos.sub(translation).mul(zoom).add(new Vec2(-r, -t))
    var autoCamera = true

    def r = Main.width * 0.5f * zoom
    def t = Main.height * 0.5f * zoom
    val n = -1f
    val f = 1f

    def init() {
      Display.setParent(canvas)
      Display.setVSyncEnabled(false)
      Display.create()

      glClearColor(0.3f, 0.3f, 0.3f, 1f)
      glMatrixMode(GL_PROJECTION_MATRIX)
      glLoadIdentity()
      glMatrixMode(GL_MODELVIEW_MATRIX)
    }
    
    reactions += {
      case e:UIElementResized =>
        println(preferredSize)
        println(maximumSize)
        println(size)
        preferredSize = size
    }

  }

  //def creature = Simulation.creature
  //def population = Simulation.population

  val top = new MainFrame {
    val autoCameraCheckBox = new CheckBox("Auto Camera")
    /*val backgroundSimulationCheckBox = new CheckBox("Background Simulation") {
      selected = true
      reactions += {
        case e:ButtonClicked =>
          if( selected ) NeatSimulation ! Stop
          else           NeatSimulation ! Start
      }
    }*/

    val panel = new BoxPanel(Orientation.Vertical) {
      contents += new BoxPanel(Orientation.Horizontal) {
        contents += autoCameraCheckBox
        //contents += backgroundSimulationCheckBox
        contents += new ActionButton("Reset", LiveSimulation ! Reset)
      }

      
      contents += new BoxPanel(Orientation.Horizontal) {
        contents += renderArea
      }

      listenTo(keys)
      reactions += {
        case KeyPressed(_, Key.Escape, _, _) =>
          quit()
        case KeyPressed(_, Key.R, _, _) =>
          LiveSimulation ! Reset
      }
    }
    contents = panel
    panel.requestFocus() // be able to listen to key events
  }
  
  renderArea.listenTo(top)

  override def main(args:Array[String]) {
    Future { NeatSimulation.start() }
    super.main(args)
    renderArea.init()
    LiveSimulation.start()
  }

  override def quit() = {
    LiveSimulation.stop()
    //Display.destroy()
    super.quit()
  }
}
