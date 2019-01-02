package prog

import org.apache.spark.sql._
import com.mongodb.spark.config._
import com.typesafe.config.ConfigFactory


object MainClass extends App{

  val conf = ConfigFactory.load()
  val sparkSession = SparkSession.builder()
                          .master(conf.getString("app.spark.master"))
                          .appName("SparkMongoExtract")
                          .config("spark.mongodb.input.uri", conf.getString("app.spark.mongo"))
                          .getOrCreate()

  val mongo = new SparkMongoExtract(sparkSession);
  val sparkSQL = new SparkSQLTransform(sparkSession);
  val influxDB = new InfluxDBLoad(
    conf.getString("app.influxDB.url"),
    conf.getInt("app.influxDB.port"),
    conf.getString("app.influxDB.dbName"),
    conf.getString("app.influxDB.sinceDate")
  );

  // SparkMongoExtract
  mongo.extract()

  // SparkSQLTransform
  val transformation: Dataset[Row] = sparkSQL.transform();

  // InfluxDBLoad
  influxDB.load(transformation)

  sparkSession.close()
}
