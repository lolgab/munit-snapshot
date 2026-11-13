package munit.snapshot

import java.io.BufferedWriter
import java.io.OutputStreamWriter

import scala.collection.mutable

import munit.*
import munit.internal.console.StackTraces
import upickle.default.*

trait SnapshotSuite extends FunSuite:

  // Snapshot file and its content, loaded when the first snapshot test runs
  private var snapshot: Option[(os.Path, mutable.Map[String, ujson.Value])] =
    None
  private val json = mutable.LinkedHashMap.empty[String, ujson.Value]
  private val snapshotTests = mutable.LinkedHashSet.empty[String]

  private def loadSnapshot(
      loc: Location
  ): (os.Path, mutable.Map[String, ujson.Value]) =
    snapshot.getOrElse {
      val path = os.Path(loc.path)
      val file = path / os.up / s"${path.last.stripSuffix(path.ext)}json"
      val initialJson =
        if os.isFile(file) then ujson.read(os.read.stream(file)).obj.value
        else mutable.Map.empty[String, ujson.Value]
      snapshot = Some((file, initialJson))
      (file, initialJson)
    }

  def snapshotTest[T: ReadWriter](name: String)(body: => T)(using
      Location
  ): Unit =
    snapshotTest(TestOptions(name))(body)

  def snapshotTest[T: ReadWriter](options: TestOptions)(body: => T)(using
      loc: Location
  ): Unit =
    snapshotTests += options.name
    test(options) {
      StackTraces.dropInside {
        val (file, initialJson) = loadSnapshot(loc)
        try
          initialJson.get(options.name) match
            case Some(v) =>
              json += options.name -> v
              assertEquals(body, read[T](v))
            case None =>
              json += options.name -> writeJs(body)
        finally
          if options.name == snapshotTests.last then
            val finalJson = json.filter((k, _) => snapshotTests(k))
            if !finalJson.iterator.sameElements(initialJson) then
              val writer = BufferedWriter(
                OutputStreamWriter(os.write.over.outputStream(file))
              )
              ujson.writeTo(finalJson, writer, 2)
              writer.write('\n')
              writer.close()
      }
    }
