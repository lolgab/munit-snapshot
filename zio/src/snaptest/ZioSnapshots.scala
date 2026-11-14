package snaptest

import zio.*
import zio.internal.stacktracer.SourceLocation
import zio.test.*

/** Snapshot testing for ZIO Test specs:
  *
  * {{{
  * object ParserSpec extends ZIOSpecDefault, ZioSnapshots:
  *   def spec = suite("parser")(
  *     test("sum") {
  *       assertZIOSnapshot(parse("1 + 2")) // key: parser / sum
  *     }
  *   )
  * }}}
  *
  * Snapshots are keyed by the labels of the suites and test, and written after
  * all tests ran. Tests can be filtered, so snapshots that are no longer
  * checked are only removed with the `prune` [[SnapshotMode]].
  *
  * Both are done by [[snapshotAspects]], added to `aspects`. A spec overriding
  * `aspects` must keep them with `super.aspects ++ ...`.
  */
trait ZioSnapshots extends ZIOSpecAbstract:

  /** Whether snapshot files are written, see [[SnapshotMode]] */
  def snapshotMode: SnapshotMode = SnapshotMode.fromEnv

  private lazy val session = SnapshotSession(getClass, snapshotMode)

  private val currentTest =
    Unsafe.unsafe(unsafe =>
      FiberRef.unsafe.make(Option.empty[SnapshotSession.Test])(using unsafe)
    )

  /** Gives each test its snapshots, and writes them after all tests ran */
  def snapshotAspects: Chunk[TestAspect[Nothing, Any, Nothing, Any]] = Chunk(
    new TestAspect[Nothing, Any, Nothing, Any]:
      def some[R, E](spec: Spec[R, E])(using Trace): Spec[R, E] =
        withTests(spec, Vector.empty)
    ,
    TestAspect.afterAll(
      ZIO.attemptBlocking(session.finish(allRan = false)).orDie
    )
  )

  override def aspects
      : Chunk[TestAspectAtLeastR[Environment & TestEnvironment]] =
    super.aspects ++ snapshotAspects

  // Labels are only known walking the spec from the root
  private def withTests[R, E](spec: Spec[R, E], labels: Vector[String])(using
      Trace
  ): Spec[R, E] =
    spec.caseValue match
      case Spec.LabeledCase(label, inner) =>
        Spec(Spec.LabeledCase(label, withTests(inner, labels :+ label)))
      case Spec.MultipleCase(specs) =>
        Spec(Spec.MultipleCase(specs.map(withTests(_, labels))))
      case Spec.ExecCase(exec, inner) =>
        Spec(Spec.ExecCase(exec, withTests(inner, labels)))
      case Spec.ScopedCase(scoped) =>
        Spec(Spec.ScopedCase(scoped.map(withTests(_, labels))))
      case Spec.TestCase(test, annotations) =>
        val run = ZIO.suspendSucceed(
          currentTest.locally(Some(session.test(labels.mkString(" / "))))(test)
        )
        Spec(Spec.TestCase(run, annotations))

  /** Checks that `obtained` matches the snapshot stored for the current test.
    *
    * Calling it more than once in a test stores numbered snapshots, unless a
    * `label` distinguishes them.
    */
  def assertSnapshot[A](obtained: A, label: String = "")(using
      render: SnapshotRender[A],
      trace: Trace,
      loc: SourceLocation,
      file: sourcecode.File
  ): UIO[TestResult] =
    currentTest.get.flatMap {
      case None =>
        ZIO.succeed(
          assertNever("assertSnapshot can only be called in a test of the spec")
        )
      case Some(test) =>
        ZIO
          .attemptBlocking(
            test.check(render.render(obtained), file.value, label)
          )
          .orDie
          .map {
            case result @ SnapshotResult.Mismatch(_, snapshot, obtained, _) =>
              // The assertion code is printed, so it is kept readable
              val equalToSnapshot = Assertion.equalTo(snapshot)
              assert(obtained)(equalToSnapshot) ?? result.failure.get
            case result =>
              result.failure.fold(assertCompletes)(assertNever(_))
          }
    }

  /** Checks that the result of `effect` matches the snapshot stored for the
    * current test, like `assertZIO` does with an assertion
    */
  def assertZIOSnapshot[R, E, A](effect: ZIO[R, E, A], label: String = "")(using
      render: SnapshotRender[A],
      trace: Trace,
      loc: SourceLocation,
      file: sourcecode.File
  ): ZIO[R, E, TestResult] =
    effect.flatMap(assertSnapshot(_, label))

  /** Snapshotting a `ZIO` would store its description, not its result */
  protected inline given zioSnapshotRender[R, E, A]
      : SnapshotRender[ZIO[R, E, A]] =
    compiletime.error(
      "Cannot snapshot a ZIO value, use assertZIOSnapshot to snapshot its result"
    )
