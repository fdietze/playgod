package playgod

import org.lwjgl.opengl.GL11._

import org.lwjgl.opengl.Display
import org.lwjgl.opengl.DisplayMode
import org.jbox2d.dynamics._
import org.jbox2d.dynamics.joints._
import org.jbox2d.common._
import org.lwjgl.input.Keyboard._

import swing._
import event._
import playgod.Box2DTools._

object Main extends SimpleSwingApplication {

  val renderArea = new LWJGLComponent(new Dimension(800,600))
  val top = new swing.MainFrame {
    val panel = new BoxPanel(swing.Orientation.Vertical) {
      contents += new Button("Button") {
        action = new Action("click") {
          override def apply() {
            createBox(world, new Vec2(util.Random.nextGaussian().toFloat, 10), hx = 1f, hy = 1f)
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

  val world = new World(new Vec2(0, -9.81f), true)
  world.setDebugDraw(DebugDrawer)


  var running = true
  def start() {
    import Box2DTools._

    createBox(world, new Vec2(0, -10), hx = 50, hy = 3, density = 0f)

    val boxA = createBox(world, new Vec2(0, 4), hx = 1f, hy = 1f)
    val boxB = createBox(world, new Vec2(1.5f, 10), hx = 1f, hy = 1f)
    
    val jointDef = new DistanceJointDef
    
    
    jointDef.initialize(boxA, boxB, boxA.getPosition, boxB.getPosition);
    jointDef.collideConnected = true;
    
    val joint = world.createJoint(jointDef)




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

      world.step(1 / 60.0f, 10, 10)
      world.drawDebugData()

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
