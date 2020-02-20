scalaVersion := "2.12.5"

resolvers += "sbt-github-release" at "https://dl.bintray.com/ohnosequences/sbt-plugins/"

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.14.10")
addSbtPlugin("ohnosequences" % "sbt-github-release" % "0.7.0")
