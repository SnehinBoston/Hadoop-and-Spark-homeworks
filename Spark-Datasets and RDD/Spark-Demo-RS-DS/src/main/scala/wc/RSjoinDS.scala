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


object RSjoinDS {
  
  def main(args: Array[String]) {
    val logger: org.apache.log4j.Logger = LogManager.getRootLogger
    if (args.length != 2) {
      logger.error("Usage:\nwc.WordCountMain <input dir> <output dir>")
      System.exit(1)
    }
    val conf = new SparkConf().setAppName("RSjoinDS")
    val sc = new SparkContext(conf)

		// ================
    val ss = SparkSession
      .builder()
      .appName("TriangleCount RS join DS")
      .getOrCreate()

    val textFile = sc.textFile(args(0))
    import ss.implicits._

    val edgesDataSet = textFile.map(line => (line.split(",")(0).toInt,line.split(",")(1).toInt)).filter(row_vals => row_vals._1 <= 10000 & row_vals._2 <= 10000).toDS()

    // Join edgesDataSet to itself to retrieve all path pairs
    val pathOf2 = edgesDataSet.as("Edges1").join(edgesDataSet.as("Edges2"))
      .where($"Edges1._2" === $"Edges2._1")
      .select($"Edges1._1", $"Edges2._2")

    // Join path2 DS to edgesDS to close the triangle and get the count
    val noOfTriangles = pathOf2.as("Edges3").join(edgesDataSet.as("Edges1"))
      .where($"Edges3._1" === $"Edges1._2" && $"Edges3._2" === $"Edges1._1")
      .count()

    // Save result to args(1)
    sc.parallelize(Seq("No of Triangles", noOfTriangles / 3)).saveAsTextFile(args(1))

  }
}