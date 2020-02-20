name := "spark-jdbc"

version := "1.0.0"

scalaVersion := "2.11.12"

libraryDependencies += "org.spark-project.hive" % "hive-jdbc" % "1.2.1.spark2"
libraryDependencies += "org.apache.spark" %% "spark-hive" % "2.4.4"
libraryDependencies += "org.jsoup" % "jsoup" % "1.12.1"
libraryDependencies += "com.h2database" % "h2" % "1.4.200"
libraryDependencies += "com.google.code.gson" % "gson" % "2.8.6"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.1.0"

// Github Release
ghreleaseRepoOrg := "hindog"
ghreleaseAssets := Seq((assembly / assemblyOutputPath).value)


assembleArtifact := true
assemblyJarName := "spark-jdbc-" + version.value + ".jar"
mainClass in assembly := None
packageOptions in assembly ~= { pos =>
  pos.filterNot { po =>
    po.isInstanceOf[Package.MainClass]
  }
}

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", "MANIFEST.MF") => MergeStrategy.discard
  case PathList("META-INF", "LICENSE") => MergeStrategy.discard
  case PathList("META-INF", f) if f.endsWith(".DSA") => MergeStrategy.discard
  case PathList("META-INF", f) if f.endsWith(".SF") => MergeStrategy.discard
  case x => MergeStrategy.first
}
