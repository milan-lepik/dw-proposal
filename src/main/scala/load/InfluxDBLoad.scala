package prog

import scala.concurrent.{Await, Future, future}
import org.apache.spark.sql.Row
import org.apache.spark.sql.Dataset
import scala.collection.mutable._
import scala.concurrent.duration._
import com.typesafe.config.ConfigFactory
import scala.concurrent.ExecutionContext.Implicits.global
import com.paulgoldbaum.influxdbclient._
import java.time._
import java.time.format.DateTimeFormatter

case class AggStructure(
    project: String, 
    subproject: String, 
    name: String, 
    stage: String,
    iname: String, 
    var count: Int
)

class InfluxDBLoad(url:String, port:Int, dbName:String, sinceDate:String) {
  val DayGranularityConstant = 86400000;
  val InfluxDBTimeoutConstant = 10.second;
  val AnalysisStartConstant = Instant.from(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(sinceDate)).toEpochMilli();
  val AnalysisEndConstant = System.currentTimeMillis() 
  val database = InfluxDB.connect(url, port).selectDatabase(dbName)
  
  def load(rdd:Dataset[Row]) = {
    var itemsById: Map[String, ComponentStructure] = Map()
    var data: ArrayBuffer[ComponentStructure] = ArrayBuffer()

    rdd.collect().foreach(item => {
      data += ComponentStructure(
            Instant.from(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(item.getAs[String]("dateTime"))).toEpochMilli(),
            if (item.getAs[String]("project") != null) item.getAs[String]("project") else "empty",
            if (item.getAs[String]("subproject") != null) item.getAs[String]("subproject") else "empty",
            if (item.getAs[String]("name") != null) item.getAs[String]("name") else "empty",
            if (item.getAs[String]("iname") != null) item.getAs[String]("iname") else "empty",
            if (item.getAs[String]("stage_code") != null) item.getAs[String]("stage_code") else "empty",
            if (item.getAs[String]("id") != null) item.getAs[String]("id") else "empty"
      )
    })

    if (!Await.result(database.exists(), InfluxDBTimeoutConstant)) {
      Await.result(database.create(), InfluxDBTimeoutConstant)
    }

    // non-optimalized way how to populate influxDB with O(n^2) complexity
    var tmsMilis = AnalysisStartConstant
    while(tmsMilis < AnalysisEndConstant){
      // 1) data are already sorted ASC by date => so extract the last known stage for each item ID
      var group: Map[String, Map[String, String]] = Map();
      data.foreach((item) => {
        if (item.time < tmsMilis + DayGranularityConstant) {
          val key = item.project + "_" +  item.subproject + "_" +  item.name + "_" +  item.iname + "_" +  item.id
          val value = Map(
            "project" -> item.project, 
            "subproject" -> item.subproject, 
            "name" -> item.name, 
            "stage" -> item.stage, 
            "iname" -> item.iname
          )
          // use the last known stage for this item
          group(key) = value;
        }
      })


      // 2) aggregate the last known stage
      var aggr: Map[String, AggStructure] = Map();
      group.values.foreach(itemLastStage => {
          val key = itemLastStage("project") + "_" +  
            itemLastStage("subproject") + "_" +  
            itemLastStage("name") + "_" +  
            itemLastStage("stage") + "_" + 
            itemLastStage("iname")
          if (aggr.contains(key)) {
            aggr(key).count += 1;
          } else {
            aggr(key) = AggStructure(
              itemLastStage("project"),
              itemLastStage("subproject"),
              itemLastStage("name"), 
              itemLastStage("stage"), 
              itemLastStage("iname"), 
            1);
          }
      })

      // 3) write points to InfluxDB
      aggr.values.foreach(send => {
          val point = Point("component", tmsMilis)
            .addTag("project", send.project)
            .addTag("subproject", send.subproject)
            .addTag("name", send.name)
            .addTag("stage", send.stage)
            .addTag("institution", send.iname)
            .addField("value", send.count)    
          Await.result(
            database.write(point, Parameter.Precision.MILLISECONDS).recover{ case e: WriteException => println(e.getMessage())}
            , InfluxDBTimeoutConstant
            )
        })
      
      tmsMilis += DayGranularityConstant
    }
    database.close()
  }
}

case class ComponentStructure(
    time: Long, 
    project: String, 
    subproject: String, 
    name: String, 
    iname: String, 
    stage: String,
    id: String
)