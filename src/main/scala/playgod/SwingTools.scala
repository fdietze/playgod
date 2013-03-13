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

class SettingTextField[T](init:T, update:String => Unit) extends TextField {
  maximumSize = new Dimension(50,50)
  text = init.toString
  listenTo(this)
  reactions += {
    case e:EditDone =>
      update(text)
  }
}
