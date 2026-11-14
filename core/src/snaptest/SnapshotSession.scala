package snaptest

import org.portablescala.reflect.Reflect

/** Snapshots of a test suite, shared by its tests.
  *
  * Test framework integrations create one session per suite, a
  * [[SnapshotSession.Test]] per running test, and call [[finish]] once the
  * suite completed. Sessions are thread safe, so tests can run in parallel.
  *
  * @param suite
  *   the suite class, its snapshots are stored in the generated
  *   `<Suite>Snapshots` object in the same package
  */
final class SnapshotSession(
    suite: Class[?],
    val mode: SnapshotMode = SnapshotMode.fromEnv
):

  private val names = SnapshotSource.names(suite.getName)

  // Snapshots file and its content, loaded by the first check because the
  // path depends on the location of the test sources
  private var loaded: Option[(String, SnapshotStore)] = None

  private def load(sourcePath: String): (String, SnapshotStore) =
    loaded.getOrElse {
      // Relative source paths (Scala CLI) resolve from the working directory
      val dir = sourcePath.take(
        sourcePath.lastIndexWhere(c => c == '/' || c == '\\') + 1
      )
      // Scala CLI compiles `.test.scala` files with the test dependencies
      val ext =
        if sourcePath.endsWith(".test.scala") then ".test.scala" else ".scala"
      val path = s"${dir}snapshots/${names.obj}$ext"
      val initial = Reflect
        .lookupLoadableModuleClass(
          s"${names.fullName}$$",
          Platform.classLoader(suite)
        )
        .fold(Nil)(_.loadModule().asInstanceOf[Snapshots].entries)
      val result = (path, SnapshotStore(initial))
      loaded = Some(result)
      result
    }

  /** Starts checking the snapshots of the test `name`.
    *
    * Snapshots are keyed by test name, then numbered when a test checks more
    * than one, unless a label distinguishes them.
    */
  def test(name: String): SnapshotSession.Test =
    SnapshotSession.Test(this, name)

  private[snaptest] def check(
      key: String,
      value: String,
      sourcePath: String
  ): SnapshotResult = synchronized {
    val (path, store) = load(sourcePath)
    store.expected(key) match
      case Some(expected) if expected == value    => SnapshotResult.Matched
      case Some(_) if mode == SnapshotMode.Always =>
        store.update(key, value)
        SnapshotResult.Recorded
      case Some(expected) =>
        val line = SnapshotSource.line(names, store.initial, key)
        SnapshotResult.Mismatch(
          key,
          expected,
          value,
          path + line.fold("")(l => s":$l")
        )
      case None if mode == SnapshotMode.No =>
        SnapshotResult.Missing(key, path)
      case None =>
        store.update(key, value)
        SnapshotResult.Recorded
  }

  /** Writes the snapshots that changed.
    *
    * Snapshots that were not checked are removed when `allRan` is set, which
    * integrations only do when every test of the suite ran and passed, or in
    * the [[SnapshotMode.Prune]] mode.
    */
  def finish(allRan: Boolean): Unit = synchronized {
    for (path, store) <- loaded if mode != SnapshotMode.No do
      val prune = allRan || mode == SnapshotMode.Prune
      store.result(prune).foreach { entries =>
        if entries.isEmpty then Platform.deleteFile(path)
        else Platform.writeFile(path, SnapshotSource.render(names, entries))
      }
  }

object SnapshotSession:

  /** Snapshots checked by a running test */
  final class Test private[SnapshotSession] (
      session: SnapshotSession,
      val name: String
  ):
    private var count = 0

    /** Checks `value` against the snapshot of this test.
      *
      * @param sourcePath
      *   the test source file, snapshots are stored in the `snapshots`
      *   directory next to it
      */
    def check(
        value: String,
        sourcePath: String,
        label: String = ""
    ): SnapshotResult =
      val key = synchronized {
        count += 1
        if label.nonEmpty then s"$name: $label"
        else if count == 1 then name
        else s"$name ($count)"
      }
      session.check(key, value, sourcePath)
