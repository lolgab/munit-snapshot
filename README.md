# munit-snapshot

Snapshot testing for MUnit.
This project is a basic implementation of the [snapshot tests](https://jestjs.io/docs/en/snapshot-testing).

## Getting started

After adding the `munit-snapshot` dependency:

```scala
libraryDependencies += "com.github.lolgab" %% "munit-snapshot" % "0.0.2"
```

You can create a basic test in a file called `ExampleSnapshotTest.scala`:

```scala
package example

import munit.snapshot._
import upickle.default._

class ExampleSnapshotTest extends SnapshotSuite {
  snapshotTest("one plus one") {
    1 + 1
  }
}
```

Running the tests the first time will generate a file
called `ExampleSnapshotTest.json` with the following content:

```json
{
  "one plus one": 2
}
```

where `"one plus one"` is the test name (it should be unique in the file) and
`2` is the result of the test evaluation.

The subsequent runs will evaluate the expression again, and fail when they do
not match the saved json file.

If a test is removed, running the tests will remove the key from the json file.

Here you can see what happens if I change the json file to:

```json
{
  "one plus one": 3
}
```

Running the test again will output:

```
6:class ExampleSnapshotTest extends SnapshotSuite {
7:  snapshotTest("one plus one") {
8:    1 + 1
values are not the same
=> Obtained
3
=> Diff (- obtained, + expected)
-3
+2
```
