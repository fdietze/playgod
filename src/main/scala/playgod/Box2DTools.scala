package playgod

import org.jbox2d.dynamics._
import org.jbox2d.common._
import org.jbox2d.collision.shapes.PolygonShape
import org.jbox2d.callbacks.DebugDraw
import org.lwjgl.opengl.GL11._

object Box2DTools {

  object MathHelpers {
    type Vec2 = org.jbox2d.common.Vec2
    object Vec2 {
      @inline def apply(x:Float, y:Float) = new Vec2(x,y)
      @inline def apply(x:Float) = new Vec2(x,x)
    }

    implicit class RichVec2(v:Vec2) {
      def +(that:Vec2) = v.add(that)
      def -(that:Vec2) = v.sub(that)
      def *(that:Float) = v.mul(that)
      def :=(that:Vec2) = v.set(that.x, that.y)
    }
  }

  def createBox(world: World,
                pos: Vec2,
                hx: Float = 1f,
                hy: Float = 1f,
                density: Float = 1f,
                friction: Float = 0.6f,
                center: Vec2 = new Vec2(0, 0),
                angle: Float = 0f,
                collisionGroupIndex:Int = 0) : Body = {
    val bd = new BodyDef
    if (density != 0f) bd.`type` = BodyType.DYNAMIC
    bd.active = true
    bd.position.set(pos)

    val fixtureDef = new FixtureDef
    val dynamicBox = new PolygonShape
    dynamicBox.setAsBox(hx, hy, center, angle)
    fixtureDef.shape = dynamicBox
    fixtureDef.density = density
    fixtureDef.friction = friction
    fixtureDef.filter.groupIndex = collisionGroupIndex

    val body = world.createBody(bd)
    body.createFixture(fixtureDef)
    
    return body
  }

  object DebugDrawer extends DebugDraw(new OBBViewportTransform) {

    appendFlags(DebugDraw.e_shapeBit | DebugDraw.e_centerOfMassBit | DebugDraw.e_jointBit)


    def drawPoint(argPoint: Vec2, argRadiusOnScreen: Float, argColor: Color3f) {}

    def drawSolidPolygon(vertices: Array[Vec2], vertexCount: Int, color: Color3f) {
      glColor3f(color.x, color.y, color.z)
      glBegin(GL_LINE_LOOP)
      for (i <- 0 until vertexCount) {
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

}
