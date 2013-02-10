package playgod

import org.jbox2d.dynamics._
import org.jbox2d.dynamics.joints._
import org.jbox2d.common._
import Box2DTools._

object Physics {
  val world = new World(new Vec2(0, -9.81f), true)
  world.setDebugDraw(DebugDrawer)
  
  createBox(world, new Vec2(0, -10), hx = 50, hy = 3, density = 0f)

  val sample = Skeleton.forky
  
  def update() {
    sample.update()
  }
}
