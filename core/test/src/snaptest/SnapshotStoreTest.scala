package snaptest

class SnapshotStoreTest extends munit.FunSuite:

  private val initial = Seq("a" -> "1", "b" -> "2")

  test("unchanged") {
    val store = SnapshotStore(initial)
    assertEquals(store.expected("a"), Some("1"))
    assertEquals(store.expected("b"), Some("2"))
    assertEquals(store.result(prune = true), None)
  }

  test("updates keep the order and append new snapshots") {
    val store = SnapshotStore(initial)
    store.update("c", "3")
    store.update("a", "0")
    assertEquals(
      store.result(prune = false),
      Some(Seq("a" -> "0", "b" -> "2", "c" -> "3"))
    )
  }

  test("prune removes unchecked snapshots") {
    val store = SnapshotStore(initial)
    store.expected("b")
    assertEquals(store.result(prune = false), None)
    assertEquals(store.result(prune = true), Some(Seq("b" -> "2")))
  }
