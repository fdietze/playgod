package playgod

import org.jbox2d.dynamics._
import org.jbox2d.dynamics.joints._
import org.jbox2d.common._
import Box2DTools._

import collection.mutable

abstract class Simulation {
  def world:World
  def update() {}
}

abstract class ObjectDefinition {
  def bodyDef:BodyDef
  def fixtureDef:FixtureDef
  def create(world:World):Body = {
    val body = world.createBody(bodyDef)
    body.createFixture(fixtureDef)
    return body
  }
}

object Physics {
  val timeStep = 1f / 60f

  def step(simulations:Seq[Simulation]) { simulations.par.foreach(_.world.step(timeStep, 10, 10)) }
  def update(simulations:Seq[Simulation]) { simulations.par.foreach(_.update()) }
  def debugDraw(simulations:Seq[Simulation]) { simulations.foreach(_.world.drawDebugData()) }
}
