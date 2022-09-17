package com.github.colinlouie.spark.delta

object Main {

  def main(args: Array[String]): Unit = {

    // warehouseLocation points to the default location for managed databases and tables.
    // This is for local testing. Your cluster will typically take care of this for you.
    val warehouseLocation = new java.io.File("spark-warehouse").getAbsolutePath

    val spark = org.apache.spark.sql.SparkSession
      .builder()
      /** The next two lines add Delta Lake support. */
      .config("spark.sql.catalog.spark_catalog", "org.apache.spark.sql.delta.catalog.DeltaCatalog")
      .config("spark.sql.extensions", "io.delta.sql.DeltaSparkSessionExtension")
      /** The next two lines prevent this:
        * {{{Exception in thread "main" org.apache.spark.sql.AnalysisException: Table or view not found}}}
        */
      .config("spark.sql.warehouse.dir", warehouseLocation)
      .enableHiveSupport()
      .getOrCreate()

    val sc = spark.sparkContext

    import spark.implicits._

    spark.sql("CREATE TABLE IF NOT EXISTS test_table(name STRING, age STRING) USING delta")

    val upsertQuery = """
MERGE INTO test_table                   -- this is the target table.
  USING to_upsert                       -- this is our source.
    ON test_table.name = to_upsert.name -- rows to match.
WHEN MATCHED THEN
  UPDATE SET
    age = to_upsert.age
WHEN NOT MATCHED THEN
  INSERT (
    name,
    age
  )
  VALUES (
    to_upsert.name,
    to_upsert.age
  )
"""

    // See what is in the table.
    spark.sql("SELECT * FROM test_table").show(truncate = false)

    // Create the row(s) of data to insert/update the table with.
    // The TempView allows us to use SparkSQL. We could have used DataFrames directly with Scala DSL too.
    // Verify the table's contents after the upsert.
    val firstUpsert = sc.parallelize(Seq(("hello", "1"))).toDF("name", "age")
    firstUpsert.createOrReplaceTempView("to_upsert")
    spark.sql(upsertQuery)
    spark.sql("SELECT * FROM test_table").show(truncate = false)

    val secondUpsert = sc.parallelize(Seq(("hello", "2"))).toDF("name", "age")
    secondUpsert.createOrReplaceTempView("to_upsert")
    spark.sql(upsertQuery)
    spark.sql("SELECT * FROM test_table").show(truncate = false)

  } // def main

} // object Main
