package munit.snapshot

import scala.collection.mutable

/** Snapshots of a suite: the ones compiled in its [[Snapshots]] object, updated
  * during the test run.
  */
private[snapshot] final class SnapshotStore(
    val initial: Seq[(String, String)]
):

  private val entries = mutable.LinkedHashMap.from(initial)
  private val checked = mutable.Set.empty[String]
  private var updated = false

  def expected(key: String): Option[String] =
    checked += key
    entries.get(key)

  def update(key: String, value: String): Unit =
    checked += key
    entries(key) = value
    updated = true

  /** Snapshots to write back, or `None` when nothing changed.
    *
    * Snapshots that were not checked are kept unless `prune` is set, which is
    * only safe when every test of the suite ran successfully.
    */
  def result(prune: Boolean): Option[Seq[(String, String)]] =
    val kept =
      if prune then entries.filter((k, _) => checked(k)).toSeq
      else entries.toSeq
    Option.when(updated || kept.size != entries.size)(kept)
