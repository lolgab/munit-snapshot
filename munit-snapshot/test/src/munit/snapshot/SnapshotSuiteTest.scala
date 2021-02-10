package munit.snapshot

import upickle.default._

class SnapshotSuiteTest extends SnapshotSuite {
  snapshotTest("mul") { 2 * 2 }
  snapshotTest("add") { 2 + 2 }
  snapshotTest("list concatenation") {
    Seq(1, 2, 3, 4) ++ Seq(5, 6, 7, 8)
  }

  case class TestClass(a: String, b: Either[String, String], c: Seq[Int])
  implicit val testClassRW = macroRW[TestClass]
  snapshotTest("class") {
    TestClass(
      "Hello",
      Right("World"),
      Seq(1, 2, 3, 4, 5, 6)
    )
  }
}
