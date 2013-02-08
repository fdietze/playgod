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
import org.jbox2d.common.{IViewportTransform, Transform, Color3f, Vec2}
import org.lwjgl.input.Keyboard._
import org.jbox2d.callbacks.DebugDraw

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
    val debugDrawer = new DebugDrawer(null)
    debugDrawer.appendFlags(-1)
    world.setDebugDraw(debugDrawer)

    val body = world.createBody(bd)

    glMatrixMode(GL_PROJECTION_MATRIX)
    glOrtho(-400/16,400/16,-300/16,300/16,-1,1)
    glMatrixMode(GL_MODELVIEW_MATRIX)

    while (!Display.isCloseRequested() && !isKeyDown(KEY_ESCAPE)) {
      glLoadIdentity()
      glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT)
      // render OpenGL here


      world.step(1/60.0f,10,10)
      world.drawDebugData()

      println( body.getPosition.y )
      Display.update()
    }

    Display.destroy()
  }
}

class DebugDrawer(viewport: IViewportTransform) extends DebugDraw(viewport) {
  def drawPoint(argPoint: Vec2, argRadiusOnScreen: Float, argColor: Color3f) {}

  def drawSolidPolygon(vertices: Array[Vec2], vertexCount: Int, color: Color3f) {}

  def drawCircle(center: Vec2, radius: Float, color: Color3f) {}

  def drawSolidCircle(center: Vec2, radius: Float, axis: Vec2, color: Color3f) {}

  def drawSegment(p1: Vec2, p2: Vec2, color: Color3f) {
    glBegin(GL_LINES)
    glColor3f(color.x, color.y, color.z)
    glVertex2f(p1.x, p1.y)
    glVertex2f(p2.x, p2.y)
    glEnd()
  }

  def drawTransform(xf: Transform) {
    glPushMatrix()
    glTranslatef(xf.position.x, xf.position.y, 0)
    glRotatef(xf.getAngle, 0, 0, 1)

    glBegin(GL_LINES)
    glColor3f(1, 0, 0)
    glVertex2f(0, 0)
    glVertex2f(1, 0)
    glColor3f(0, 1, 0)
    glVertex2f(0, 0)
    glVertex2f(0, 1)
    glEnd()
  }

  def drawString(x: Float, y: Float, s: String, color: Color3f) {}
}
