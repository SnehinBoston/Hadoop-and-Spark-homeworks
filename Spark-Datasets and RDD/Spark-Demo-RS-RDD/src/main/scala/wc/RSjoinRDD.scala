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


object RSjoinRDD {
  
  def main(args: Array[String]) {
    val logger: org.apache.log4j.Logger = LogManager.getRootLogger
    if (args.length != 2) {
      logger.error("Usage:\nwc.WordCountMain <input dir> <output dir>")
      System.exit(1)
    }
    val conf = new SparkConf().setAppName("RSjoinRDD").setMaster("local")
    val sc = new SparkContext(conf)


    val textFile = sc.textFile(args(0))
    val edges1 = textFile.map(line => (line.split(",")(0).toInt,line.split(",")(1).toInt)).filter(row_vals => row_vals._1 <= 10000 & row_vals._2 <= 10000)

    // Reverse the edges and filter out users greater than MAX
    val rev_edges = textFile.map(line => (line.split(",")(1).toInt,line.split(",")(0).toInt)).filter(row_vals => row_vals._1 <= 10000 & row_vals._2 <= 10000)

    //  Create RDD with the relationship pair as key and 1 as value
    val native_edges = textFile.map(line => (line.split(",")(0).toInt,line.split(",")(1).toInt)).filter(row => row._1 <= 10000 & row._2 <= 10000).map(row1 => ((row1._1, row1._2),1))

    // Join edges1 to rev_edges on key.
    val pathOf2 = edges1.join(rev_edges).map(row => ((row._2._1, row._2._2),1))
    //Count the  triangles.
    val countOfTriangles = pathOf2.join(native_edges).count()
    val RS_output = sc.parallelize(Seq("No of Triangles", countOfTriangles / 3))
    RS_output.saveAsTextFile(args(1))


  }
}