package snaptest

import utest.*

object UTestSnapshotsTest extends TestSuite, UTestSnapshots:

  case class User(name: String, roles: List[String])

  val tests = Tests {
    test("values") {
      assertSnapshot(User("ada", List("admin", "dev")))
    }

    test("multiple snapshots in a test") {
      assertSnapshot(1 + 1)
      assertSnapshot("multi\nline")
      assertSnapshot(Map("a" -> 1), label = "map")
    }

    test("nested") {
      test("first") {
        assertSnapshot("first")
      }
      test("second") {
        assertSnapshot("second")
      }
    }
  }
