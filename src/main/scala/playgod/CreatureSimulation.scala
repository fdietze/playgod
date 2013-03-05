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

  val ground = createBox(world, pos = new Vec2(0, -3), width = 1000, height = 2, density = 0f, friction = 1f)
  for( i <- 0 until Main.obstacleCount )
    createBox(world, pos = new Vec2(50 + i*10f, -3), width = 3f, height = 6f + i / 2.5f, density = 0f, friction = 1f)
  val creature = creatureDef.create(world, brainWeights)

  override def update() {
    creature.update()
  }

  def reset() {
    creature.reset()
  }
}
