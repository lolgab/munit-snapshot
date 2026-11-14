package snaptest

import org.specs2.mutable.Specification

class Specs2SnapshotsTest extends Specification, Specs2Snapshots:

  case class User(name: String, roles: List[String])

  "snapshots" should {
    "match values" in {
      User("ada", List("admin", "dev")) must matchSnapshot("user")
    }

    "match strings verbatim" in {
      "multi\nline" must matchSnapshot("multi line")
    }

    "use custom renders" in {
      given SnapshotRender[Int] = i => s"<$i>"
      42 must matchSnapshot("custom render")
    }
  }
