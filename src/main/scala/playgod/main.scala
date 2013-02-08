package playgod

import swing._
import org.lwjgl.opengl.Display
import org.lwjgl.opengl.GL11._
import javax.swing.JPanel
import java.awt.BorderLayout
import java.awt.Canvas

import org.lwjgl.opengl.Display
import org.lwjgl.opengl.DisplayMode
import org.jbox2d.dynamics._
import org.jbox2d.common._
import org.lwjgl.input.Keyboard._
import org.jbox2d.callbacks.DebugDraw
import org.jbox2d.collision.shapes.PolygonShape
import org.lwjgl.BufferUtils

class Box(world:World,
          pos:Vec2,
          hx:Float = 1f,
          hy:Float = 1f,
          density:Float = 1f,
          friction:Float = 0.3f,
          center:Vec2 = new Vec2(0,0),
          angle:Float = 0f) {
  val bd = new BodyDef
  if( density != 0f ) bd.`type` = BodyType.DYNAMIC
  bd.active = true
  bd.position.set(pos)
  val body = world.createBody(bd)
  val dynamicBox = new PolygonShape
  dynamicBox.setAsBox(hx,hy, center, angle)
  val fixtureDef = new FixtureDef
  fixtureDef.shape = dynamicBox
  fixtureDef.density = density
  fixtureDef.friction = friction
  body.createFixture(fixtureDef)
}

object Main {


  def main(args: Array[String]) {

    val world = new World(new Vec2(0,-9.81f), false)
    world.setDebugDraw(DebugDrawer)

    val ground = new Box(world, new Vec2(0,-10), hx = 50, hy = 3, density = 0f)
    val box = new Box(world, new Vec2(0,4), hx = 1f, hy = 1f)
    val box2 = new Box(world, new Vec2(1.5f,10), hx = 1f, hy = 1f)



    Display.setDisplayMode(new DisplayMode(800, 600))
    Display.create()

    glClearColor(0.3f, 0.3f, 0.3f, 1f)

    glMatrixMode(GL_PROJECTION_MATRIX)
    glLoadIdentity()

    glMatrixMode(GL_MODELVIEW_MATRIX)
    glLoadIdentity()
    val r = 400/16f
    val t = 300/16f
    val n = -1f
    val f = 1f
    glScalef(1/r,1/t,1f)

    var ii = 0
    val array = new Array[Long](64)

    while (!Display.isCloseRequested() && !isKeyDown(KEY_ESCAPE)) {
      glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT)

      world.step(1/60.0f,10,10)
      world.drawDebugData()

      Display.update()



      array(ii) = System.currentTimeMillis()
      ii = (ii + 1) & 63
      val time_s = 1/((array((ii - 1) & 63) - array(ii)) / 64000f)
      Display.setTitle("%8.3ffps" format time_s)
    }

    Display.destroy()
  }
}

object DebugDrawer extends DebugDraw(new OBBViewportTransform) {

  appendFlags(DebugDraw.e_shapeBit | DebugDraw.e_centerOfMassBit | DebugDraw.e_jointBit)


  def drawPoint(argPoint: Vec2, argRadiusOnScreen: Float, argColor: Color3f) {}

  def drawSolidPolygon(vertices: Array[Vec2], vertexCount: Int, color: Color3f) {
    glColor3f(color.x, color.y, color.z)
    glBegin(GL_LINE_LOOP)
    for( i <- 0 until vertexCount ) {
      val v = vertices(i)
      glVertex2f(v.x, v.y)
    }
    glEnd()
  }

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
    glRotatef(xf.getAngle.toDegrees, 0, 0, 1)

    glBegin(GL_LINES)
    glColor3f(1, 0, 0)
    glVertex2f(0, 0)
    glVertex2f(1, 0)
    glColor3f(0, 1, 0)
    glVertex2f(0, 0)
    glVertex2f(0, 1)
    glEnd()

    glPopMatrix()
  }

  def drawString(x: Float, y: Float, s: String, color: Color3f) {}
}
