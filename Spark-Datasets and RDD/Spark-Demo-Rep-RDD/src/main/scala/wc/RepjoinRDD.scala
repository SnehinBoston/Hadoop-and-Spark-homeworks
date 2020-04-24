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


object RepjoinRDD {
  
  def main(args: Array[String]) {
    val logger: org.apache.log4j.Logger = LogManager.getRootLogger
    if (args.length != 2) {
      logger.error("Usage:\nwc.WordCountMain <input dir> <output dir>")
      System.exit(1)
    }
    val conf = new SparkConf().setAppName("RepjoinRDD")
    val sc = new SparkContext(conf)

		// ================
    
    val textFile = sc.textFile(args(0))
    val edges1 = textFile.map(line => (line.split(",")(0).toInt, line.split(",")(1).toInt)).filter(row => row._1 <= 50000 & row._2 <= 50000)

    // Convert each line of csv to pair, filter out users greater than MAX
    val revEdges = textFile.map(line => (line.split(",")(1).toInt, line.split(",")(0).toInt)).filter(row => row._1 <= 50000 & row._2 <= 50000)


    val smallRDDLocal = edges1.collectAsMap()
    sc.broadcast(smallRDDLocal)

    // Join edges1 to edges2 (containing reverse of edges1) on key to retrieve all path2 pairs as values. Then map the values as key pair and 1 as value
    val path2 = revEdges.mapPartitions(
      iter => {
        iter.flatMap
        {
          case (k,v1 ) =>
            smallRDDLocal.get(k) match {
              case None => Seq.empty[((Int, Int), Int)]
              case Some(v2) =>  Seq(((v1, v2),1))
            }
        }
      }, preservesPartitioning = true)

    //path2.collect().foreach(println)

    // Convert each line of csv to pair, filter out users greater than MAX and create RDD with the relationshp pair as key and 1 as value
    val edges3 = sc.parallelize(smallRDDLocal.map(row1 => ((row1._1, row1._2),1)).toSeq)

    //edges3.collect().foreach(println)

    // Join edges3 with path2 and count the number of keys
    val noOfTriangles = path2.join(edges3).groupByKey().count()

    // Save the result in the output file
    sc.parallelize(Seq("No of Triangles", noOfTriangles / 3)).saveAsTextFile(args(1))
    /*
    val ss = SparkSession
      .builder()
      .appName("Reduce side join")
      .config("spark.some.config.option", "some-value")
      .getOrCreate()
    */
    //import ss.implicits._

  }
}