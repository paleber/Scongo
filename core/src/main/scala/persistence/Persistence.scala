package persistence

import model.element.{LevelId, Plan}

trait Persistence  {

  @throws[Exception]
  def loadMetaInfo: Map[String, List[String]]

  @throws[Exception]
  def loadPlan(levelId: LevelId): Plan

  @throws[Exception]
  def savePlan(levelId: LevelId, plan: Plan): Unit

}
