package snaptest

import org.specs2.execute.Failure
import org.specs2.execute.Result
import org.specs2.execute.Success
import org.specs2.matcher.Matcher
import org.specs2.specification.AfterAll

/** Snapshot testing for specs2 specifications, with a matcher:
  *
  * {{{
  * class ParserSpec extends Specification, Specs2Snapshots:
  *   "parser" should {
  *     "parse sums" in {
  *       parse("1 + 2") must matchSnapshot("parse sums")
  *     }
  *   }
  * }}}
  *
  * specs2 does not expose the running example, so snapshots are keyed
  * explicitly. For the same reason, snapshots that are no longer checked are
  * only removed with the `prune` [[SnapshotMode]].
  */
trait Specs2Snapshots extends AfterAll:

  /** Whether snapshot files are written, see [[SnapshotMode]] */
  def snapshotMode: SnapshotMode = SnapshotMode.fromEnv

  private lazy val session = SnapshotSession(getClass, snapshotMode)

  /** Matches values equal to the snapshot stored under `key` */
  def matchSnapshot[T](key: String)(using
      render: SnapshotRender[T],
      file: sourcecode.File
  ): Matcher[T] =
    Matcher[T, Result] { (value: T) =>
      session
        .test(key)
        .check(render.render(value), file.value)
        .failureWithDiff
        .fold(Success()) { message =>
          // specs2 reports the first frame outside of itself, which would be
          // this matcher instead of the example
          val stackTrace = Exception().getStackTrace.toList
            .filterNot(_.getClassName.startsWith("snaptest.Specs2Snapshots"))
          Failure(message, trace = stackTrace)
        }
    }

  def afterAll(): Unit = session.finish(allRan = false)
