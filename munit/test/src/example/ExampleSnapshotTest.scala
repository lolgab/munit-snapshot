package example

import snaptest.*

class ExampleSnapshotTest extends munit.FunSuite, MunitSnapshots:
  snapshotTest("one plus one") {
    1 + 1
  }
