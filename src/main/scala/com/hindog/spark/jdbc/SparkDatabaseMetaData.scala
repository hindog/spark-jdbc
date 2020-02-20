package com.hindog.spark.jdbc

import java.sql.ResultSet

import com.hindog.spark.jdbc.catalog.{CatalogMetadata, ColumnMetadata, FunctionMetadata, SchemaMetadata, TableMetadata}
import org.apache.hive.jdbc.HiveDatabaseMetaData
import org.apache.hive.service.cli.thrift.{TCLIService, TSessionHandle}

class SparkDatabaseMetaData(conn: SparkConnection, client: TCLIService.Iface, session: TSessionHandle)
extends HiveDatabaseMetaData(conn, client, session) {

  override def getCatalogSeparator: String = super.getCatalogSeparator

  override def getCatalogTerm: String = super.getCatalogTerm

  override def getCatalogs: ResultSet = new CatalogMetadata(conn)
  override def getSchemas: ResultSet = new SchemaMetadata(null, null, conn)
  override def getSchemas(catalog: String, schemaPattern: String): ResultSet = new SchemaMetadata(catalog, schemaPattern, conn)
  override def getTables(catalog: String, schemaPattern: String, tableNamePattern: String, types: Array[String]): ResultSet = new TableMetadata(catalog, schemaPattern, tableNamePattern, types, conn)
  override def getColumns(catalog: String, schemaPattern: String, tableNamePattern: String, columnNamePattern: String): ResultSet = new ColumnMetadata(catalog, schemaPattern, tableNamePattern, columnNamePattern, conn)
  override def getFunctions(catalogName: String, schemaPattern: String, functionNamePattern: String): ResultSet = new FunctionMetadata(catalogName, schemaPattern, functionNamePattern, conn)
}
