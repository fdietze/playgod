package playgod

import org.lwjgl.opengl.GL11._
import org.lwjgl.opengl.Display


import swing._
import event._
import org.jbox2d.common.Vec2
import org.lwjgl.input.Mouse


object Main extends SimpleSwingApplication {
  val width = 800
  val height = 600

  val renderArea = new LWJGLComponent(new Dimension(width, height)) {
    val fps = 60
    var zoom = 1f/10f
    var translation = new Vec2(0,-200)
    def componentMousePos = new Vec2(Mouse.getX, Mouse.getY)
    def mousePos = componentMousePos.sub(translation).mul(zoom).add(new Vec2(-r, -t))

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
  }

  def creature = Simulation.creature
  //def population = Simulation.population

  val top = new swing.MainFrame {
    val drawBestCheckBox = new CheckBox("elite")

    val panel = new BoxPanel(Orientation.Vertical) {
      contents += new BoxPanel(Orientation.Horizontal) {
        contents += drawBestCheckBox
        contents += new Label("sub: ")
        contents += new SettingTextField(Simulation.subSteps, x => Simulation.subSteps = x.toInt)
        contents += new Label("age: ")
        contents += new SettingTextField(creature.maxSimulationSteps, x => creature.maxSimulationSteps = x.toInt)
        /*contents += new Label("popsize: ")
        contents += new SettingTextField(population.populationSize, x => population.populationSize = x.toInt)
        contents += new Label("cross %: ")
        contents += new SettingTextField(population.crossoverProbability, x => population.crossoverProbability = x.toDouble)
        contents += new Label("mut %: ")
        contents += new SettingTextField(population.mutationProbability, x => population.mutationProbability = x.toDouble)
        contents += new Label("mut: ")
        contents += new SettingTextField(population.mutationStrength, x => population.mutationStrength = x.toDouble)
        contents += new Label("elitism: ")
        contents += new SettingTextField(population.elitism, x => population.elitism = x.toDouble)*/
      }

      contents += renderArea
      
      listenTo(keys)
      reactions += {
        case KeyPressed(_, Key.Escape, _, _) =>
          quit()
      }

    }
    contents = panel
    panel.requestFocus() // be able to listen to key events
  }

  override def main(args:Array[String]) {
    super.main(args)
    renderArea.init()
    Simulation.start()
  }
}
