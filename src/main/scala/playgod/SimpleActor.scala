package playgod

import collection.mutable

trait SimpleActor {
  protected val mailbox = new mutable.SynchronizedQueue[Any]
  def !(msg:Any) { mailbox enqueue msg }
  protected def receive:PartialFunction[Any,Unit]
  protected def processMessages() {
    while( mailbox.nonEmpty )
      receive(mailbox.dequeue())
  }
}
