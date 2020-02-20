package com.hindog.spark.jdbc.catalog

import java.sql.{Connection, ResultSet}

import scala.collection._
import org.apache.spark.sql.Row
import org.apache.spark.sql.catalyst.expressions.{GenericRow, GenericRowWithSchema}
import org.apache.spark.sql.types.{StringType, StructField, StructType}

class TableMetadata(catalog: String, schemaPattern: String, tablePattern: String, types: Array[String], conn: Connection) extends AbstractMetadata(conn) {

  override def fetch(): List[Row] = {
    if (isDebugEnabled) {
      log.info(s"Fetching Table Metadata for catalog: $catalog, schemaPattern: $schemaPattern, tablePattern: $tablePattern, types: ${types.mkString(", ")}")
    }
    val rows = mutable.ListBuffer[Row]()
    val schemas = new SchemaMetadata(catalog, schemaPattern, conn)
    withStatement { stmt =>
      while (schemas.next()) {
        val schema = schemas.getString("TABLE_SCHEM")
        val tables = stmt.executeQuery(s"show tables from $schema")
        rows ++= extractResult(tables)(getRow)
      }
    }

    rows.toList
  }

  override def filter(row: Row): Boolean = {
    filterColumn(row.getAs[String]("TABLE_NAME"), tablePattern)
  }

  override def sort(rows: List[Row]): List[Row] = rows.sortBy { row => (
    row.getAs[String]("TABLE_TYPE"),
    row.getAs[String]("TABLE_CAT"),
    row.getAs[String]("TABLE_SCHEM"),
    row.getAs[String]("TABLE_NAME")
  )}

  override def schema: StructType = StructType(Seq(
    StructField("TABLE_CAT", StringType),
    StructField("TABLE_SCHEM", StringType),
    StructField("TABLE_NAME", StringType),
    StructField("TABLE_TYPE", StringType),
    StructField("REMARKS", StringType),
    StructField("TYPE_CAT", StringType),
    StructField("TYPE_SCHEM", StringType),
    StructField("TYPE_NAME", StringType),
    StructField("SELF_REFERENCING_COL_NAME", StringType),
    StructField("REF_GENERATION", StringType)
  ))

  override def getRow(rs: ResultSet): Row = {
    new GenericRowWithSchema(Array[Any](
      CatalogMetadata.catalogName,
      rs.getString("database"),
      rs.getString("tableName"),
      if (rs.getBoolean("isTemporary")) "GLOBAL TEMPORARY" else "TABLE",
      "",
      null,
      null,
      null,
      null,
      null
    ), schema)
  }
}
