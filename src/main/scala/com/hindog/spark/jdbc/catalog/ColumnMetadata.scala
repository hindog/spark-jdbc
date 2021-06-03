package com.hindog.spark.jdbc.catalog

import java.sql.{Connection, DatabaseMetaData, ResultSet}
import org.apache.spark.sql.Row
import org.apache.spark.sql.catalyst.expressions.GenericRowWithSchema
import org.apache.spark.sql.types.{DataType, IntegerType, NumericType, ShortType, StringType, StructField, StructType}
import org.apache.spark.SparkException

import scala.collection._
import scala.util.control.NonFatal
import scala.util.matching.Regex

class ColumnMetadata(catalog: String, schemaPattern: String, tablePattern: String, columnPattern: String, conn: Connection) extends AbstractMetadata(conn) {

  // Used to fix column type DDL definitions when Spark truncates it due to excessive size.
  // For example, it will add "... 5 more fields" for very large struct types and then we are unable to parse column DDL.
  // This regex will remove that so that the type can be parsed, except without the additional columns.
  // This doesn't really impact anything on the JDBC side since we don't return the full type, only the "top-level" type of a colunn
  lazy val truncatedTypeRegex: Regex = "(.*),\\.\\.\\.\\s\\d+\\smore\\sfields(.*)".r

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

        while (columns.next() && !columns.getString("col_name").startsWith("#")) {
          try {
            val rawDataType = columns.getString("DATA_TYPE")

            // parse the Spark column DDL.  If the type was truncated due to size, we'll patch it up first
            val dataType = truncatedTypeRegex.findFirstMatchIn(rawDataType) match {
              case Some(m) => m.group(1) + m.group(2) // combine parts before and after the "... N more fields"
              case None => rawDataType
            }

            val jdbcType = JdbcType.apply(DataType.fromDDL(dataType))
            val sparkType = jdbcType.toSparkType

            // https://docs.oracle.com/javase/7/docs/api/java/sql/DatabaseMetaData.html#getColumns(java.lang.String,%20java.lang.String,%20java.lang.String,%20java.lang.String)
            val row = new GenericRowWithSchema(Array(
              tables.getString("TABLE_CAT"),
              tables.getString("TABLE_SCHEM"),
              tables.getString("TABLE_NAME"),
              columns.getString("col_name"),
              jdbcType.sqlType,
              rawDataType,
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
            ), schema)

            ordinal += 1
            rows += row
          } catch {
            case NonFatal(ex) => throw new SparkException(s"Error retrieving metadata for column: ${tables.getString("TABLE_SCHEM")}.${table}.${columns.getString("col_name")} with type: ${columns.getString("DATA_TYPE")}", ex)
          }
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
