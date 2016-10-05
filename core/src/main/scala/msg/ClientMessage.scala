package msg

import akka.actor.ActorRef
import model.Point

sealed trait ClientMessage

object ClientMessage {

  case object Shutdown extends ClientMessage

  case object ShowMenu extends ClientMessage

  case class RegisterView(view: ActorRef) extends ClientMessage

  case class ShowGame(level: Int) extends ClientMessage

  case class RotateBlockLeft(index: Int) extends ClientMessage

  case class RotateBlockRight(index: Int) extends ClientMessage

  case class MirrorBlockVertical(index: Int) extends ClientMessage

  case class MirrorBlockHorizontal(index: Int) extends ClientMessage

  case class UpdateBlockPosition(index: Int, position: Point) extends ClientMessage

}
