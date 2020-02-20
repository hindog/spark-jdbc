package com.hindog.spark.jdbc

import java.sql.{Connection, Driver, DriverManager, DriverPropertyInfo, SQLFeatureNotSupportedException}
import java.util.Properties
import java.util.concurrent.atomic.AtomicBoolean
import java.util.logging.Logger

import org.apache.hive.jdbc.HiveDriver

class SparkDriver extends Driver {
  SparkDriver.register()

  private val hiveDriver = new HiveDriver()

  override def connect(url: String, info: Properties): Connection =
    new SparkConnection(convertToHiveUrl(url), info)

  override def acceptsURL(url: String): Boolean = {
    url.startsWith("jdbc:spark://") || url.startsWith("jdbc:hive2://")
  }

  override def getPropertyInfo(url: String, info: Properties): Array[DriverPropertyInfo] =
    hiveDriver.getPropertyInfo(convertToHiveUrl(url), info)

  override def getMajorVersion: Int = org.apache.spark.SPARK_VERSION.split('.').head.toInt

  override def getMinorVersion: Int = org.apache.spark.SPARK_VERSION.split('.')(1).toInt

  override def jdbcCompliant(): Boolean = false

  override def getParentLogger: Logger = throw new SQLFeatureNotSupportedException("Method not supported")

  private def convertToHiveUrl(url: String): String =
    url.replace("jdbc:spark://", "jdbc:hive2://")
}

object SparkDriver {
  private lazy val registered = new AtomicBoolean(false)
  def register(): Unit = {
    if (!registered.get()) {
      registered.set(true)
      DriverManager.registerDriver(new SparkDriver())
    }
  }

}