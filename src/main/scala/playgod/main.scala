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

object Main {


  def main(args: Array[String]) {
    Display.setDisplayMode(new DisplayMode(800, 600))
    Display.create()

    // init OpenGL here
    glClearColor(0.3f, 0.3f, 0.3f, 1f)

    val world = new World(new Vec2(0,-1), false)

    val groundBodyDef = new BodyDef
    groundBodyDef.position.set(0, -10)
    val groundBody = world.createBody(groundBodyDef)
    val groundBox = new PolygonShape
    groundBox.setAsBox(50,10)
    groundBody.createFixture(groundBox, 0)

    val bd = new BodyDef
    bd.`type` = BodyType.DYNAMIC
    bd.active = true
    bd.position.set(0,4)
    val body = world.createBody(bd)
    val dynamicBox = new PolygonShape
    dynamicBox.setAsBox(1,1)
    val fixtureDef = new FixtureDef
    fixtureDef.shape = dynamicBox
    fixtureDef.density = 1f
    fixtureDef.friction = 0.3f
    body.createFixture(fixtureDef)


    val debugDrawer = new DebugDrawer(new OBBViewportTransform)
    debugDrawer.appendFlags(DebugDraw.e_shapeBit | DebugDraw.e_centerOfMassBit | DebugDraw.e_jointBit)
    world.setDebugDraw(debugDrawer)


    glMatrixMode(GL_PROJECTION_MATRIX)
    glLoadIdentity()
    //glOrtho(-400f/16,400f/16,-300f/16,300f/16,-1f,1f)


    val matrix = BufferUtils.createFloatBuffer(16)
    val r = 400/16f
    val t = 300/16f
    val n = -1f
    val f = 1f
    matrix.put(Array[Float](1/r, 0, 0, 0,   0, 1/t, 0, 0,   0, 0, -2/(f-n), 0,   0, 0, -(f+n)/(f-n),1))
    matrix.flip()
    glLoadMatrix(matrix)


    glGetFloat(GL_PROJECTION_MATRIX,matrix)
    for( y <- 0 until 16 ) {
      if( y % 4 == 0 ) println()
      print(" " + matrix.get(y))
    }



    glMatrixMode(GL_MODELVIEW_MATRIX)

    while (!Display.isCloseRequested() && !isKeyDown(KEY_ESCAPE)) {
      glLoadIdentity()
      glScalef(1/r,1/t,1f)
      glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT)
      // render OpenGL here





      world.step(1/60.0f,10,10)
      world.drawDebugData()

      Display.update()
    }

    Display.destroy()
  }
}

class DebugDrawer(viewport: IViewportTransform) extends DebugDraw(viewport) {
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
    glRotatef(xf.getAngle, 0, 0, 1)

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
