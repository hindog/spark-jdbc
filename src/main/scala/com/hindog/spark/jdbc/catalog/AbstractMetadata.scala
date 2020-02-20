package com.hindog.spark.jdbc.catalog

import java.sql.{Connection, ResultSet, ResultSetMetaData, Statement}

import com.hindog.spark.jdbc.Debug
import javax.sql.RowSetMetaData
import javax.sql.rowset.RowSetMetaDataImpl
import org.apache.spark.sql.Row
import org.apache.spark.sql.types.{DecimalType, StructType}
import org.h2.tools.SimpleResultSet
import org.slf4j.{Logger, LoggerFactory}

import scala.collection._
import scala.util.control.NonFatal

abstract class AbstractMetadata(conn: Connection) extends SimpleResultSet()  {

  protected val log: Logger = LoggerFactory.getLogger(getClass)

  addColumns()
  populate()

  def isDebugEnabled: Boolean = Set("1", "true").map(v => Option(v)).contains(sys.props.get("spark.jdbc.debug"))

  def fetch(): List[Row]
  def schema: StructType
  def filter(row: Row): Boolean = true
  def getRow(rs: ResultSet): Row
  def sort(rows: List[Row]): List[Row] = rows

  def populate(): Unit = {
    val stmt = conn.createStatement()
    try {
      val rows = fetch()
      sort(rows.filter(filter)).foreach(row => addRow(row.toSeq.toArray.asInstanceOf[Array[Object]]: _*))
    } finally {
      if (!stmt.isClosed) {
        stmt.close()
      }
    }

    if (isDebugEnabled) {
      try {
        Debug.print(this)
      } catch {
        case NonFatal(ex) => ex.printStackTrace()
      }
    }
  }

  def extractResult(rs: ResultSet)(f: ResultSet => Row): List[Row] = {
    val rows = mutable.ListBuffer[Row]()
    while (rs.next()) {
      rows += f(rs)
    }
    rows.toList
  }

  def filterColumn(value: Any, filter: String): Boolean = {
    if (filter != null) {
      if (filter.nonEmpty) {
        val regex = ("^" + filter.replace("%", ".*") + "$").r
        regex.findPrefixMatchOf(value.toString).isDefined
      } else value == "" || value == null
    } else true
  }

  def withStatement[T](f: Statement => T): T = {
    val stmt = conn.createStatement()
    try {
      f(stmt)
    } finally {
      if (!stmt.isClosed) {
        stmt.close()
      }
    }
  }

  def addColumns(): Unit = {
    (1 to metadata.getColumnCount).foreach { col =>
      addColumn(metadata.getColumnName(col), metadata.getColumnType(col), metadata.getColumnTypeName(col), metadata.getPrecision(col), metadata.getScale(col))
    }
  }

  lazy val metadata: RowSetMetaData = {
    val meta = new RowSetMetaDataImpl()
    meta.setColumnCount(schema.size)

    schema.zipWithIndex.map { case (field, idx) => (field, idx + 1) }.foreach { case (field, idx) =>
      val sqlType = JdbcType(field.dataType)

      meta.setAutoIncrement(idx, false)
      meta.setCaseSensitive(idx, true)
      meta.setColumnDisplaySize(idx, field.dataType.defaultSize)
      meta.setCurrency(idx, false)
      meta.setNullable(idx, if (field.nullable) ResultSetMetaData.columnNullable else ResultSetMetaData.columnNoNulls)
      field.dataType match {
        case v:DecimalType =>
          meta.setPrecision(idx, v.precision)
          meta.setScale(idx, v.scale)
        case other => // no-op
      }

      meta.setSearchable(idx, false)
      meta.setSigned(idx, false)
      meta.setColumnLabel(idx, field.name)
      meta.setColumnName(idx, field.name)
      meta.setColumnType(idx, sqlType.sqlType)
      meta.setColumnTypeName(idx, field.dataType.catalogString)
    }

    meta
  }

  override def getMetaData: ResultSetMetaData = metadata
}
