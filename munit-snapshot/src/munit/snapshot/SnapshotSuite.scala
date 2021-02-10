package munit.snapshot

import munit._
import munit.internal.console.StackTraces
import upickle.default._

trait SnapshotSuite extends FunSuite {

  def snapshotTest[T: ReadWriter](
      name: String
  )(body: => T)(implicit loc: Location): Unit = {
    snapshotTest(new TestOptions(name))(body)
  }
  def snapshotTest[T: ReadWriter](
      options: TestOptions
  )(body: => T)(implicit loc: Location): Unit = test(options) {
    StackTraces.dropInside {
      val value = body
      def pair = options.name -> writeJs(value)

      val path = os.Path(loc.path)
      val file = path / os.up / s"${path.last.stripSuffix("scala")}json"
      val toWriteOpt: Option[ujson.Obj] = if (os.isFile(file)) {
        val json = ujson.read(os.read(file)).obj
        val res = json.get(options.name) match {
          case Some(v) =>
            assertEquals(read[T](v), value)
            json
          case None =>
            json
              .addOne(pair)
              .toSeq
              .sortBy(_._1)
        }
        val withoutStales = res.filter { case (name, _) =>
          munitTests.exists(_.name == name)
        }
        if (withoutStales != json) Some(withoutStales) else None
      } else {
        Some(Seq(pair))
      }
      toWriteOpt.foreach(json =>
        os.write.over(file, ujson.write(json, 2) + "\n")
      )
    }
  }
}
