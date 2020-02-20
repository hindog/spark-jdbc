package com.hindog.spark.jdbc.catalog

import java.sql.{Connection, DatabaseMetaData, ResultSet}

import org.apache.spark.sql.Row
import org.apache.spark.sql.catalyst.expressions.GenericRowWithSchema
import org.apache.spark.sql.types.{StringType, StructField, StructType}

import scala.collection._

class FunctionMetadata(catalog: String, schemaPattern: String, functionPattern: String, conn: Connection) extends AbstractMetadata(conn) {

  override def fetch(): List[Row] = {
    if (isDebugEnabled) {
      log.info(s"Fetching Function Metadata for catalog: $catalog, schemaPattern: $schemaPattern, functionPattern: $functionPattern")
    }
    val rows = mutable.ListBuffer[Row]()
    if (catalog == "" && schemaPattern ==  "") {
      withStatement { stmt =>
        val funcs = stmt.executeQuery(s"show functions")
        rows ++= extractResult(funcs)(getRow)
      }
    }

    rows.toList
  }

  override def filter(row: Row): Boolean = {
    filterColumn(row.getAs[String]("FUNCTION_NAME"), functionPattern)
  }

  override def sort(rows: List[Row]): List[Row] = rows.sortBy { row => (
    row.getAs[String]("FUNCTION_NAME")
  )}

  override def schema: StructType = StructType(Seq(
    StructField("FUNCTION_CAT", StringType),
    StructField("FUNCTION_SCHEM", StringType),
    StructField("FUNCTION_NAME", StringType),
    StructField("REMARKS", StringType),
    StructField("FUNCTION_TYPE", StringType),
    StructField("SPECIFIC_NAME", StringType)
  ))

  override def getRow(rs: ResultSet): Row = {
    val name = rs.getString("function")
    val usage = FunctionMetadata.lookup.getOrElse(name, withStatement { stmt =>
      val rs = stmt.executeQuery(s"describe function $name")
      var usage = ""
      while (rs.next()) {
        val desc = rs.getString("function_desc")
        if (desc.startsWith("Usage:")) {
          usage = desc.stripPrefix("Usage: ")
        }
      }
      usage
    })

    new GenericRowWithSchema(Array[Any](
      null,
      null,
      name,
      usage,
      DatabaseMetaData.functionNoTable,
      name
    ), schema)
  }
}

object FunctionMetadata {
  val lookup: Predef.Map[String, String] = {
    val src = scala.io.Source.fromURL(getClass.getResource("/functions.csv"))
    try {
      src.getLines().map(line => line.split('\t')).map(kv => kv(0) -> kv(1)).toMap
    } finally {
      src.close()
    }
  }
}