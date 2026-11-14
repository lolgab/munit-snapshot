package snaptest

// A suite and its snapshots, written like the generated ones
class SessionFixture
object SessionFixtureSnapshots extends Snapshots:
  def entries = Seq("a" -> "1", "b" -> "2")

class SnapshotSessionTest extends munit.FunSuite:

  // Sessions in these tests never write: `finish` is only called in `No` mode
  private val source = "/tmp/snaptest/FixtureTest.scala"
  private val path = "/tmp/snaptest/snapshots/SessionFixtureSnapshots.scala"

  test("matched") {
    val session = SnapshotSession(classOf[SessionFixture], SnapshotMode.No)
    assertEquals(session.test("a").check("1", source), SnapshotResult.Matched)
  }

  test("mismatch links to the snapshot line") {
    val session = SnapshotSession(classOf[SessionFixture], SnapshotMode.Auto)
    val result = session.test("b").check("3", source)
    assertEquals(result, SnapshotResult.Mismatch("b", "2", "3", s"$path:10"))
    assert(result.failure.get.contains(s"at $path:10"))
  }

  test("always records mismatches") {
    val session = SnapshotSession(classOf[SessionFixture], SnapshotMode.Always)
    assertEquals(session.test("b").check("3", source), SnapshotResult.Recorded)
  }

  test("auto records new snapshots") {
    val session = SnapshotSession(classOf[SessionFixture], SnapshotMode.Auto)
    assertEquals(session.test("c").check("3", source), SnapshotResult.Recorded)
  }

  test("keys are numbered or labeled") {
    val session = SnapshotSession(classOf[SessionFixture], SnapshotMode.No)
    val test = session.test("t")
    assertEquals(test.check("x", source), SnapshotResult.Missing("t", path))
    assertEquals(test.check("x", source), SnapshotResult.Missing("t (2)", path))
    assertEquals(
      test.check("x", source, label = "l"),
      SnapshotResult.Missing("t: l", path)
    )
    // Counting restarts when the test runs again
    assertEquals(
      session.test("t").check("x", source),
      SnapshotResult.Missing("t", path)
    )
    session.finish(allRan = true)
  }

  test("suites without snapshots") {
    val session = SnapshotSession(classOf[SnapshotSessionTest], SnapshotMode.No)
    assertEquals(
      session.test("a").check("1", source),
      SnapshotResult.Missing(
        "a",
        "/tmp/snaptest/snapshots/SnapshotSessionTestSnapshots.scala"
      )
    )
  }

  test("relative source paths") {
    val session = SnapshotSession(classOf[SessionFixture], SnapshotMode.No)
    assertEquals(
      session.test("missing").check("1", "FixtureTest.scala"),
      SnapshotResult.Missing(
        "missing",
        "snapshots/SessionFixtureSnapshots.scala"
      )
    )
  }

  test(".test.scala files get .test.scala snapshot files") {
    val session = SnapshotSession(classOf[SessionFixture], SnapshotMode.No)
    assertEquals(
      session.test("missing").check("1", "FixtureTest.test.scala"),
      SnapshotResult.Missing(
        "missing",
        "snapshots/SessionFixtureSnapshots.test.scala"
      )
    )
  }

  test("other test files get .scala snapshot files") {
    val session = SnapshotSession(classOf[SessionFixture], SnapshotMode.No)
    assertEquals(
      session.test("missing").check("1", "FixtureTest.spec.scala"),
      SnapshotResult.Missing(
        "missing",
        "snapshots/SessionFixtureSnapshots.scala"
      )
    )
  }
