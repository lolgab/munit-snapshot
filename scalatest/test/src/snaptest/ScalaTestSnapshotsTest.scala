package snaptest

import scala.concurrent.Future

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.funsuite.AsyncFunSuite

class ScalaTestSnapshotsTest extends AnyFunSuite, ScalaTestSnapshots:

  case class User(name: String, roles: List[String])

  test("values") {
    assertSnapshot(User("ada", List("admin", "dev")))
  }

  test("multiple snapshots in a test") {
    assertSnapshot(1 + 1)
    assertSnapshot("multi\nline")
    assertSnapshot(Map("a" -> 1), label = "map")
  }

  test("custom render") {
    given SnapshotRender[Int] = i => s"<$i>"
    assertSnapshot(42)
  }

  ignore("ignored tests do not prevent pruning") {
    assertSnapshot("never checked")
  }

class AsyncScalaTestSnapshotsTest
    extends AsyncFunSuite,
      AsyncScalaTestSnapshots:

  test("asynchronous values") {
    Future(List(1, 2, 3)).map(assertSnapshot(_))
  }
