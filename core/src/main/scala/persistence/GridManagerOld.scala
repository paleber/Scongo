package persistence


private[persistence] object GridManagerOld {

  /*
  private implicit val formats = Serialization.formats(NoTypeHints)

  private[persistence] case class MapItem(plan: GridPlan,
                                          grid: Option[AnchoredGrid] = None)

  private[persistence] val gridMap = {
    val dirGrids = new File("core/src/main/resources/blocks")
    val map = mutable.Map.empty[String, MapItem]
    for (file <- dirGrids.listFiles()) {
      val source = Source.fromFile(file).mkString
      val plan = read[GridPlan](source)
      val name = file.getName.replace(".json", "")
      map += ((name, MapItem(plan)))
    }
    map
  }

  private[persistence] def load(name: String): AnchoredGrid = {
    assert(gridMap.contains(name))
    val item = gridMap(name)
    if (item.grid.isDefined) {
      return item.grid.get
    }
    val grid = GridBuilder.build(item.plan)
    gridMap.update(name, item.copy(grid = Some(grid)))
    grid
  }*/

}