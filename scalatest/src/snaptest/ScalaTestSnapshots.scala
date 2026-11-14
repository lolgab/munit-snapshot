package snaptest

import scala.collection.mutable

import org.scalactic.source.Position
import org.scalatest.*

/** Parts shared by [[ScalaTestSnapshots]] and [[AsyncScalaTestSnapshots]] */
trait ScalaTestSnapshotsBase extends Suite, BeforeAndAfterAll:

  /** Whether snapshot files are written, see [[SnapshotMode]] */
  def snapshotMode: SnapshotMode = SnapshotMode.fromEnv

  private lazy val session = SnapshotSession(getClass, snapshotMode)

  @volatile private var running: Option[SnapshotSession.Test] = None
  private val passedTests = mutable.Set.empty[String]

  protected def snapshotTestStarted(name: String): Unit =
    running = Some(session.test(name))

  protected def snapshotTestCompleted(name: String, outcome: Outcome): Unit =
    running = None
    if outcome.isSucceeded then passedTests.synchronized(passedTests += name)

  /** Checks that `obtained` matches the snapshot stored for the current test.
    *
    * Calling it more than once in a test stores numbered snapshots, unless a
    * `label` distinguishes them.
    */
  def assertSnapshot[T](obtained: T, label: String = "")(using
      render: SnapshotRender[T],
      pos: Position,
      // `Position.filePathname` is only filled with a compiler flag
      file: sourcecode.File
  ): Assertion =
    val test = running.getOrElse(
      failAt("assertSnapshot can only be called while a test is running", pos)
    )
    test
      .check(render.render(obtained), file.value, label)
      .failureWithDiff
      .fold(succeed)(failAt(_, pos))

  // `fail` reports the position of its own call, so it would point here
  private def failAt(message: String, pos: Position): Nothing =
    throw exceptions.TestFailedException(_ => Some(message), None, pos)

  override protected def afterAll(): Unit =
    try
      // Snapshots of tests that did not run (filtered) or did not pass must
      // be kept
      val allRan = passedTests.synchronized(
        testNames.forall(name =>
          passedTests(name) ||
            tags.get(name).exists(_.contains("org.scalatest.Ignore"))
        )
      )
      session.finish(allRan)
    finally super.afterAll()

/** Snapshot testing for ScalaTest suites:
  *
  * {{{
  * class ExampleTest extends AnyFunSuite, ScalaTestSnapshots:
  *   test("parser") {
  *     assertSnapshot(parse("1 + 2"))
  *   }
  * }}}
  */
trait ScalaTestSnapshots extends TestSuite, ScalaTestSnapshotsBase:

  override def withFixture(test: NoArgTest): Outcome =
    snapshotTestStarted(test.name)
    var outcome: Outcome = Canceled()
    try
      outcome = super.withFixture(test)
      outcome
    finally snapshotTestCompleted(test.name, outcome)

/** Snapshot testing for asynchronous ScalaTest suites, like `AsyncFunSuite` */
trait AsyncScalaTestSnapshots extends AsyncTestSuite, ScalaTestSnapshotsBase:

  override def withFixture(test: NoArgAsyncTest): FutureOutcome =
    snapshotTestStarted(test.name)
    super
      .withFixture(test)
      .onCompletedThen(result =>
        snapshotTestCompleted(test.name, result.getOrElse(Canceled()))
      )
