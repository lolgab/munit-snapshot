package munit.snapshot

import scala.collection.mutable
import scala.concurrent.Future
import scala.util.control.NonFatal

import munit.*
import munit.internal.console.StackTraces
import org.portablescala.reflect.Reflect

trait SnapshotSuite extends FunSuite:

  /** Whether snapshot files are written, see [[SnapshotMode]] */
  def snapshotMode: SnapshotMode = SnapshotMode.fromEnv

  /** Directory of the generated snapshots source file.
    *
    * `loc` is the location of the first snapshot assertion, by default the file
    * is in the `snapshots` directory next to it.
    */
  def snapshotDirectory(loc: Location): String =
    val path = loc.path
    val dir = path.substring(0, path.lastIndexWhere(c => c == '/' || c == '\\'))
    s"$dir/snapshots"

  private lazy val snapshotsNames = SnapshotSource.names(getClass.getName)

  private final class RunningTest(val name: String):
    var snapshots = 0

  @volatile private var running: Option[RunningTest] = None
  private val completedTests = mutable.Set.empty[String]
  @volatile private var anyFailed = false

  // Snapshots source file and its content, loaded by the first snapshot
  // assertion
  private var store: Option[(String, SnapshotStore)] = None

  private def loadStore(loc: Location): (String, SnapshotStore) =
    store.getOrElse {
      val path = s"${snapshotDirectory(loc)}/${snapshotsNames.obj}.scala"
      val initial = Reflect
        .lookupLoadableModuleClass(
          s"${snapshotsNames.fullName}$$",
          Platform.classLoader(getClass)
        )
        .fold(Nil)(_.loadModule().asInstanceOf[Snapshots].entries)
      val loaded = (path, SnapshotStore(initial))
      store = Some(loaded)
      loaded
    }

  override def munitTestTransforms: List[TestTransform] =
    super.munitTestTransforms :+ TestTransform(
      "snapshot",
      test =>
        test.withBody { () =>
          running = Some(RunningTest(test.name))
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
      test.snapshots += 1
      val key =
        if label.nonEmpty then s"${test.name}: $label"
        else if test.snapshots == 1 then test.name
        else s"${test.name} (${test.snapshots})"
      val (path, snapshots) = loadStore(loc)
      val value = render.render(obtained)
      val mode = snapshotMode
      snapshots.expected(key) match
        case Some(expected) if expected == value    => ()
        case Some(_) if mode == SnapshotMode.Always =>
          snapshots.update(key, value)
        case Some(expected) =>
          // `path:line` is a clickable link in IDEs and terminals
          val line = SnapshotSource
            .line(snapshotsNames, snapshots.initial, key)
            .fold("")(l => s":$l")
          assertEquals(
            value,
            expected,
            s"Snapshot '$key' does not match, " +
              s"rerun with ${SnapshotMode.EnvVar}=always to update it\n" +
              s"  at $path$line"
          )
        case None if mode == SnapshotMode.No =>
          fail(
            s"Missing snapshot '$key', run the tests locally or with " +
              s"${SnapshotMode.EnvVar}=always to record it\n  in $path"
          )
        case None => snapshots.update(key, value)
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
      for (path, snapshots) <- store if snapshotMode != SnapshotMode.No do
        // Snapshots of tests that did not run (filtered, `.only`) or failed
        // before reaching their assertions must be kept
        val allRan = !anyFailed && munitTests()
          .filterNot(_.tags(Ignore))
          .forall(t => completedTests(t.name))
        snapshots.result(prune = allRan).foreach { entries =>
          if entries.isEmpty then Platform.deleteFile(path)
          else
            Platform.writeFile(
              path,
              SnapshotSource.render(snapshotsNames, entries)
            )
        }
    finally super.afterAll()
