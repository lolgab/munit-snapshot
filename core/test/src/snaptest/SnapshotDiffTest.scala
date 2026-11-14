package snaptest

class SnapshotDiffTest extends munit.FunSuite:

  test("changed line") {
    assertNoDiff(
      SnapshotDiff("a\nb\nc", "a\nB\nc"),
      """| a
         |-b
         |+B
         | c""".stripMargin
    )
  }

  test("added and removed lines") {
    assertNoDiff(
      SnapshotDiff("a\nb", "b\nc"),
      """|-a
         | b
         |+c""".stripMargin
    )
  }

  test("long unchanged runs are elided") {
    val lines = (1 to 20).map(_.toString)
    assertNoDiff(
      SnapshotDiff(lines.mkString("\n"), lines.updated(9, "x").mkString("\n")),
      """| ...
         | 7
         | 8
         | 9
         |-10
         |+x
         | 11
         | 12
         | 13
         | ...""".stripMargin
    )
  }
