package munit.snapshot

/** Renders a value to the text stored in a snapshot file.
  *
  * Strings are stored verbatim, everything else is pretty-printed with
  * [[https://github.com/com-lihaoyi/PPrint pprint]]. Provide a given instance
  * to customize how a type is rendered, for example to hide timestamps or
  * random identifiers.
  */
trait SnapshotRender[T]:
  def render(value: T): String

object SnapshotRender:

  /** Maximum line width of pretty-printed values */
  val Width = 100

  def apply[T](using r: SnapshotRender[T]): SnapshotRender[T] = r

  given string: SnapshotRender[String] = identity(_)

  given default: [T] => SnapshotRender[T] = value =>
    pprint.PPrinter.BlackWhite
      .apply(value, width = Width, height = Int.MaxValue)
      .plainText
