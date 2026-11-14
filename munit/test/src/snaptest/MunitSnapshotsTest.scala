package snaptest

class MunitSnapshotsTest extends munit.FunSuite, MunitSnapshots:
  snapshotTest("mul")(2 * 2)
  snapshotTest("add")(2 + 2)
  snapshotTest("list concatenation") {
    Seq(1, 2, 3, 4) ++ Seq(5, 6, 7, 8)
  }

  case class TestClass(a: String, b: Either[String, String], c: Seq[Int])

  snapshotTest("class") {
    TestClass(
      "Hello",
      Right("World"),
      Seq(1, 2, 3, 4, 5, 6)
    )
  }

  snapshotTest("strings are stored verbatim") {
    "first line\nwith \"quotes\" and a \\ backslash\n\ttabbed"
  }

  snapshotTest("strings needing escapes") {
    "\"starts with a quote, contains \"\"\" and\r\n\u0000"
  }

  snapshotTest("unicode") {
    "naïve café 😀"
  }

  snapshotTest("long values are wrapped") {
    (1 to 40).map(i => s"item$i").toList
  }

  test("multiple snapshots in a test") {
    assertSnapshot(Map("a" -> 1))
    assertSnapshot(Some(List(1, 2)))
    assertSnapshot(Vector.empty[Int], label = "empty")
  }

  test("custom render") {
    given SnapshotRender[Int] = i => s"<$i>"
    assertSnapshot(42)
  }
