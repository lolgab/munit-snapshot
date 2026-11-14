package snaptest

import org.portablescala.reflect.annotation.EnableReflectiveInstantiation

/** Snapshots of a suite, implemented by the `<Suite>Snapshots` object generated
  * next to it.
  */
@EnableReflectiveInstantiation
trait Snapshots:

  /** Snapshots in file order, which is kept when the file is rewritten. Lookups
    * go through a map built once per suite.
    */
  def entries: Seq[(String, String)]
