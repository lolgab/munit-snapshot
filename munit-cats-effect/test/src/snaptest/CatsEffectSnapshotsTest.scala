package snaptest

import cats.effect.IO
import munit.CatsEffectSuite

class CatsEffectSnapshotsTest extends CatsEffectSuite, CatsEffectSnapshots:

  test("io results") {
    assertIOSnapshot(IO(List(1, 2, 3)))
  }

  test("plain values") {
    assertSnapshot(Map("a" -> 1))
  }

  test("io values are rejected at compile time") {
    assert(
      compileErrors("assertSnapshot(IO(1))")
        .contains("Cannot snapshot an IO value, use assertIOSnapshot")
    )
  }
