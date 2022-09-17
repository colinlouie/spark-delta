# Apache Spark with Delta Lake support

Why am I doing this? I wanted to use Delta format when working locally.

What are my requirements? I am using Databricks on AWS.

I care about stability, so I am choosing to only use DBR LTS runtimes.

https://docs.databricks.com/release-notes/runtime/10.4.html

# Prerequisites

The following revolves around the DBR runtime, since I want the closest thing to the Production environment.

I am using an Intel MacBook Pro with macOS Monterey 12.5.1.

Assumptions:
- You know how to find (and download, then install) the prerequisites for your OS/architecture
- You know the JDK ecosystem
- You know the Scala ecosystem, including sbt
- You know Apache Spark
- You want to work with Delta tables
- You came here because uber JARs are hard

## The list

1. Azul Zulu JDK 8.56.0.21
1. jenv 0.5.5
1. sbt 1.7.1
1. Apache Spark 3.2.1

## Add Zulu JDK using jenv

```shell
$ jenv add /Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home/
zulu64-1.8.0.302 added
```

## Set the local version for our project

```shell
$ jenv versions | grep zulu
  zulu64-1.8.0.302

$ jenv local zulu64-1.8.0.302
```

## Verify we are running the wanted JDK

```shell
$ java -version
openjdk version "1.8.0_302"
OpenJDK Runtime Environment (Zulu 8.56.0.21-CA-macosx) (build 1.8.0_302-b08)
OpenJDK 64-Bit Server VM (Zulu 8.56.0.21-CA-macosx) (build 25.302-b08, mixed mode)
```

## Verify Apache Spark is also using the same JDK

If your version of Apache Spark, and Java do not match the below snippet, you need to fix that first. You _WILL_ run 
into issues if the Spark and Delta Lake versions are not compatible.

```shell
$ spark-shell
  :
Welcome to
      ____              __
     / __/__  ___ _____/ /__
    _\ \/ _ \/ _ `/ __/  '_/
   /___/ .__/\_,_/_/ /_/\_\   version 3.2.1
      /_/

Using Scala version 2.12.15 (OpenJDK 64-Bit Server VM, Java 1.8.0_302)
  :
scala>
```

# Delta Lake support

## Build uber JAR

```shell
$ sbt assembly
```

## Verify we can use Delta format in spark-shell

This tests that the uber JAR contains the dependencies for Delta Lake.

```shell
$ spark-shell \
  --conf "spark.sql.extensions=io.delta.sql.DeltaSparkSessionExtension" \
  --conf "spark.sql.catalog.spark_catalog=org.apache.spark.sql.delta.catalog.DeltaCatalog" \
  --jars target/scala-2.12/spark-delta-assembly-0.1.jar

scala> spark.sql("CREATE TABLE IF NOT EXISTS test_table(key STRING, value STRING) USING delta")
res0: org.apache.spark.sql.DataFrame = []
```

## Verify our Spark job via spark-submit

The Delta configuration items are baked into the code so we no longer have to specify it on the command line.

- `--class` specifies the class the Spark job should execute.
- `--conf "spark.driver.extraJavaOptions"` tells the driver to use the custom `log4j.properties` file.
- `--conf "spark.executor.extraJavaOptions"` tells the executors to use the custom `log4j.properties` file.
- `--files` takes a _local file_ (to spark-submit) and uploads it to the working directory for the driver, and 
  executors.

Note that you can use a different `log4j.properties` file for the driver, and the executors.

```shell
$ log4j_properties="$(pwd)/src/main/resources/log4j.properties"
$ spark-submit \
  --class com.github.colinlouie.spark.delta.Main \
  --conf "spark.driver.extraJavaOptions=-Dlog4j.configuration=file:${log4j_properties}" \
  --conf "spark.executor.extraJavaOptions=-Dlog4j.configuration=file:${log4j_properties}" \
  --files "${log4j_properties}" \
  target/scala-2.12/spark-delta-assembly-0.1.jar
```
