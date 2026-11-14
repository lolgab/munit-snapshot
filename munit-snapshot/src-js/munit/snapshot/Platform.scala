package munit.snapshot

import scala.scalajs.js

// Snapshots are compiled in, so reading works everywhere, but writing them
// needs the Node.js filesystem
private[snapshot] object Platform:

  private val process = js.Dynamic.global.process
  private val hasProcess = js.typeOf(process) != "undefined"

  // `process.getBuiltinModule` works with every module kind, including
  // ES modules, while `require` is only available to CommonJS and scripts
  private val hasBuiltinModule =
    hasProcess && js.typeOf(process.getBuiltinModule) == "function"

  val canWrite: Boolean =
    hasBuiltinModule || js.typeOf(js.Dynamic.global.require) == "function"

  private def nodeModule(name: String): js.Dynamic =
    if hasBuiltinModule then process.getBuiltinModule(name)
    else js.Dynamic.global.require(name)

  private lazy val fs = nodeModule("fs")
  private lazy val nodePath = nodeModule("path")

  // Ignored by the Scala.js reflective lookup
  def classLoader(cls: Class[?]): ClassLoader = null

  def writeFile(path: String, content: String): Unit =
    fs.mkdirSync(
      nodePath.dirname(path),
      js.Dynamic.literal(recursive = true)
    )
    fs.writeFileSync(path, content, "utf8")

  def deleteFile(path: String): Unit =
    if fs.existsSync(path).asInstanceOf[Boolean] then fs.unlinkSync(path)

  def env(name: String): Option[String] =
    if hasProcess then
      process.env.selectDynamic(name).asInstanceOf[js.UndefOr[String]].toOption
    else None
