package playgod

import org.lwjgl.opengl.Display
import org.lwjgl.opengl.DisplayMode
import swing._
import event._

class LWJGLComponent(dimension:java.awt.Dimension) extends Component {
  import javax.swing.JPanel
  import java.awt.Canvas
  override lazy val peer = new JPanel
  val canvas = new Canvas()
  canvas.setPreferredSize(dimension)
  peer.add(canvas)
}
