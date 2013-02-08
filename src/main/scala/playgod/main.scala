package playgod

import swing._
import org.lwjgl.opengl.Display
import org.lwjgl.opengl.GL11._
import javax.swing.JPanel
import java.awt.BorderLayout
import java.awt.Canvas

import org.lwjgl.opengl.Display
import org.lwjgl.opengl.DisplayMode
import org.jbox2d.dynamics.{BodyType, BodyDef, World, Body}
import org.jbox2d.common.Vec2

object Main {


  def main(args: Array[String]) {
    Display.setDisplayMode(new DisplayMode(800, 600))
    Display.create()

    // init OpenGL here
    glClearColor(0.3f, 0.3f, 0.3f, 1f)

    val world = new World(new Vec2(0,-1), false)
    val bd = new BodyDef
    bd.`type` = BodyType.DYNAMIC
    bd.active = true
    bd.position.set(0,0)

    val body = world.createBody(bd)



    while (!Display.isCloseRequested()) {
      glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT)
      // render OpenGL here

      world.step(1/60.0f,10,10)

      println( body.getPosition.y )
      Display.update()
    }

    Display.destroy()
  }
}
