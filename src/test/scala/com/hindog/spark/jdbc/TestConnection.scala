package com.hindog.spark.jdbc

import java.sql.DriverManager

import com.hindog.spark.jdbc.catalog.{CatalogMetadata, SchemaMetadata}

object TestConnection extends App {

  val conn = DriverManager.getConnection("jdbc:spark://localhost:10000/")

  //Debug.print(conn.getMetaData.getTables("Spark", null, null, Array.empty))
  Debug.print(new CatalogMetadata(conn))
  Debug.print(new SchemaMetadata(null, "%", conn))
//  Debug.print(new TableMetadata("Spark", null, null, Array.empty, conn))
//  Debug.print(new ColumnMetadata("Spark", null, "abilities", columnPattern = null, conn))
//  Debug.print(new FunctionMetadata("Spark", null, null, conn))

}
