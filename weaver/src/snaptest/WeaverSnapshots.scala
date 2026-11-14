package snaptest

import cats.effect.IO
import cats.effect.Resource
import weaver.Expectations
import weaver.IOSuite
import weaver.SourceLocation

/** Snapshot testing for weaver suites:
  *
  * {{{
  * object ParserSuite extends SimpleIOSuite, WeaverSnapshots:
  *   test("parser") {
  *     parseIO("1 + 2").flatMap(expectSnapshot("parser", _))
  *   }
  *
  *   snapshotTest("sum")(parseIO("1 + 2"))
  * }}}
  *
  * Weaver tests do not know their name, so snapshots are keyed explicitly.
  * Tests run in parallel and can be filtered, so snapshots that are no longer
  * checked are only removed with the `prune` [[SnapshotMode]].
  *
  * Snapshots are written when the shared resource is released. A suite defining
  * its own `sharedResource` must add [[finishSnapshots]] to it:
  * {{{
  * override def sharedResource = myResource.onFinalize(finishSnapshots)
  * }}}
  */
trait WeaverSnapshots extends IOSuite:

  /** Whether snapshot files are written, see [[SnapshotMode]] */
  def snapshotMode: SnapshotMode = SnapshotMode.fromEnv

  private lazy val session = SnapshotSession(getClass, snapshotMode)

  /** Writes the snapshots that changed */
  def finishSnapshots: IO[Unit] = IO.blocking(session.finish(allRan = false))

  abstract override def sharedResource: Resource[IO, Res] =
    super.sharedResource.onFinalize(finishSnapshots)

  /** Expects `obtained` to match the snapshot stored under `key` */
  def expectSnapshot[A](key: String, obtained: A)(using
      render: SnapshotRender[A],
      loc: SourceLocation,
      file: sourcecode.File
  ): IO[Expectations] =
    IO {
      session
        .test(key)
        .check(render.render(obtained), file.value)
        .failureWithDiff
        .fold(success)(failure(_))
    }

  /** Defines a test checking the result of `body` against the snapshot keyed by
    * the test name
    */
  def snapshotTest[A](name: String)(body: IO[A])(using
      render: SnapshotRender[A],
      loc: SourceLocation,
      file: sourcecode.File
  ): Unit =
    test(name)(body.flatMap(expectSnapshot(name, _)))

  /** Snapshotting an `IO` would store its description, not its result */
  protected inline given ioSnapshotRender[A]: SnapshotRender[IO[A]] =
    compiletime.error(
      "Cannot snapshot an IO value, use flatMap to snapshot its result"
    )
