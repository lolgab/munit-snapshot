package snaptest

import scala.collection.mutable
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.util.control.NonFatal

import utest.TestSuite
import utest.framework.Tree

/** Snapshot testing for uTest suites:
  *
  * {{{
  * object ExampleTests extends TestSuite, UTestSnapshots:
  *   val tests = Tests {
  *     test("parser") {
  *       assertSnapshot(parse("1 + 2"))
  *     }
  *   }
  * }}}
  *
  * Snapshots are keyed by test path, like `parser.nested`.
  */
trait UTestSnapshots extends TestSuite:

  /** Whether snapshot files are written, see [[SnapshotMode]] */
  def snapshotMode: SnapshotMode = SnapshotMode.fromEnv

  private lazy val session = SnapshotSession(getClass, snapshotMode)

  @volatile private var running: Option[SnapshotSession.Test] = None
  private val passedTests = mutable.Set.empty[Seq[String]]

  override def utestWrap(path: Seq[String], runBody: => Future[Any])(using
      ec: ExecutionContext
  ): Future[Any] =
    running = Some(session.test(path.mkString(".")))
    val result =
      try super.utestWrap(path, runBody)
      catch case NonFatal(e) => Future.failed(e)
    result.andThen { r =>
      running = None
      if r.isSuccess then passedTests.synchronized(passedTests += path)
    }

  /** Checks that `obtained` matches the snapshot stored for the current test.
    *
    * Calling it more than once in a test stores numbered snapshots, unless a
    * `label` distinguishes them.
    */
  def assertSnapshot[T](obtained: T, label: String = "")(using
      render: SnapshotRender[T],
      file: sourcecode.File
  ): Unit =
    val test = running.getOrElse(
      throw AssertionError(
        "assertSnapshot can only be called while a test is running"
      )
    )
    test
      .check(render.render(obtained), file.value, label)
      .failureWithDiff
      .foreach(message => throw AssertionError(message))

  override def utestAfterAll(): Unit =
    try
      // Snapshots of tests that did not run (filtered) or did not pass must
      // be kept
      val allRan =
        passedTests.synchronized(leaves(tests.nameTree).forall(passedTests))
      session.finish(allRan)
    finally super.utestAfterAll()

  // Paths of the tests, the root of the tree is the suite itself
  private def leaves(tree: Tree[String]): Seq[Seq[String]] =
    def loop(path: Seq[String], node: Tree[String]): Seq[Seq[String]] =
      if node.children.isEmpty then Seq(path)
      else node.children.flatMap(c => loop(path :+ c.value, c))
    loop(Nil, tree)
