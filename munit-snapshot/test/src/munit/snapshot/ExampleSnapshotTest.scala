package example

import munit.snapshot._
import upickle.default._

class ExampleSnapshotTest extends SnapshotSuite {
  snapshotTest("one plus one") {
    1 + 1
  }
}
