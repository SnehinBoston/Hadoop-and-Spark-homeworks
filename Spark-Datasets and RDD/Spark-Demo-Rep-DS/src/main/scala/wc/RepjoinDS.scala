package wc

import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.log4j.LogManager
import org.apache.log4j.Level
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.types.StructType
import org.apache.spark.sql.types.StructField
import org.apache.spark.sql.types.StringType
import org.apache.spark.sql.functions._


object RepjoinDS {
  
  def main(args: Array[String]) {
    val logger: org.apache.log4j.Logger = LogManager.getRootLogger
    if (args.length != 2) {
      logger.error("Usage:\nwc.WordCountMain <input dir> <output dir>")
      System.exit(1)
    }
    val conf = new SparkConf().setAppName("RepjoinDS")
    val sc = new SparkContext(conf)


    val textFile = sc.textFile(args(0))
    val ss = SparkSession
      .builder()
      .appName("RS join DataSet")
      .getOrCreate()

    import ss.implicits._

    // Get the key-value pair, filter out users greater than 10000 and convert the resultant RDD to dataset
    val edgesDataSet = textFile.map(line => (line.split(",")(0).toInt, line.split(",")(1).toInt)).filter(row => row._1 <= 10000 & row._2 <= 10000).toDS()

    val broadCastDataSet = broadcast(edgesDataSet.as("broadcastEdges"))


    // Join edgesDataSet to itself to retrieve all pathOf2 pairs
    val pathOf2 = edgesDataSet.as("Edges1").join(broadcast(broadCastDataSet))
      .where($"Edges1._2" === $"broadcastEdges._1")
      .select($"Edges1._1", $"broadcastEdges._2")

    // Join pathOf2 DS to edgesDataSet to close the triangle and get the count
    val noOfTriangles = pathOf2.as("Edges3").join(broadcast(broadCastDataSet))
      .where($"Edges3._1" === $"broadcastEdges._2" && $"Edges3._2" === $"broadcastEdges._1")
      .count()

    // Save the result in the output file
    sc.parallelize(Seq("No of Triangles", noOfTriangles / 3)).saveAsTextFile(args(1))

  }
}