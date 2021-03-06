package persistence

import java.util.concurrent.TimeUnit

import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.concurrent.ExecutionContext.Implicits.global

import akka.actor.{Actor, ActorLogging}
import builder.LevelBuilder
import model.element.{Level, LevelKey}
import model.msg.{ClientMsg, ServerMsg}
import persistence.ResourceManager._
import scaldi.{Injectable, Injector}

object ResourceManager {

  case object LoadMenu extends ClientMsg

  case class MenuLoaded(info: Map[String, Set[String]]) extends ServerMsg

  case class LoadLevel(id: LevelKey) extends ClientMsg

  case class LevelLoaded(level: Level) extends ServerMsg

  case object LoadingLevelFailed extends ServerMsg

}

class ResourceManager(implicit inj: Injector) extends Actor with ActorLogging with Injectable {
  log.debug("Initializing")

  private val persistence: Persistence = inject[Persistence]

  private def state(info: Map[String, Set[String]], levels: Map[LevelKey, Level]): Receive = {

    case LoadMenu =>
      log.debug("Loading menu")
      sender ! MenuLoaded(info)

    case LoadLevel(id) =>
      levels.get(id).fold[Unit] {

        val target = sender
        persistence.readPlan(id).map { plan =>
          val level = LevelBuilder.build(id, plan)
          self ! LevelLoaded(level)
          target ! LevelLoaded(level)
        }.recover { case _ =>
          target ! LoadingLevelFailed
        }

      } { level =>
        log.debug("Loading level from cache: " + id)
        sender ! LevelLoaded(level)
      }

    case LevelLoaded(level) =>
      context.become(state(info, levels.updated(level.id, level)))

  }

  override def receive: Receive = state(convertIdList(Await.result(persistence.readAllKeys(), Duration(5, TimeUnit.SECONDS))), Map.empty)

  private def convertIdList(ids: Set[LevelKey]): Map[String, Set[String]] = {
    ids.map(_.category).map(cat => (cat, ids.filter(_.category == cat).map(_.name))).toMap
  }

}
