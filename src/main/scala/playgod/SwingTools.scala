package playgod

import org.lwjgl.opengl.Display
import org.lwjgl.opengl.DisplayMode
import org.lwjgl.opengl.GL11
import swing._
import event._
import java.awt.Dimension

class LWJGLComponent(dimension:Dimension) extends Component {
  import javax.swing.JPanel
  import java.awt.Canvas
  override lazy val peer = new JPanel
  val canvas = new Canvas()
  canvas.setPreferredSize(dimension)
  peer.add(canvas)
  
  override def preferredSize_=(dim:Dimension) {
    canvas.setPreferredSize(dim)
    super.preferredSize = dim
    //GL11.glViewport(0,0,dim.width, dim.height)
  }
}

class SettingTextField[T](init:T, update:String => Unit) extends TextField {
  maximumSize = new Dimension(50,50)
  text = init.toString
  reactions += {
    case e:EditDone =>
      update(text)
  }
}

class ActionButton(title:String, action: => Unit) extends Button(title) {
  reactions += {
    case e:ButtonClicked => action
  }
}

trait Stretch extends Component { maximumSize = new Dimension(Int.MaxValue, Int.MaxValue) }
trait StretchX extends Component { maximumSize = new Dimension(Int.MaxValue, maximumSize.height) }
trait StretchY extends Component { maximumSize = new Dimension(maximumSize.width, Int.MaxValue) }

