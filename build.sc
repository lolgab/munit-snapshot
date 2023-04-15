import mill._, scalalib._, scalanativelib._, publish._
import $ivy.`com.lihaoyi::mill-contrib-bloop:`
import $ivy.`com.goyeau::mill-scalafix::0.2.11`
import com.goyeau.mill.scalafix.ScalafixModule

trait Common extends ScalaModule with PublishModule with ScalafixModule {
  def artifactName = "munit-snapshot"
  def millSourcePath = super.millSourcePath / os.up
  def scalaVersion = "2.13.10"
  def organization = "com.github.lolgab"
  def ivyDeps = super.ivyDeps() ++ Agg(
    ivy"org.scalameta::munit::1.0.0-M7",
    ivy"com.lihaoyi::upickle::3.1.0",
    ivy"com.lihaoyi::os-lib::0.9.1"
  )
  def publishVersion = "0.0.5-SNAPSHOT"
  def pomSettings =
    PomSettings(
      description = "Snapshot testing for MUnit",
      organization = "com.github.lolgab",
      url = "https://github.com/lolgab/munit-snapshot",
      licenses = Seq(License.MIT),
      versionControl = VersionControl.github("lolgab", "munit-snapshot"),
      developers = Seq(
        Developer("lolgab", "Lorenzo Gabriele", "https://github.com/lolgab")
      )
    )
  trait CommonTest extends Tests with TestModule.Munit {
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
  def scalafixIvyDeps = Agg(ivy"com.github.liancheng::organize-imports:0.6.0")
}

object `munit-snapshot` extends Module {
  object jvm extends Common {
    object test extends CommonTest with Tests
  }
  object native extends ScalaNativeModule with Common {
    def scalaNativeVersion = "0.4.12"
    object test extends CommonTest with Tests
  }
}
