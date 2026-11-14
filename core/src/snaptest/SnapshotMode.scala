package snaptest

/** Controls whether snapshot files are written.
  *
  * The default mode is read from the `SNAPTEST` environment variable (`auto`,
  * `always`, `prune` or `no`). When it is not set, the mode is [[No]] on CI
  * (when the `CI` environment variable is set) or where files cannot be written
  * (Scala.js in a browser), and [[Auto]] otherwise.
  */
enum SnapshotMode:

  /** Record new snapshots, fail on mismatching ones */
  case Auto

  /** Record new snapshots and overwrite mismatching ones */
  case Always

  /** Like [[Auto]], and also remove the snapshots that were not checked. Use it
    * on a full run, for frameworks that cannot tell whether every test ran.
    */
  case Prune

  /** Never write snapshot files, fail on new and mismatching snapshots */
  case No

object SnapshotMode:

  val EnvVar = "SNAPTEST"

  def fromEnv: SnapshotMode =
    Platform.env(EnvVar).map(_.trim.toLowerCase).filter(_.nonEmpty) match
      case Some("auto")   => Auto
      case Some("always") => Always
      case Some("prune")  => Prune
      case Some("no")     => No
      case Some(other)    =>
        throw IllegalArgumentException(
          s"Invalid $EnvVar value '$other', expected one of: auto, always, prune, no"
        )
      case None => if isCI || !Platform.canWrite then No else Auto

  private def isCI: Boolean =
    Platform.env("CI").exists(v => v.nonEmpty && v != "false" && v != "0")
