package com.hindog.spark.jdbc.catalog

import java.sql.{Connection, ResultSet, ResultSetMetaData}

import com.hindog.spark.jdbc.SparkConnection
import org.apache.spark.sql.Row
import org.apache.spark.sql.catalyst.expressions.{GenericRow, GenericRowWithSchema}
import org.apache.spark.sql.types.{StringType, StructField, StructType}

class SchemaMetadata(catalog: String, schemaPattern: String, conn: Connection) extends AbstractMetadata(conn) {

  override def fetch(): List[Row] = {
    if (isDebugEnabled) {
      log.info(s"Fetching Schema Metadata for catalog: $catalog, schemaPattern: $schemaPattern")
    }
    withStatement { stmt =>
      val rs = stmt.executeQuery("show databases")
      extractResult(rs)(getRow)
    }
  }


  override def sort(rows: List[Row]): List[Row] = rows.sortBy { row => (
    row.getAs[String]("TABLE_CATALOG"),
    row.getAs[String]("TABLE_SCHEM")
  )}

  override def schema: StructType = StructType(Seq(
    StructField("TABLE_SCHEM", StringType),
    StructField("TABLE_CATALOG", StringType, true)
  ))

  override def filter(row: Row): Boolean = filterColumn(row.get(0), schemaPattern) && filterColumn(row.get(1), catalog)

  override def getRow(rs: ResultSet): Row = {
    new GenericRowWithSchema(Array[Any](rs.getString("databaseName"), CatalogMetadata.catalogName), schema)
  }

}
