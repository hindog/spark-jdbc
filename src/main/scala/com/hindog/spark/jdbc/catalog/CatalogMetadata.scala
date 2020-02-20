package com.hindog.spark.jdbc.catalog

import java.sql.{Connection, ResultSet}

import com.hindog.spark.jdbc.SparkConnection
import org.apache.spark.sql.Row
import org.apache.spark.sql.catalyst.expressions.{GenericRow, GenericRowWithSchema}
import org.apache.spark.sql.types.{StringType, StructField, StructType}

class CatalogMetadata(conn: Connection) extends AbstractMetadata(conn) {
  override def fetch() = {
    if (isDebugEnabled) {
      log.info("Fetching Catalog Metadata")
    }
    withStatement { stmt =>
      val rs = stmt.executeQuery(s"select '${CatalogMetadata.catalogName}' as catalogName")
      extractResult(rs)(getRow)
    }
  }
  override def schema: StructType = StructType(Seq(StructField("TABLE_CAT", StringType, false)))
  override def getRow(rs: ResultSet): Row = new GenericRowWithSchema(Array[Any](rs.getString("catalogName")), schema)
}

object CatalogMetadata {
  def catalogName: String = "Spark"
}
