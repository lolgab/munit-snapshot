package munit.snapshot

import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Files
import java.nio.file.Paths

private[snapshot] object Platform:

  val canWrite = true

  def classLoader(cls: Class[?]): ClassLoader = cls.getClassLoader

  def writeFile(path: String, content: String): Unit =
    val p = Paths.get(path)
    Files.createDirectories(p.getParent)
    Files.write(p, content.getBytes(UTF_8))

  def deleteFile(path: String): Unit =
    Files.deleteIfExists(Paths.get(path))

  def env(name: String): Option[String] = sys.env.get(name)
