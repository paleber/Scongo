package model.console

import model.basic.Point
import model.msg.ClientMsg


private[console] trait TextCommand {

  val description: String

  val numberArgs: Int

  @throws(classOf[NumberFormatException])
  def parse(args: Array[String]): ClientMsg

}

private[console] object CmdShutdown extends TextCommand {
  override val description = "- Shutdown the application"
  override val numberArgs = 0

  override def parse(args: Array[String]) = ClientMsg.Shutdown
}

private[console] object CmdShowGame extends TextCommand {
  override val description = "level:INT - Show the game"
  override val numberArgs = 1

  override def parse(args: Array[String]) = ClientMsg.ShowGame(args(1))
}

private[console] object CmdShowMenu extends TextCommand {
  override val description = "- Show the menu"
  override val numberArgs = 0

  override def parse(args: Array[String]) = ClientMsg.ShowMenu
}

private[console] object CmdRotateBlockLeft extends TextCommand {
  override val description = "index:INT - Rotate a block left"
  override val numberArgs = 1

  override def parse(args: Array[String]) = ClientMsg.RotateBlockLeft(args(1).toInt)
}

private[console] object CmdRotateBlockRight extends TextCommand {
  override val description = "index:INT - Rotate a block right"
  override val numberArgs = 1

  override def parse(args: Array[String]) = ClientMsg.RotateBlockRight(args(1).toInt)
}

private[console] object CmdMirrorBlockVertical extends TextCommand {
  override val description = "index:INT - Mirror a block vertical"
  override val numberArgs = 1

  override def parse(args: Array[String]) = ClientMsg.MirrorBlockVertical(args(1).toInt)
}

private[console] object CmdMirrorBlockHorizontal extends TextCommand {
  override val description = "index:INT - Mirror a block horizontal"
  override val numberArgs = 1

  override def parse(args: Array[String]) = ClientMsg.MirrorBlockHorizontal(args(1).toInt)
}

private[console] object CmdMoveBlock extends TextCommand {
  override val description = "index:INT x:DOUBLE y:DOUBLE - Mirror a block horizontal"
  override val numberArgs = 3

  override def parse(args: Array[String]) =
    ClientMsg.UpdateBlockPosition(args(1).toInt, Point(args(2).toDouble, args(3).toDouble))
}
