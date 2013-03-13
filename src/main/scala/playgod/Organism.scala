package playgod

import concurrent.{Await, Promise}
import concurrent.duration.Duration
import org.jbox2d.dynamics.World
import org.jbox2d.common.Vec2
import playgod.Box2DTools.DebugDrawer


abstract class Organism {
  def genome:Genome
  private val fitnessPromise = Promise[Double]()
  def fitness:Double = Await.result(fitnessPromise.future, Duration.Inf)
  def fitness_=(value:Double) = fitnessPromise.success(value)
}

abstract class SimulationOrganism extends Organism {
  def reward = 0.0
  def penalty = 0.0
  def simulationStep()

  val maxSteps = 500
  var age = 0
  private var mScore = 0.0
  def score = mScore / age

  def step() {
    if ( age < maxSteps ) {
      simulationStep()
      age += 1
      mScore += reward - penalty

      if( age >= maxSteps )
        fitness = score
    }
  }

  def finish() {
    while( age < maxSteps )
      step()
  }
}

abstract class Box2DSimulationOrganism extends SimulationOrganism {
  val timeStep = 1f / 30f
  val world = new World(new Vec2(0, -9.81f), true)
  world.setDebugDraw(DebugDrawer)

  def simulationStep() { world.step(timeStep, 10, 10) }
  def debugDraw() { world.drawDebugData() }
}
