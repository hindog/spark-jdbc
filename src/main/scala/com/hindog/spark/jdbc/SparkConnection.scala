package com.hindog.spark.jdbc

import java.sql.{DatabaseMetaData, SQLException}
import java.util.Properties

import org.apache.hive.jdbc.HiveConnection
import org.apache.hive.service.cli.thrift.{TCLIService, TSessionHandle}

class SparkConnection(uri: String, info: Properties) extends HiveConnection(uri, info) {
  override def getCatalog: String = "Spark"

  override def getMetaData: DatabaseMetaData = {
    if (super.isClosed) {
      new SQLException("Connection is closed")
    }
    val hiveClass = classOf[HiveConnection]

    val client = hiveClass.getDeclaredField("client")
    client.setAccessible(true)
    val session = hiveClass.getDeclaredField("sessHandle")
    session.setAccessible(true)

    new SparkDatabaseMetaData(this, client.get(this).asInstanceOf[TCLIService.Iface], session.get(this).asInstanceOf[TSessionHandle])
  }

  /**
   * Override with no-op since DataGrip/IntelliJ call this method and Hive throws "Method not supported" Exception
   * @param readOnly
   */
  override def setReadOnly(readOnly: Boolean): Unit = {
  }
}
