# Data warehouse proposal
This project contains code and setup guide of the data warehouse solution. In this proposal I use Scala & Apache Spark to extract and transform data from MongoDB and then save it to InfluxDB. Data in InfluxDB are then visualised in Grafana Dashboard.

## Install to run local env
```
brew install apache-spark
brew install mongodb
brew install chronograf
brew install influxdb
brew install grafana
brew install sbt@1
```

## Restore mongo-dump to local mongo
```
mongorestore -d db --gzip ~/Downloads/w1dbisst70dn4seyb0fiyf7oq6tyx0h9
```

## Run ETL
```
sbt run
```

## Setup Grafana
 - Add data source InfluxDB http://localhost:8086
 - Import dashboard from json grafana-dashboard/components-stage-bi.json