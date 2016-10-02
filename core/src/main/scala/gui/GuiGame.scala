package gui

import java.awt.event.{KeyAdapter, KeyEvent, MouseAdapter, MouseEvent}
import java.awt.{BasicStroke, Color, Graphics, Graphics2D, Polygon}
import javax.swing.JPanel
import java.awt.{Point => AwtPoint}

import akka.actor.{Actor, ActorLogging}
import engine.{Grid, Point}
import model.{Block, Level}
import msg.{ClientMessage, ServerMessage}

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.language.postfixOps


case class GuiGame(level: Level) extends JPanel with Actor with ActorLogging {
  log.debug("Initializing")

  private val blocks = level.blocks.toArray
  private val blockPolys = Array.fill[Polygon](blocks.length)(new Polygon())

  private val boardPoly = new Polygon()

  private var scaleFactor: Double = 1
  private var xOffset, yOffset: Double = 0
  private var lastX, lastY: Int = 0


  private class Selected(var index: Int, var block: Block) {
    val poly = new Polygon()
  }

  private var selected: Option[Selected] = None


  private case class MoveBlock(p: AwtPoint)

  private case class SelectBlock(p: AwtPoint)

  private case object ReleaseBlock


  private sealed trait Action

  private case object RotateRight extends Action

  private case object RotateLeft extends Action

  private case object MirrorVertical extends Action

  private case object MirrorHorizontal extends Action

  private case object BackToMenu

  addMouseMotionListener(new MouseAdapter {
    override def mouseDragged(e: MouseEvent) = self ! MoveBlock(e.getPoint)
  })

  addMouseListener(new MouseAdapter {
    override def mousePressed(e: MouseEvent) = self ! SelectBlock(e.getPoint)

    override def mouseReleased(e: MouseEvent) = self ! ReleaseBlock
  })

  addKeyListener(new KeyAdapter {
    override def keyPressed(e: KeyEvent) = e.getKeyCode match {
      case KeyEvent.VK_A => self ! RotateLeft
      case KeyEvent.VK_LEFT => self ! RotateLeft

      case KeyEvent.VK_D => self ! RotateRight
      case KeyEvent.VK_RIGHT => self ! RotateRight

      case KeyEvent.VK_W => self ! MirrorVertical
      case KeyEvent.VK_UP => self ! MirrorVertical

      case KeyEvent.VK_S => self ! MirrorHorizontal
      case KeyEvent.VK_DOWN => self ! MirrorHorizontal

      case KeyEvent.VK_ESCAPE => self ! BackToMenu

      case msg => log.warning(s"Ignoring Key: ${e.getKeyCode} - ${e.getKeyChar}")
    }
  })

  context.parent ! Gui.SetContentPane(this)


  private def convertCornersToPoly(points: List[Point], position: Point, poly: Polygon): Unit = {
    poly.reset()
    for (i <- points.indices) {
      poly.addPoint(
        scaleX(points(i).x + position.x),
        scaleY(points(i).y + position.y)
      )
    }
  }

  override def paint(g: Graphics): Unit = {

    // Calculate scaleFactor and offsets
    scaleFactor = Math.min(getWidth / level.width, getHeight / level.height)
    xOffset = (getWidth - level.width * scaleFactor) / 2
    yOffset = (getHeight - level.height * scaleFactor) / 2

    // Convert corners to polygon
    convertCornersToPoly(level.board.corners, Point.ORIGIN, boardPoly)
    for (i <- blocks.indices) {
      convertCornersToPoly(blocks(i).grid.corners, blocks(i).position, blockPolys(i))
    }

    // Draw the Background
    g.setColor(Color.GRAY)
    g.fillRect(0, 0, getWidth, getHeight)

    g.setColor(Color.WHITE)
    g.fillRect(scaleX(0), scaleY(0), scale(level.width), scale(level.height))

    g.setColor(Color.GRAY)
    for (y <- 1 until level.height.toInt) {
      g.drawLine(0, scaleY(y), getWidth, scaleY(y))
    }
    for (x <- 1 until level.width.toInt) {
      g.drawLine(scaleX(x), 0, scaleX(x), getHeight)
    }

    val g2 = g.asInstanceOf[Graphics2D]
    g2.setStroke(new BasicStroke((0.05 * scaleFactor).toInt))


    // Draw the board
    g.setColor(Color.LIGHT_GRAY)
    g.fillPolygon(boardPoly)

    g.setColor(Color.GRAY)
    g.drawPolygon(boardPoly)

    for (line <- level.board.lines) {
      g.drawLine(
        scaleX(line.start.x),
        scaleY(line.start.y),
        scaleX(line.end.x),
        scaleY(line.end.y))
    }

    // Draw unselected the blocks
    for (poly <- blockPolys if selected.isEmpty ||
      blockPolys.indexOf(poly) != selected.get.index) {

      g.setColor(new Color(100, 255, 255))
      g.fillPolygon(poly)

      g.setColor(new Color(0, 139, 139))
      g.drawPolygon(poly)
    }

    if (selected.isDefined) {
      convertCornersToPoly(
        selected.get.block.grid.corners,
        selected.get.block.position,
        selected.get.poly
      )

      g.setColor(new Color(100, 255, 100))
      g.fillPolygon(selected.get.poly)

      g.setColor(new Color(0, 139, 0))
      g.drawPolygon(selected.get.poly)
    }

  }

  private def scale(z: Double): Int = {
    (z * scaleFactor).toInt
  }

  private def scaleX(z: Double): Int = {
    (z * scaleFactor + xOffset).toInt
  }

  private def scaleY(z: Double): Int = {
    (z * scaleFactor + yOffset).toInt
  }


  override def receive = {

    case ServerMessage.UpdateBlock(index, block) =>
      blocks(index) = block

    case MoveBlock(position) =>
      if (selected.isDefined) {
        val delta = Point((position.x - lastX) / scaleFactor, (position.y - lastY) / scaleFactor)
        selected.get.block = selected.get.block.copy(
          position = selected.get.block.position + delta
        )
        lastX = position.x
        lastY = position.y
      }

    case SelectBlock(position) =>
      selected = None
      for (poly <- blockPolys) {
        if (poly.contains(position)) {
          lastX = position.x
          lastY = position.y
          val index = blockPolys.indexOf(poly)
          selected = Some(new Selected(index, blocks(index)))
        }
      }

    case ReleaseBlock =>
      if (selected.isDefined) {
        val msg = ClientMessage.UpdateBlockPosition(
          selected.get.index,
          selected.get.block.position
        )
        blocks(selected.get.index) = selected.get.block
        selected = None
        activeAction = None
        context.parent ! msg
      }

    case RotateLeft =>
      if (selected.isDefined && activeAction.isEmpty) {
        context.parent ! ClientMessage.RotateBlockLeft(selected.get.index)
        activeAction = Some(BlockAction(RotateLeft, selected.get.block.grid))
        self ! HandleBlockAction
      }

    case RotateRight =>
      if (selected.isDefined && activeAction.isEmpty) {
        context.parent ! ClientMessage.RotateBlockRight(selected.get.index)
        activeAction = Some(BlockAction(RotateRight, selected.get.block.grid))
        self ! HandleBlockAction
      }

    case MirrorVertical =>
      if (selected.isDefined && activeAction.isEmpty) {
        context.parent ! ClientMessage.MirrorBlockVertical(selected.get.index)
        activeAction = Some(BlockAction(MirrorVertical, selected.get.block.grid))
        self ! HandleBlockAction
      }

    case MirrorHorizontal =>
      if (selected.isDefined && activeAction.isEmpty) {
        context.parent ! ClientMessage.MirrorBlockHorizontal(selected.get.index)
        activeAction = Some(BlockAction(MirrorHorizontal, selected.get.block.grid))
        self ! HandleBlockAction
      }


    case BackToMenu =>
      context.parent ! ClientMessage.ShowMenu

    case HandleBlockAction =>
      handleBlockAction()

    case msg => log.warning("Unhandled message: " + msg)
  }

  private def handleBlockAction(): Unit = {
    if (activeAction.isDefined) {

      activeAction.get.action match {

        case RotateLeft =>
          selected.get.block = selected.get.block.copy(
            grid = activeAction.get.startGrid.rotate(
              -Math.PI / 2 / activeAction.get.maxSteps * activeAction.get.curStep
            )
          )

        case RotateRight =>
          selected.get.block = selected.get.block.copy(
            grid = activeAction.get.startGrid.rotate(
              Math.PI / 2 / activeAction.get.maxSteps * activeAction.get.curStep
            )
          )

        case MirrorVertical =>
          selected.get.block = selected.get.block.copy(
            grid = activeAction.get.startGrid.mirrorVertical(
              activeAction.get.curStep.toFloat / activeAction.get.maxSteps
            )
          )

        case MirrorHorizontal =>
          selected.get.block = selected.get.block.copy(
            grid = activeAction.get.startGrid.mirrorHorizontal(
              activeAction.get.curStep.toFloat / activeAction.get.maxSteps
            )
          )

        case _ => log.error("Unknown Block Action")
      }

      if (activeAction.get.curStep < activeAction.get.maxSteps) {
        activeAction = Some(activeAction.get.copy(
          curStep = activeAction.get.curStep + 1)
        )
        context.system.scheduler.scheduleOnce(20 millis) {
          self ! HandleBlockAction
        }
      } else {
        activeAction = None
      }
    }
  }


  private var activeAction: Option[BlockAction] = None

  private case class BlockAction(action: Action, startGrid: Grid, curStep: Int = 0, maxSteps: Int = 6)

  private case object HandleBlockAction

}
