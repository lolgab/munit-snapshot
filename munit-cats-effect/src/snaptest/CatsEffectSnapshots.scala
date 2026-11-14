package snaptest

import cats.effect.IO
import munit.CatsEffectSuite
import munit.Location

/** Snapshot testing for munit-cats-effect suites:
  *
  * {{{
  * class ParserTest extends CatsEffectSuite, CatsEffectSnapshots:
  *   test("parser") {
  *     assertIOSnapshot(parseIO("1 + 2"))
  *   }
  * }}}
  */
trait CatsEffectSnapshots extends CatsEffectSuite, MunitSnapshots:

  /** Checks that the result of `obtained` matches the snapshot stored for the
    * current test, like `assertIO` does with an expected value
    */
  def assertIOSnapshot[A](obtained: IO[A], label: String = "")(using
      render: SnapshotRender[A],
      loc: Location
  ): IO[Unit] =
    obtained.flatMap(value => IO(assertSnapshot(value, label)))

  /** Snapshotting an `IO` would store its description, not its result */
  protected inline given ioSnapshotRender[A]: SnapshotRender[IO[A]] =
    compiletime.error(
      "Cannot snapshot an IO value, use assertIOSnapshot to snapshot its result"
    )
