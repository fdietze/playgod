package playgod

import collection.mutable
import org.lwjgl.opengl.GL11._

object Stats {
  val statsHeight = 0.2f
  val statsRange = Main.width
  def population = Main.population


  val bestScore = new mutable.ArrayBuffer[Double]
  val avgScore = new mutable.ArrayBuffer[Double]
  val worstScore = new mutable.ArrayBuffer[Double]

  def draw() {
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

    if( bestScore.size == 0 ) {glPopMatrix(); return}
    def bestData = bestScore.takeRight(statsRange)
    def avgData = avgScore.takeRight(statsRange)
    def worstData = worstScore.takeRight(statsRange)

    val min = worstData.min
    val max = bestData.max
    val range = max - min


    for( (data,color) <- List((worstData,new Color(0xFFB4B0)), (avgData,new Color(242,200,148)), (bestData,new Color(0xF5FFA3))) ) {
      glBegin(GL_LINE_STRIP)
      color.glColor()
      if( bestScore.size == 1 ) {
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

}
