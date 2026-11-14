package snaptest

import scala.compiletime.testing.typeCheckErrors

import cats.effect.IO
import cats.syntax.all.*
import weaver.SimpleIOSuite

object WeaverSnapshotsTest extends SimpleIOSuite, WeaverSnapshots:

  case class User(name: String, roles: List[String])

  test("explicit keys") {
    IO(User("ada", List("admin", "dev"))).flatMap(expectSnapshot("user", _))
  }

  snapshotTest("snapshot test")(IO("multi\nline"))

  test("parallel checks") {
    (1 to 5).toList
      .parTraverse(i => expectSnapshot(s"parallel $i", i * i))
      .map(_.combineAll)
  }

  pureTest("io values are rejected at compile time") {
    val errors = typeCheckErrors("""expectSnapshot("k", IO(1))""")
    expect(errors.exists(_.message.startsWith("Cannot snapshot an IO value")))
  }
