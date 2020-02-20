package com.hindog.spark.jdbc

import java.net.URL
import scala.collection.JavaConverters._
import org.jsoup.Jsoup

object ExtractSparkFunctions extends App {
  val sparkVersion = "2.4.4"
  val url = s"https://spark.apache.org/docs/${sparkVersion}/api/sql/"
  val doc = Jsoup.parse(new URL(url), 60000)
  val main = doc.select("div[role=main]")
  val elements = main.select("h3, h3+p")

  elements.asScala.grouped(2).foreach { func =>
    val name = func(0).text()
    val desc = func(1).text()
    if (desc.trim.nonEmpty) {
      println(s"$name\t$desc")
    }
  }
}
