name := "spark-delta"

version := "0.1"

/** Technically, only 2.12.x is significant, but I chose to match the exact version of the Apache Spark runtime. */
scalaVersion := "2.12.15"

lazy val sparkVersion = "3.2.1"

// Execute scalafmtCheck on compile.
Compile / compile := (Compile / compile).dependsOn(scalafmtCheckAll).value

// Automatically reload the build when source changes are detected.
Global / onChangedBuildSource := ReloadOnSourceChanges

assembly / assemblyMergeStrategy := {
  // Typically, we would discard duplicate META-INF entries, but in the case of Delta Lake,
  // META-INF/services/org.apache.spark.sql.sources.DataSourceRegister needs to be in the JAR.
  // This avoids the dreaded:
  //   Exception in thread "main" java.lang.ClassNotFoundException: Failed to find data source: delta.
  // Thank you https://github.com/Vishvin95 !
  // Reference: https://github.com/delta-io/delta/issues/224#issuecomment-826878424
  case PathList("META-INF", "services", xg @ _*) => MergeStrategy.concat
  case PathList("META-INF", xs @ _*) => MergeStrategy.discard
  case x => MergeStrategy.first
}

/** --------------------------------------- */
/** Required for operation of Apache Spark. */
/** --------------------------------------- */

// https://mvnrepository.com/artifact/org.apache.spark/spark-core
libraryDependencies += "org.apache.spark" %% "spark-core" % sparkVersion % Provided

// https://mvnrepository.com/artifact/org.apache.spark/spark-sql
libraryDependencies += "org.apache.spark" %% "spark-sql" % sparkVersion % Provided

/** ------------------------- */
/** Required for application. */
/** ------------------------- */

// https://mvnrepository.com/artifact/io.delta/delta-core
libraryDependencies += "io.delta" %% "delta-core" % "1.1.0"
