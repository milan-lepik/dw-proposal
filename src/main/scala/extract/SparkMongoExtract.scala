package prog

import com.mongodb.spark.MongoSpark
import org.apache.spark.sql.SparkSession
import com.mongodb.spark.NotNothing
import org.apache.log4j._ //Logger
import com.mongodb.spark.config._
import scala.reflect.runtime.universe.TypeTag

class SparkMongoExtract(sparkSession: SparkSession) {
  def extract() = {
    Logger.getLogger("org").setLevel(Level.ERROR)
    
    useCollection[Component]("component", sparkSession)
    useCollection[Institution]("institution", sparkSession)
    useCollection[ComponentType]("componentType", sparkSession)
  }
  
  private def useCollection[D <: Product: TypeTag: NotNothing](collectionName: String, sparkSession: SparkSession, printCollectionInfo: Boolean = false) = {
    val readConfig = ReadConfig(Map("collection" -> collectionName), Some(ReadConfig(sparkSession)))
    val collection = MongoSpark.load[D](sparkSession, readConfig)
    collection.createOrReplaceTempView(collectionName)
    
    if (printCollectionInfo) {
      println("---------------- Collection: "+collectionName+" ---------------------")
      println("Number of documents in "+collectionName+" collection: " + collection.count())
      println("Structure of documents in "+collectionName)
      println(collection.printSchema())
    }
  }
}


case class AggStructure(
    project: String, 
    subproject: String, 
    name: String, 
    stage: String,
    var count: Int
)

// https://docs.mongodb.com/spark-connector/master/scala/datasets-and-sql/
case class Id(oid: String)
case class Comment(code: String, dateTime: String, userIdentity: String, comment: String)
case class Attachment(dateTime: String, title: String, description: String, userIdentity: String, code: String, contentType: String, filename: String)
case class Stage(code: String, dateTime: String)
case class ComponentTypeStage(code: String, name: String, order: Int, initial: Boolean, `final`: Boolean)
case class ComponentTypeType(
    code: String, 
    name: String, 
    version:String, 
    subprojects:Array[String],
    snComponentIdentifier:String, 
    existing:Boolean, 
    cddNumber:String
)

case class Sys(cts: java.sql.Timestamp, mts: java.sql.Timestamp, rev: Int)
case class ComponentType(
    _id : Id, 
    code: String,
    name: String,
    state: String,
    project: String,
    subprojects: Array[String],
    category: String,
    snRequired: Boolean,
    snComponentIdentifier: String,
    //properties
    types: Array[ComponentTypeType],
    stages: Array[Stage],
    testTypes: Array[String], 
    awid: String, 
    sys: Sys, 
    snAutomatically: Boolean
)
case class Component(
  _id: Id,
  assembled: Boolean, //flag
  attachments: Array[Attachment],
  awid: String,
  code: String,
  comments: Array[Comment],
  completed: Boolean, //flag
  componentType: String,
  //currentGrade: null (nullable = true)
  currentLocation: String,  
  stages: Array[Stage],
  currentStage: String, // faze zivotniho cyklu
  dummy: Boolean,
  //grade: null (nullable = true)
  institution: String,
  project: String,
  //qaPassed: Boolean
  //qaState: null (nullable = true)
  reworked: Boolean, //flag
  serialNumber: String,
  state: String,
  subproject: String,
  sys: Sys,
  trashed: Boolean, //flag, nepovedla se vyrobit, nebo completed a trashed znamena ze prosla QA a pak byla znicena
  `type`: String,
  userIdentity: String
)

case class Institution(_id: Id, code: String, name: String)