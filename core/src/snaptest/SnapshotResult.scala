package snaptest

/** Outcome of checking a value against its snapshot */
enum SnapshotResult:

  /** The value matches the stored snapshot */
  case Matched

  /** The snapshot was new or updated, and will be written */
  case Recorded

  /** The value does not match the stored snapshot
    *
    * @param location
    *   `path:line` of the snapshot, a clickable link in IDEs and terminals
    */
  case Mismatch(
      key: String,
      expected: String,
      obtained: String,
      location: String
  )

  /** There is no snapshot and the mode does not allow recording it */
  case Missing(key: String, path: String)

  /** Failure message, `None` when the check passed */
  def failure: Option[String] = this match
    case Matched | Recorded            => None
    case Mismatch(key, _, _, location) =>
      Some(
        s"Snapshot '$key' does not match, " +
          s"rerun with ${SnapshotMode.EnvVar}=always to update it\n" +
          s"  at $location"
      )
    case Missing(key, path) =>
      Some(
        s"Missing snapshot '$key', run the tests locally or with " +
          s"${SnapshotMode.EnvVar}=always to record it\n  in $path"
      )

  /** [[failure]] followed by a diff, for frameworks without a readable one */
  def failureWithDiff: Option[String] = this match
    case Mismatch(_, expected, obtained, _) =>
      failure.map(f =>
        s"$f\n=> Diff (- expected, + obtained)\n${SnapshotDiff(expected, obtained)}"
      )
    case _ => failure
