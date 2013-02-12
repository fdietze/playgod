package playgod

import org.jbox2d.dynamics._
import org.jbox2d.dynamics.joints._
import org.jbox2d.common._
import Box2DTools._

import collection.mutable

object Physics {
  private var lastCollisionGroupIndex:Int = 0
  def nextCollisionGroupIndex:Int = {
    lastCollisionGroupIndex += 1
    return lastCollisionGroupIndex
  }
  
  val world = new World(new Vec2(0, -9.81f), true)
  world.setDebugDraw(DebugDrawer)
  
  val ground = createBox(world, new Vec2(0, -20), hx = 50, hy = 3, density = 0f)

  val creatures = Array.tabulate(30){ i =>
    val creature = Skeleton.forky
    creature.addPosition(new Vec2(util.Random.nextGaussian.toFloat*10f,i*6))
    creature
  }
  
  def update() {
    creatures.foreach(_.update())
  }
}
