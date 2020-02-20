package com.hindog.spark.jdbc

import java.sql.{ResultSet, ResultSetMetaData}

import com.hindog.spark.jdbc.catalog.JdbcType
import org.apache.spark.sql.{DataFrame, Row, SparkSession}
import org.apache.spark.sql.catalyst.expressions.GenericRow
import org.apache.spark.sql.types.{MetadataBuilder, StructField, StructType}

import scala.collection.mutable

object Debug {

  private lazy val session = {
    val s = SparkSession.builder().config("spark.master", "local[2]").appName("JDBCDriver").getOrCreate()
    sys.addShutdownHook(if (!s.sparkContext.isStopped) s.stop())
    s
  }

  def printSchema(rs: ResultSet): Unit = {
    toDataFrame(rs).printSchema()
  }

  def print(rs: ResultSet): Unit = {
    toDataFrame(rs).show(10000, false)
  }

  def toDataFrame(rs: ResultSet): DataFrame = {
    import scala.collection.JavaConverters._
    val meta = rs.getMetaData
    val fields = (1 to meta.getColumnCount).map { col =>
      val tpe = JdbcType(meta.getColumnTypeName(col), meta.getColumnType(col), meta.getPrecision(col), meta.getScale(col))
      val metaData = new MetadataBuilder().putString("class", tpe.clazz.getName).build()
      meta.getColumnName(col) -> StructField(meta.getColumnName(col), tpe.toSparkType, meta.isNullable(col) == ResultSetMetaData.columnNullable, metaData)
    }

    val fieldMap = fields.toMap
    val rows = mutable.ListBuffer[Row]()
    while (rs.next()) {
      val values = (1 to meta.getColumnCount).map { col =>
        val colName = meta.getColumnName(col)
        rs.getObject(colName, Class.forName(fieldMap(colName).metadata.getString("class")))
      }
      rows += new GenericRow(values.toArray)
    }

    session.createDataFrame(rows.asJava, StructType(fields.map(_._2)))
  }
}
