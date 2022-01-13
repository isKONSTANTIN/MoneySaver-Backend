name := "MoneySaver"

version := "0.1"

scalaVersion := "2.13.7"

val AkkaVersion = "2.6.8"
val AkkaHttpVersion = "10.2.7"

enablePlugins(JooqCodegenPlugin)

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", "services", "java.sql.Driver") => MergeStrategy.concat
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case PathList("reference.conf") => MergeStrategy.concat
  case x => MergeStrategy.first
}

Compile / discoveredMainClasses in Compile := Seq("su.knst.moneysaver.Main")

sources in (Compile,doc) := Seq.empty
publishArtifact in (Compile, packageDoc) := false

jooqCodegenConfig := file("jooq-codegen.xml")

libraryDependencies ++= Seq(
  "ch.megard" %% "akka-http-cors" % "1.1.2",
  "ch.qos.logback" % "logback-classic" % "1.3.0-alpha10",
  "org.apache.logging.log4j" % "log4j-to-slf4j" % "2.14.1",
  "com.github.tototoshi" %% "scala-csv" % "1.3.10",
  "org.postgresql" % "postgresql" % "42.3.1" % JooqCodegen,
  "org.postgresql" % "postgresql" % "42.3.1",
  "org.scalaj" %% "scalaj-http" % "2.4.2",
  "org.jooq" % "jooq" % "3.15.5",
  "org.mindrot" % "jbcrypt" % "0.3m",
  "org.jooq" % "jooq-codegen-maven" % "3.15.4",
  "org.jooq" % "jooq-meta" % "3.15.5",
  "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
  "com.typesafe.akka" %% "akka-http" % AkkaHttpVersion,
  "com.google.code.gson" % "gson" % "2.8.9",
  "com.google.inject" % "guice" % "3.0"
)