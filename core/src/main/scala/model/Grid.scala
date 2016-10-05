package model

case class Grid(rotationSteps: Int,
                anchors: List[Point],
                corners: List[Point],
                lines: List[Line]) {

  def +(p: Point): Grid = {
    copy(
      anchors = anchors.toArray.transform(a => a + p).toList,
      corners = corners.toArray.transform(c => c + p).toList,
      lines = lines.toArray.transform(l => l + p).toList
    )
  }

  def rotate(angle: Double, pivot: Point = Point.ORIGIN): Grid = {
    copy(
      anchors = anchors.toArray.transform(p => p.rotate(angle, pivot)).toList,
      corners = corners.toArray.transform(p => p.rotate(angle, pivot)).toList,
      lines = lines.toArray.transform(l => l.rotate(angle, pivot)).toList
    )
  }

  def mirrorVertical(percentage: Double = 1): Grid = {
    copy(
      anchors = anchors.toArray.transform(p => p.mirrorVertical(percentage)).toList,
      corners = corners.toArray.transform(p => p.mirrorVertical(percentage)).toList,
      lines = lines.toArray.transform(l => l.mirrorVertical(percentage)).toList
    )
  }

  def mirrorHorizontal(percentage: Double = 1): Grid = {
    copy(
      anchors = anchors.toArray.transform(p => p.mirrorHorizontal(percentage)).toList,
      corners = corners.toArray.transform(p => p.mirrorHorizontal(percentage)).toList,
      lines = lines.toArray.transform(l => l.mirrorHorizontal(percentage)).toList
    )
  }

}