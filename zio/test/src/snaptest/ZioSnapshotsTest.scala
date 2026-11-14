package snaptest

import scala.compiletime.testing.typeCheckErrors

import zio.*
import zio.test.*

object ZioSnapshotsTest extends ZIOSpecDefault, ZioSnapshots:

  case class User(name: String, roles: List[String])

  def spec = suite("snapshots")(
    test("values") {
      assertSnapshot(User("ada", List("admin", "dev")))
    },
    test("effect results") {
      assertZIOSnapshot(ZIO.succeed("multi\nline"))
    },
    test("multiple snapshots in a test") {
      for
        first <- assertSnapshot(1 + 1)
        second <- assertSnapshot(List(1, 2))
        labeled <- assertSnapshot(Map("a" -> 1), label = "map")
      yield first && second && labeled
    },
    suite("nested")(
      (1 to 5).map(i => test(s"parallel $i")(assertSnapshot(i * i)))*
    ),
    test("zio values are rejected at compile time") {
      val errors = typeCheckErrors("assertSnapshot(ZIO.succeed(1))")
      assertTrue(
        errors.exists(_.message.startsWith("Cannot snapshot a ZIO value"))
      )
    }
  )
