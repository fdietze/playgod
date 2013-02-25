package playgod

import org.jbox2d.dynamics.World
import org.jbox2d.common.Vec2
import playgod.Box2DTools._
import collection.mutable

class CreatureSimulation(
    val creatureDef:CreatureDefinition,
    val brainWeights:Option[Array[Double]] = None) extends Simulation {

  val world = new World(new Vec2(0, -9.81f), true)
  world.setDebugDraw(DebugDrawer)

  val ground = createBox(world, new Vec2(0, -3), hx = 500, hy = 3, density = 0f)
  val creature = creatureDef.create(world, brainWeights)

  override def update() {
    creature.update()
  }

  def reset() {
    creature.reset()
  }
}
