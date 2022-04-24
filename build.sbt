lazy val baseName       = "Rogues"
lazy val baseNameL      = baseName.toLowerCase
lazy val projectVersion = "0.1.0-SNAPSHOT"

lazy val buildInfoSettings = Seq(
  // ---- build info ----
  buildInfoKeys := Seq(name, organization, version, scalaVersion, description,
    BuildInfoKey.map(homepage) { case (k, opt)           => k -> opt.get },
    BuildInfoKey.map(licenses) { case (_, Seq((lic, _))) => "license" -> lic }
  ),
  buildInfoOptions += BuildInfoOption.BuildTime
)

lazy val commonSettings = Seq(
  version      := projectVersion,
  homepage     := Some(url(s"https://github.com/Sciss/$baseName")),
  scalaVersion := "3.1.0", // "2.13.7",
  licenses     := Seq("AGPL v3+" -> url("http://www.gnu.org/licenses/agpl-3.0.txt")),
  run / fork   := true,
) ++ assemblySettings

lazy val root = project.in(file("."))
  .aggregate(common)
  .settings(commonSettings)
//  .settings(assemblySettings)
  .settings(
    name := baseName,
    description  := "An art piece",
  )

lazy val common = project.in(file("common"))
  .enablePlugins(BuildInfoPlugin)
  .settings(commonSettings)
  .settings(buildInfoSettings)
  .settings(
    name := s"$baseName-common",
    description := "Common code",
    libraryDependencies ++= Seq(
      "com.pi4j"      %  "pi4j-core"                % deps.common.pi4j,         // GPIO control
      "com.pi4j"      %  "pi4j-plugin-raspberrypi"  % deps.common.pi4j,         // GPIO control
      "com.pi4j"      %  "pi4j-plugin-pigpio"       % deps.common.pi4j,         // GPIO control
      "de.sciss"      %% "audiofile"                % deps.common.audioFile,    // record data as sound file
      "de.sciss"      %% "fileutil"                 % deps.common.fileUtil,     // utility functions
      "de.sciss"      %% "model"                    % deps.common.model,        // events
      "de.sciss"      %% "numbers"                  % deps.common.numbers,      // numeric utilities
      "de.sciss"      %% "scalaosc"                 % deps.common.osc,          // open sound control
      "de.sciss"      %% "swingplus"                % deps.common.swingPlus,    // user interface
      "net.harawata"  %  "appdirs"                  % deps.common.appDirs,      // finding standard directories
      "net.imagej"    %  "ij"                       % deps.common.imageJ,       // analyzing image data
      "org.rogach"    %% "scallop"                  % deps.common.scallop,      // command line option parsing
      "org.hid4java"  %  "hid4java"                 % deps.common.hid4java,     // USB HID access
      "com.fazecast"  %  "jSerialComm"              % deps.common.jSerialComm,  // Serial port reading
    ),
    resolvers += Resolver.sonatypeRepo("snapshots"),  // needed for hid4java
    buildInfoPackage := "de.sciss.rogues",
  )

lazy val deps = new {
  val common = new {
    val appDirs     = "1.2.1"
    val audioFile   = "2.4.0"
    val fileUtil    = "1.1.5"
    val hid4java    = "develop-20201104.172733-8" // stable: "0.7.0"
    val imageJ      = "1.53j" // "1.47h"
    val jSerialComm = "2.8.0"
    val model       = "0.3.5"
    val numbers     = "0.2.1"
    val osc         = "1.3.1"
    val pi4j        = "2.1.0"
    val scallop     = "4.1.0"
    val swingPlus   = "0.5.0"
  }
}

lazy val assemblySettings = Seq(
  // ---- assembly ----
  assembly / test            := {},
  assembly / target          := baseDirectory.value,
  ThisBuild / assemblyMergeStrategy := {
    case "logback.xml" => MergeStrategy.last
    case PathList("org", "xmlpull", _ @ _*)              => MergeStrategy.first
    case PathList("org", "w3c", "dom", "events", _ @ _*) => MergeStrategy.first // Apache Batik
    case p @ PathList(ps @ _*) if ps.last endsWith "module-info.class" =>
      println(s"DISCARD: $p")
      MergeStrategy.discard // Jackson, Pi4J
    case x =>
      val old = (ThisBuild / assemblyMergeStrategy).value
      old(x)
  },
//  assembly / fullClasspath := (Test / fullClasspath).value // https://github.com/sbt/sbt-assembly/issues/27
)
