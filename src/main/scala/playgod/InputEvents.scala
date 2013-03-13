package playgod

import org.jbox2d.dynamics.joints.MouseJoint
import org.lwjgl.input.{Keyboard, Mouse}
import org.lwjgl.input.Mouse._

object InputEvents {
  import Main.renderArea.{translation,zoom,mousePos}
  import Simulation.{arrowDirection,autoArrowDirections}

  var mouseJoint:Option[MouseJoint] = None
  var dragging = false

  def processEvents() {
    import Mouse._
    while( Mouse.next ) {
      ( getEventButton, getEventButtonState ) match {
        case (0 , true) => // left down
          /*for( organism <- population.organisms.map(_.asInstanceOf[Box2DSimulationOrganism]) ) {
            val world = organism.world

            val tolerance = new Vec2(0.01f, 0.01f)
            val toleranceArea = new AABB(box2dMousePos.sub(tolerance), box2dMousePos.add(tolerance))
              world.queryAABB(new QueryCallback {
                def reportFixture(fixture:Fixture):Boolean = {
                  if( fixture.getDensity == 0f ) return true

                  val body = fixture.getBody
                  val mouseJointDef = new MouseJointDef
                  mouseJointDef.bodyA = body
                  mouseJointDef.bodyB = body
                  mouseJointDef.target.set(box2dMousePos)
                  mouseJointDef.maxForce = 1000f * body.getMass
                  mouseJoint = Some(world.createJoint(mouseJointDef).asInstanceOf[MouseJoint])
                  body.setAwake(true)

                  return false // cancel iteration
                }
              }, toleranceArea)
          }*/
          if( !mouseJoint.isDefined ) {
            dragging = true
          }
        case (0 , false) => // left up
          /*if( mouseJoint.isDefined ) {
            for( organism <- population.organisms.map(_.asInstanceOf[Box2DSimulationOrganism]) ) {
              val world = organism.world
              world.destroyJoint(mouseJoint.get)
            }
            mouseJoint = None
          }*/
          dragging = false
        case (1 , true) => // right down
        case (1 , false) => // right up
        case (-1, false) => // wheel
          val delta = Mouse.getDWheel
          if( delta > 0 )
            zoom /= 1.1f
          else if( delta < 0 )
            zoom *= 1.1f
        case _ =>
      }
    }

    if( mouseJoint.isDefined ) {
      mouseJoint.get.setTarget(mousePos)
    }
    if( dragging ) {
      translation.x += getDX
      translation.y += getDY
    }

    while(Keyboard.next) {
      val key = Keyboard.getEventKey
      if( Keyboard.getEventKeyState ) { // Key down
        //if( key == Keyboard.KEY_R) population.reset()
        if( key == Keyboard.KEY_LEFT) arrowDirection = -1
        if( key == Keyboard.KEY_RIGHT) arrowDirection = 1
        if( key == Keyboard.KEY_A) autoArrowDirections = !autoArrowDirections
      } else { // Key up
        if( key == Keyboard.KEY_LEFT) arrowDirection = 0
        if( key == Keyboard.KEY_RIGHT) arrowDirection = 0
      }
      println("arrowDirection: " + arrowDirection)
    }
  }
}
