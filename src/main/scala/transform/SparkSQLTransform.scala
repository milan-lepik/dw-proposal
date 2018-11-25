package prog

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.Row
import org.apache.spark.sql.Dataset
import org.apache.spark.sql._
import org.apache.spark.sql.functions._

class SparkSQLTransform(sparkSession:SparkSession) {

  def transform():Dataset[Row] = {
    return sparkSession.sql("""
        SELECT
          component._id.oid as id,
          component.project,
          component.subproject,
          componentType.name,
          componentType.code,
          componentType.stages as allStages,
          component.currentStage,
          component.stages
        FROM component 
        JOIN institution ON (component.institution = institution._id.oid)
        JOIN componentType ON (component.componentType = componentType._id.oid)  
        WHERE trashed = false AND dummy = false
    """).select(
      col("id"),
      col("project"),
      col("subproject"),
      col("name"),
      col("code"),
      explode(col("stages")).as("stage")
    ).select(
      col("*"),
      col("stage.code").as("stage_code"),
      col("stage.dateTime")
    ).drop(
      col("stage")
    ).select(
      col("*")
    ).sort(
      asc("dateTime")
    )
  }
}