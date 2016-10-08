package model.element

import model.basic.Point

case class Level(width: Double,
                 height: Double,
                 board: Grid,
                 blocks: List[Block],
                 freeAnchors: List[Point])



