package com.hindog.spark.jdbc.catalog

import org.apache.spark.sql.Row
import org.apache.spark.sql.catalyst.expressions.GenericRowWithSchema
import org.apache.spark.sql.types.{StringType, StructField, StructType}

import java.sql.{Connection, ResultSet}

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
  def catalogName: String = sys.props.getOrElse("com.hindog.spark.jdbc.catalog.name", "Spark")
}
