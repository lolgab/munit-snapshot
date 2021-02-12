import mill._, scalalib._, scalanativelib._, publish._
import $ivy.`com.lihaoyi::mill-contrib-bloop:$MILL_VERSION`
import $ivy.`com.goyeau::mill-scalafix:0.2.1`
import com.goyeau.mill.scalafix.ScalafixModule

trait Common extends ScalaModule with PublishModule with ScalafixModule {
  def artifactName = "munit-snapshot"
  def millSourcePath = super.millSourcePath / os.up
  def scalaVersion = "2.13.4"
  def organization = "com.github.lolgab"
  def ivyDeps = super.ivyDeps() ++ Agg(
    ivy"org.scalameta::munit::0.7.21",
    ivy"com.lihaoyi::upickle::1.2.3",
    ivy"com.lihaoyi::os-lib::0.7.2"
  )
  def publishVersion = "0.0.4"
  def pomSettings =
    PomSettings(
      description = "Snapshot testing for MUnit",
      organization = "com.github.lolgab",
      url = "https://github.com/lolgab/munit-snapshot",
      licenses = Seq(License.MIT),
      scm = SCM(
        "git://github.com/lolgab/munit-snapshot.git",
        "scm:git://github.com/lolgab/munit-snapshot.git"
      ),
      developers = Seq(
        Developer("lolgab", "Lorenzo Gabriele", "https://github.com/lolgab")
      )
    )
  trait CommonTest extends Tests {
    def testFrameworks = Seq("munit.Framework")
    def jsonFiles = T.input {
      os.walk(millSourcePath)
        .filter(f => os.isFile(f) && f.ext == "json")
        .map(PathRef(_))
    }
    def compile = T {
      jsonFiles()
      super.compile()
    }
  }

  def scalacOptions = Seq("-Ywarn-unused")
  def scalafixIvyDeps = Agg(ivy"com.github.liancheng::organize-imports:0.4.4")
}

object `munit-snapshot` extends Module {
  object jvm extends Common {
    object test extends CommonTest with Tests
  }
  object native extends ScalaNativeModule with Common {
    def scalaNativeVersion = "0.4.0"
    object test extends CommonTest with Tests
  }
}
