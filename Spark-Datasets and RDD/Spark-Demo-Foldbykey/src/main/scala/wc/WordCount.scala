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


object WordCountMain {
  def rdd_fk(textFile: RDD[String],outPath: String): Unit ={
    val title = textFile.map(line => line.split(",")(1)).map(word =>(word,1))
    // Fold by key
    val fold_key_output = title.foldByKey(0)(_+_)
    fold_key_output.saveAsTextFile(outPath)
    println(fold_key_output.toDebugString)
  }
  def main(args: Array[String]) {
    val logger: org.apache.log4j.Logger = LogManager.getRootLogger
    if (args.length != 2) {
      logger.error("Usage:\nwc.WordCountMain <input dir> <output dir>")
      System.exit(1)
    }
    val conf = new SparkConf().setAppName("Word Count")
    val sc = new SparkContext(conf)

    val textFile = sc.textFile(args(0))
    rdd_fk(textFile,args(1))
    //val title = textFile.map(line => line.split(",")(1)).map(word =>(word,1))
    /*
    val ss = SparkSession
      .builder()
      .appName("Spark SQL basic example")
      .config("spark.some.config.option", "some-value")
      .getOrCreate()

    import ss.implicits._

    val data = textFile.toDS()
    val follow_pairs = data.map(line => (line.split(",")(1),1)).toDF("Followee","count").groupBy("Followee").count()
    follow_pairs.write.csv(args(1))*/
  }
}
//rdd_g(textFile, args(1))
