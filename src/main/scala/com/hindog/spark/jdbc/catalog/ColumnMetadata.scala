package com.hindog.spark.jdbc.catalog

import java.sql.{Connection, DatabaseMetaData, ResultSet}

import org.apache.spark.sql.Row
import org.apache.spark.sql.catalyst.expressions.GenericRowWithSchema
import org.apache.spark.sql.types.{DataType, IntegerType, NumericType, ShortType, StringType, StructField, StructType}

import scala.collection._

class ColumnMetadata(catalog: String, schemaPattern: String, tablePattern: String, columnPattern: String, conn: Connection) extends AbstractMetadata(conn) {

  override def fetch(): List[Row] = {
    if (isDebugEnabled) {
      log.info(s"Fetching Table Metadata for catalog: $catalog, schemaPattern: $schemaPattern, tablePattern: $tablePattern, columnPattern: $columnPattern")
    }
    val rows = mutable.ListBuffer[Row]()
    val tables = new TableMetadata(catalog, schemaPattern, tablePattern, Array.empty, conn)
    withStatement { stmt =>
      while (tables.next()) {
        val table = tables.getString("TABLE_NAME")
        val tableSchema = tables.getString("TABLE_SCHEM")
        val columns = stmt.executeQuery(s"describe table $tableSchema.$table")
        var ordinal = 1

        while (columns.next()) {
          val jdbcType = JdbcType.apply(DataType.fromDDL(columns.getString("DATA_TYPE")))
          val sparkType = jdbcType.toSparkType
          val row = new GenericRowWithSchema(Array(
              tables.getString("TABLE_CAT"),
              tables.getString("TABLE_SCHEM"),
              tables.getString("TABLE_NAME"),
              columns.getString("col_name"),
              jdbcType.sqlType,
              jdbcType.name,
              sparkType.defaultSize,
              null,
              if (jdbcType.scale == 0) null else jdbcType.scale,
              if (sparkType.isInstanceOf[NumericType]) 10 else null,
              DatabaseMetaData.columnNullable,
              null,
              null,
              null,
              null,
              null,
              ordinal,
              "YES",
              null,
              null,
              null,
              null,
              "NO",
              "NO"
            ),
            schema
          )

          ordinal += 1
          rows += row
        }
      }
    }

    rows.toList
  }

  override def filter(row: Row): Boolean = filterColumn(row.getAs[String]("COLUMN_NAME"), columnPattern)

  override def sort(rows: List[Row]): List[Row] = rows.sortBy { row => (
    row.getAs[String]("TABLE_CAT"),
    row.getAs[String]("TABLE_SCHEM"),
    row.getAs[String]("TABLE_NAME"),
    row.getAs[Int]("ORDINAL_POSITION")
  )}

  override def schema: StructType = StructType(Seq(
    StructField("TABLE_CAT", StringType),
    StructField("TABLE_SCHEM", StringType),
    StructField("TABLE_NAME", StringType),
    StructField("COLUMN_NAME", StringType),
    StructField("DATA_TYPE", IntegerType),
    StructField("TYPE_NAME", StringType),
    StructField("COLUMN_SIZE", IntegerType),
    StructField("BUFFER_LENGTH", StringType),
    StructField("DECIMAL_DIGITS", IntegerType),
    StructField("NUM_PREC_RADIX", IntegerType),
    StructField("NULLABLE", IntegerType),
    StructField("REMARKS", StringType),
    StructField("COLUMN_DEF", StringType),
    StructField("SQL_DATA_TYPE", IntegerType),
    StructField("SQL_DATETIME_SUB", IntegerType),
    StructField("CHAR_OCTET_LENGTH", IntegerType),
    StructField("ORDINAL_POSITION", IntegerType),
    StructField("IS_NULLABLE", StringType),
    StructField("SCOPE_CATALOG", StringType),
    StructField("SCOPE_SCHEMA", StringType),
    StructField("SCOPE_TABLE", StringType),
    StructField("SOURCE_DATA_TYPE", ShortType),
    StructField("IS_AUTOINCREMENT", StringType),
    StructField("IS_GENERATEDCOLUMN", StringType)

  ))

  override def getRow(rs: ResultSet): Row = ???
}
