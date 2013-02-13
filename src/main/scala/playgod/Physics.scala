package playgod

import org.jbox2d.dynamics._
import org.jbox2d.dynamics.joints._
import org.jbox2d.common._
import Box2DTools._

import collection.mutable

object Physics {
  private var lastCollisionGroupIndex:Int = 1
  def nextCollisionGroupIndex:Int = {
    lastCollisionGroupIndex += 1
    return lastCollisionGroupIndex
  }
  
  val world = new World(new Vec2(0, -9.81f), true)
  world.setDebugDraw(DebugDrawer)
  
  val ground = createBox(world, new Vec2(0, -3), hx = 500, hy = 3, density = 0f)

  
  val population = new Population
  population.creatures = mutable.ArrayBuffer.tabulate(30){ i =>
    val creature = CreatureFactory.forky
    creature
  }
  
  def update() {
    population.creatures.foreach(_.update())
  }
}
