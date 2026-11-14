package snaptest

/** Line diff between a snapshot and the obtained value, for test frameworks
  * without a readable diff of their own
  */
object SnapshotDiff:

  /** Unchanged lines kept around each change */
  private val Context = 3

  /** Lines of `expected` and `obtained`, prefixed by `-` when only in
    * `expected`, `+` when only in `obtained` and a space when in both. Long
    * runs of unchanged lines are elided.
    */
  def apply(expected: String, obtained: String): String =
    val a = expected.split("\n", -1)
    val b = obtained.split("\n", -1)
    // Longest common subsequence lengths of the suffixes
    val lcs = Array.ofDim[Int](a.length + 1, b.length + 1)
    for
      i <- a.indices.reverse
      j <- b.indices.reverse
    do
      lcs(i)(j) =
        if a(i) == b(j) then lcs(i + 1)(j + 1) + 1
        else lcs(i + 1)(j).max(lcs(i)(j + 1))
    val lines = Vector.newBuilder[String]
    var i = 0
    var j = 0
    while i < a.length || j < b.length do
      if i < a.length && j < b.length && a(i) == b(j) then
        lines += s" ${a(i)}"
        i += 1
        j += 1
      else if i < a.length && (j == b.length || lcs(i + 1)(j) >= lcs(i)(j + 1))
      then
        lines += s"-${a(i)}"
        i += 1
      else
        lines += s"+${b(j)}"
        j += 1
    elide(lines.result()).mkString("\n")

  private def elide(lines: Vector[String]): Vector[String] =
    val changed = lines.indices.filter(i => !lines(i).startsWith(" "))
    def near(i: Int) = changed.exists(c => (c - i).abs <= Context)
    val result = Vector.newBuilder[String]
    var skipped = false
    for (line, i) <- lines.zipWithIndex do
      if near(i) then
        result += line
        skipped = false
      else if !skipped then
        result += " ..."
        skipped = true
    result.result()
