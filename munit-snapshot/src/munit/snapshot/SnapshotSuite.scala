package munit.snapshot

import java.io.BufferedWriter
import java.io.OutputStreamWriter
import scala.collection.mutable.LinkedHashMap

import munit._
import munit.internal.console.StackTraces
import upickle.default._

trait SnapshotSuite extends FunSuite {

  private var file: os.Path = null
  private val json = new LinkedHashMap[String, ujson.Value]
  private var initialJson: LinkedHashMap[String, ujson.Value] = null
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
      if (executedTests == 0) {
        val path = os.Path(loc.path)
        file = path / os.up / s"${path.last.stripSuffix("scala")}json"
        initialJson =
          if (os.isFile(file)) ujson.read(os.read.stream(file)).obj.value
          else new LinkedHashMap[String, ujson.Value]
        currentTests = munitTests.view.map(_.name).toSet
      }
      executedTests += 1
      initialJson.get(options.name) match {
        case Some(v) =>
          assertEquals(body, read[T](v))
          json += options.name -> v
        case None =>
          json += options.name -> writeJs(body)
      }
    }
    // Last test executed
    if (executedTests == currentTests.size) {
      val withoutStales = json.filter { case (k, _) => currentTests(k) }
      if (withoutStales.nonEmpty) {
        val writer = new BufferedWriter(
          new OutputStreamWriter(os.write.over.outputStream(file))
        )
        ujson.writeTo(initialJson ++ withoutStales, writer, 2)
        writer.write('\n')
        writer.close()
      }
    }
  }
}
