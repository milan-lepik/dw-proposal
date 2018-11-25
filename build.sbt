name         := "DataWarehouse Project"
version      := "1.0"
organization := "cz.milan"

scalaVersion := "2.11.12"
mainClass in (Compile, run) := Some("prog.MainClass")

resolvers += Resolver.mavenLocal
resolvers += "Spark Packages Repo" at "http://dl.bintray.com/spark-packages/maven"

libraryDependencies ++= Seq(
  "org.mongodb.spark" %% "mongo-spark-connector" % "2.3.1",
  "org.apache.spark" %% "spark-core" % "2.3.2",
  "org.apache.spark" %% "spark-sql" % "2.3.2",
  "com.typesafe" % "config" % "1.3.2",
  "com.paulgoldbaum" %% "scala-influxdb-client" % "0.6.1",
  "mrpowers" % "spark-daria" % "0.26.1-s_2.11"
)
