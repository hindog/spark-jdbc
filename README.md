# Spark JDBC Driver

Because the Spark 2.x ThriftServer doesn't implement the Hive JDBC Driver's Catalog API, when using Spark's version of the Hive JDBC driver (`org.spark-project.hive:hive-jdbc:1.2.1.spark2`)
within BI tools such as [DataGrip](https://www.jetbrains.com/datagrip/), the tools are unable to introspect the catalog schema (databases, tables, views, columns, etc). 

This driver fixes that by wrapping the default Spark Hive Driver (`org.spark-project.hive:hive-jdbc:1.2.1.spark2`) and adds introspection support by implementing the missing calls with calls
to Spark's [SHOW/DESCRIBE](https://docs.databricks.com/spark/latest/spark-sql/language-manual/index.html#describe-statements) commands.

### Download
 
Go to [Releases](https://github.com/hindog/spark-jdbc/releases) and download the latest JAR version.

### Configuration

Add the new Driver to your BI tool using the downloaded jar.

The driver class is `com.hindog.spark.jdbc.SparkDriver`.

JDBC url patterns are same as the [Hive Driver](https://cwiki.apache.org/confluence/display/Hive/HiveServer2+Clients#HiveServer2Clients-ConnectionURLs).  You can also use `spark` for the driver scheme, so the JDBC url `jdbc:hive2://<hostname>:<port>/` would be equivalent to `jdbc:spark://<hostname>:<port>/`

### Building

Run `sbt assembly:assembly` to build the driver jar.

The output jar will be written to `target/scala-2.11/spark-jdbc-<version>.jar` and it will include all the required dependencies and so it will be quite large.  

### Limitations

- Spark 2.x does not support multiple "catalogs", so a hard-coded catalog named `Spark` will be used.  You can override this by setting the system property `com.hindog.spark.jdbc.catalog.name` with the desired catalog name.
- Spark does not support column sizes, so the driver will return the value of the column's `defaultSize` value.
- Spark `DESCRIBE` & `SHOW` commands return a limited amount of metadata, so most JDBC metadata will not be populated.   
- Introspection can take some time to run as a separate SQL statement must be issued to introspect each database/table/column/function, etc. 

