package com.hindog.spark.jdbc.catalog

import java.nio.ByteBuffer
import java.sql.{SQLException, Types}
import org.apache.spark.sql.types._

case class JdbcType(name: String, sqlType: Int, precision: Int, scale: Int, clazz: Class[_]) {
  def toSparkType: DataType = DataType.fromDDL(name)
}

object JdbcType {
  def apply(typeName: String, sqlType: Int, precision: Int = 0, scale: Int = 0): JdbcType = {
    val dt = sqlType match {
      case Types.INTEGER => IntegerType
      case Types.BIGINT => LongType
      case Types.DOUBLE => DoubleType
      case Types.FLOAT => FloatType
      case Types.SMALLINT => ShortType
      case Types.TINYINT => ByteType
      case Types.BIT => BooleanType
      case Types.VARCHAR => StringType
      case Types.CLOB => StringType
      case Types.BLOB => BinaryType
      case Types.TIMESTAMP => TimestampType
      case Types.DATE => DateType
      case Types.DECIMAL => DecimalType(precision, scale)
      case _ => DataType.fromDDL(typeName)
    }
    JdbcType(dt)
  }

  def apply(dt: DataType): JdbcType = {
    dt match {
      case IntegerType => JdbcType(dt.catalogString, java.sql.Types.INTEGER, 0, 0, classOf[java.lang.Integer])
      case LongType => JdbcType(dt.catalogString, java.sql.Types.BIGINT, 0, 0, classOf[java.math.BigInteger])
      case DoubleType => JdbcType(dt.catalogString, java.sql.Types.DOUBLE, 0, 0, classOf[java.lang.Double])
      case FloatType => JdbcType(dt.catalogString, java.sql.Types.FLOAT, 0, 0, classOf[java.lang.Float])
      case ShortType => JdbcType(dt.catalogString, java.sql.Types.SMALLINT, 0, 0, classOf[java.lang.Short])
      case ByteType => JdbcType(dt.catalogString, java.sql.Types.TINYINT, 0, 0, classOf[java.lang.Byte])
      case BooleanType => JdbcType(dt.catalogString, java.sql.Types.BIT, 0, 0, classOf[java.lang.Integer])
      case StringType => JdbcType(dt.catalogString, java.sql.Types.VARCHAR, 0, 0, classOf[java.lang.String])
      case BinaryType => JdbcType(dt.catalogString, java.sql.Types.BLOB, 0, 0, classOf[ByteBuffer])
      case TimestampType => JdbcType(dt.catalogString, java.sql.Types.TIMESTAMP, 0, 0, classOf[java.sql.Timestamp])
      case DateType => JdbcType(dt.catalogString, java.sql.Types.DATE, 0, 0, classOf[java.sql.Date])
      case t: DecimalType => JdbcType(dt.catalogString, java.sql.Types.DECIMAL, t.precision, t.scale, classOf[java.math.BigDecimal])
      case ArrayType(elementType, containsNull) => JdbcType(dt.catalogString, java.sql.Types.ARRAY, 0, 0, classOf[java.util.List[_]])
      case MapType(keyType, valueType, containsNull) => JdbcType(dt.catalogString, java.sql.Types.STRUCT, 0, 0, classOf[java.util.Map[_, _]])
      case StructType(fields) => JdbcType(dt.catalogString, java.sql.Types.ARRAY, 0, 0, classOf[java.util.List[_]])
      case _ => throw new SQLException(s"Data type not implemented: ${dt.catalogString}")
    }
  }
}
