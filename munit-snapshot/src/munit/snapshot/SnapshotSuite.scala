package munit.snapshot

import java.io.BufferedWriter
import java.io.OutputStreamWriter

import scala.collection.mutable

import munit._
import munit.internal.console.StackTraces
import upickle.default._

trait SnapshotSuite extends FunSuite {

  private var file: os.Path = null
  private val json = new mutable.LinkedHashMap[String, ujson.Value]
  private var initialJson: mutable.Map[String, ujson.Value] = null
  private val snapshotTests = mutable.LinkedHashSet.empty[String]

  def snapshotTest[T: ReadWriter](
      name: String
  )(body: => T)(implicit loc: Location): Unit = {
    snapshotTest(new TestOptions(name))(body)
  }
  def snapshotTest[T: ReadWriter](
      options: TestOptions
  )(body: => T)(implicit loc: Location): Unit = {
    snapshotTests += options.name
    test(options) {
      StackTraces.dropInside {
        // First test executed
        if (file == null) {
          val path = os.Path(loc.path)
          file = path / os.up / s"${path.last.stripSuffix(path.ext)}json"
        }
        if (initialJson == null) {
          initialJson =
            if (os.isFile(file)) ujson.read(os.read.stream(file)).obj.value
            else mutable.Map.empty[String, ujson.Value]
        }
        try {
          initialJson.get(options.name) match {
            case Some(v) =>
              json += options.name -> v
              assertEquals(body, read[T](v))
            case None =>
              json += options.name -> writeJs(body)
          }
        } finally {
          if (options.name == snapshotTests.last) {
            val finalJson = json.filter { case (k, _) => snapshotTests(k) }
            if (!finalJson.sameElements(initialJson)) {
              val writer = new BufferedWriter(
                new OutputStreamWriter(os.write.over.outputStream(file))
              )
              ujson.writeTo(finalJson, writer, 2)
              writer.write('\n')
              writer.close()
            }
          }
        }
      }
    }
  }
}
