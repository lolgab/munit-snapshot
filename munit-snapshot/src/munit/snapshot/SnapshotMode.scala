package munit.snapshot

/** Controls whether snapshot files are written.
  *
  * The default mode is read from the `MUNIT_SNAPSHOT` environment variable
  * (`auto`, `always` or `no`). When it is not set, the mode is [[No]] on CI
  * (when the `CI` environment variable is set) or where files cannot be written
  * (Scala.js in a browser), and [[Auto]] otherwise.
  */
enum SnapshotMode:

  /** Record new snapshots, fail on mismatching ones */
  case Auto

  /** Record new snapshots and overwrite mismatching ones */
  case Always

  /** Never write snapshot files, fail on new and mismatching snapshots */
  case No

object SnapshotMode:

  val EnvVar = "MUNIT_SNAPSHOT"

  def fromEnv: SnapshotMode =
    Platform.env(EnvVar).map(_.trim.toLowerCase).filter(_.nonEmpty) match
      case Some("auto")   => Auto
      case Some("always") => Always
      case Some("no")     => No
      case Some(other)    =>
        throw IllegalArgumentException(
          s"Invalid $EnvVar value '$other', expected one of: auto, always, no"
        )
      case None => if isCI || !Platform.canWrite then No else Auto

  private def isCI: Boolean =
    Platform.env("CI").exists(v => v.nonEmpty && v != "false" && v != "0")
