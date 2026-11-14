package snaptest

import scala.collection.mutable
import scala.concurrent.Future
import scala.util.control.NonFatal

import munit.*
import munit.internal.console.StackTraces

/** Snapshot testing for munit suites:
  *
  * {{{
  * class ExampleTest extends munit.FunSuite, MunitSnapshots:
  *   snapshotTest("one plus one")(1 + 1)
  *
  *   test("parser") {
  *     assertSnapshot(parse("1 + 2"))
  *   }
  * }}}
  */
trait MunitSnapshots extends FunSuite:

  /** Whether snapshot files are written, see [[SnapshotMode]] */
  def snapshotMode: SnapshotMode = SnapshotMode.fromEnv

  private lazy val session = SnapshotSession(getClass, snapshotMode)

  @volatile private var running: Option[SnapshotSession.Test] = None
  private val completedTests = mutable.Set.empty[String]
  @volatile private var anyFailed = false

  override def munitTestTransforms: List[TestTransform] =
    super.munitTestTransforms :+ TestTransform(
      "snapshot",
      test =>
        test.withBody { () =>
          running = Some(session.test(test.name))
          val result =
            try test.body()
            catch case NonFatal(e) => Future.failed(e)
          result.transform { r =>
            running = None
            completedTests += test.name
            if r.isFailure then anyFailed = true
            r
          }(using munitExecutionContext)
        }
    )

  /** Checks that `obtained` matches the snapshot stored for the current test.
    *
    * Calling it more than once in a test stores numbered snapshots, unless a
    * `label` distinguishes them.
    */
  def assertSnapshot[T](obtained: T, label: String = "")(using
      render: SnapshotRender[T],
      loc: Location
  ): Unit =
    StackTraces.dropInside {
      val test = running.getOrElse(
        fail("assertSnapshot can only be called while a test is running")
      )
      val result = test.check(render.render(obtained), loc.path, label)
      result match
        case SnapshotResult.Mismatch(_, expected, value, _) =>
          assertEquals(value, expected, result.failure.get)
        case _ => result.failure.foreach(fail(_))
    }

  /** Defines a test checking the value of `body` against its snapshot */
  def snapshotTest[T: SnapshotRender](name: String)(body: => T)(using
      Location
  ): Unit =
    snapshotTest(TestOptions(name))(body)

  def snapshotTest[T: SnapshotRender](options: TestOptions)(body: => T)(using
      Location
  ): Unit =
    test(options)(assertSnapshot(body))

  override def afterAll(): Unit =
    try
      // Snapshots of tests that did not run (filtered, `.only`) or failed
      // before reaching their assertions must be kept
      val allRan = !anyFailed && munitTests()
        .filterNot(_.tags(Ignore))
        .forall(t => completedTests(t.name))
      session.finish(allRan)
    finally super.afterAll()
