addSbtPlugin("org.jetbrains" % "sbt-ide-settings" % "1.1.0")
addSbtPlugin("com.github.sbt" % "sbt-native-packager" % "1.9.4")
addSbtPlugin("com.github.kxbmap" % "sbt-jooq-codegen" % "0.7.0")

ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always