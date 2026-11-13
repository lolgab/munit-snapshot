package example

import munit.snapshot.*

class ExampleSnapshotTest extends SnapshotSuite:
  snapshotTest("one plus one") {
    1 + 1
  }
