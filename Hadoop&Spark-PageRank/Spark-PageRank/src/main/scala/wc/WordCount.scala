package wc

import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.log4j.LogManager
import org.apache.log4j.Level
import scala.util.control.Breaks._
import scala.collection.mutable.ArrayBuffer

object WordCountMain {
  
  def main(args: Array[String]) {
    val conf = new SparkConf().setAppName("PageRank")
    val sc = new SparkContext(conf)

    val k = 100
    rdd_pr(k, args(1), sc)
    // Graph rdd

  }
  // RDD implementation of page rank
  def rdd_pr(k: Int, output_path: String, sc: SparkContext) = {

    val numOfNodes = k * k // k squared
    val nodes = List.range(1, numOfNodes+2)
    val edges = nodes.sliding(2).map(ele =>
      if(ele.head%k == 0) {
        (ele.head.toString,"0")
      }else{
        (ele.head.toString,ele.tail.head.toString)
      }
    ).toList

    val graph_rdd = sc.parallelize(edges).repartition(4)  // create graph rdd
    //graph_rdd.persist()

    val rank = 1.0/numOfNodes
    val rank_nodes = List.range(0, numOfNodes+1)
    val ranks_list = rank_nodes.map(ele =>
    if(ele == 0){
      (ele.toString,0.0)
    }else{
      (ele.toString,rank)
      }
    )
    var rank_rdd = sc.parallelize(ranks_list).repartition(4) // create rank rdd
    var name = ArrayBuffer[String]()

    for (j <- 1 to 10) {
      name = null
      val join_rdd = graph_rdd.join(rank_rdd)  // Join Graph with Ranks on v1 to create a new RDD of triples (v1, v2, pr).
      val transferred_pr = join_rdd.flatMap(row =>
        if(row._1.toInt%k == 1){
          List((row._1,0.0),row._2)
        }else{
          List(row._2)
        })
      val temp2 = transferred_pr.reduceByKey(_+_)
      val delta = temp2.lookup("0").head
      rank_rdd  = temp2.map(ele=>
      if(ele._1.equals("0")){
        (ele._1,ele._2)
      }else{
        (ele._1,ele._2+(delta/numOfNodes))
        }
      )
    }
    print(rank_rdd.toDebugString)

    name = [rank_rdd.lookup("0").head.toString,rank_rdd.lookup("1").head.toString]
    val result =sc.parallelize(name)
    result.saveAsTextFile(output_path)
  }
}
