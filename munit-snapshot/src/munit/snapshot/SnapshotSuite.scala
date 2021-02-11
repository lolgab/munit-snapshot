package munit.snapshot

import munit._
import munit.internal.console.StackTraces
import upickle.default._

trait SnapshotSuite extends FunSuite {

  private var file: os.Path = null
  private var json: ujson.Obj = null
  private var initialJson: Map[String, ujson.Value] = null
  private var currentTests: Set[String] = null
  private var executedTests = 0

  def snapshotTest[T: ReadWriter](
      name: String
  )(body: => T)(implicit loc: Location): Unit = {
    snapshotTest(new TestOptions(name))(body)
  }
  def snapshotTest[T: ReadWriter](
      options: TestOptions
  )(body: => T)(implicit loc: Location): Unit = test(options) {
    StackTraces.dropInside {
      // First test executed
      if (file == null && json == null && initialJson == null) {
        val path = os.Path(loc.path)
        file = path / os.up / s"${path.last.stripSuffix("scala")}json"
        json = if(os.isFile(file)) ujson.read(os.read.stream(file)).obj else ujson.Obj()
        initialJson = json.value.toMap
        currentTests = munitTests.view.map(_.name).toSet
      }
      executedTests += 1

      def pair = options.name -> writeJs(body)

      json.value.get(options.name) match {
        case Some(v) => assertEquals(body, read[T](v))
        case None => json.value += pair
      }
    }
    // Last test executed
    if(executedTests == currentTests.size) {
      val withoutStales = json.value.iterator.filter { case (k, _) => currentTests(k) }.toSeq.sortBy(_._1).toMap
      if (withoutStales != initialJson) {
        os.write.over(file, ujson.write(withoutStales, 2) + "\n")
      }
    }
  }
}
