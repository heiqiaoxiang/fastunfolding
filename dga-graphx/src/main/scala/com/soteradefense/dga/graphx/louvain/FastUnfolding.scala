package com.soteradefense.dga.graphx.louvain

import org.apache.spark.SparkContext
import org.apache.spark.graphx.{VertexId, PartitionStrategy, TripletFields, Graph}

import scala.reflect.ClassTag

/**
  * Created by wzp on 16-1-23.
  */

/**
  *
  * @param minProgress
  * @param progressCounter
  * @param outputdir
  */
class FastUnfolding(outputdir: String,
                    minProgress: Int = 1,
                    progressCounter: Int = 1) {

    var qValues = Array[(Int, Double)]()

    def saveLevel(sc: SparkContext,
                  level: Int,
                  q: Double,
                  graph: Graph[MyVertexState, Long]) = {
        graph.vertices.saveAsTextFile(s"${outputdir}/level_${level}_vertices")
        graph.edges.saveAsTextFile(s"${outputdir}/level_${level}_edges")
        //graph.vertices.map( {case (id,v) => ""+id+","+v.internalWeight+","+v.community }).saveAsTextFile(outputdir+"/level_"+level+"_vertices")
        //graph.edges.mapValues({case e=>""+e.srcId+","+e.dstId+","+e.attr}).saveAsTextFile(outputdir+"/level_"+level+"_edges")
        qValues = qValues :+ ((level, q))
        println(s"qValue: $q")

        // overwrite the q values at each level
        sc.parallelize(qValues, 1).saveAsTextFile(s"${outputdir}/qvalues")
    }

    def run[VD: ClassTag](sc: SparkContext, graph: Graph[VD, Long]) = {
        val initialGraph = createGraph(graph)

        val graphWeight = initialGraph.vertices.map(
            vertex => {
                vertex._2.nodeWeight
            }
        ).reduce(_ + _)

        val broadcastGraphWeight = sc.broadcast(graphWeight)

        val initialModularity = initialGraph.vertices.map(
            vertex => {
                vertex._2.in / (2 * graphWeight) - vertex._2.tot * vertex._2.tot / (graphWeight * graphWeight)
            }
        ).reduce(_ + _)

        var level = -1
        var halt = false

        while(!halt) {
            level += 1
            println(s"Starting level ${level}")

            val (currentQ, currentGraph, passes) = runFastUnfolding(sc, initialGraph, minProgress, progressCounter)


        }
    }

    def runFastUnfolding(sc: SparkContext,
                        graph: Graph[MyVertexState, Long],
                        minProgress: Int,
                        progressCounter: Int) = {
        val cachedGraph = graph.cache()
        

    }

    def createGraph[VD: ClassTag](graph: Graph[VD, Long]): Graph[MyVertexState, Long] = {
        val nodeWeights = graph.aggregateMessages[Long](
            cxt => {
                cxt.sendToSrc(cxt.attr)
                cxt.sendToDst(cxt.attr)
            },
            (a, b) => a + b,
            TripletFields.EdgeOnly
        )

        nodeWeights.foreach(result => println(s"nodeweight: ${result._1}, ${result._2}"))


        val louvainGraph = graph.outerJoinVertices(nodeWeights)((vid, data, weightOption) => {
            val weight = weightOption.getOrElse(0L)
            val state = new MyVertexState()
            state.community = vid
            state.changed = false
            state.tot = weight
            state.in = 0
            state.nodeWeight = weight
            state
        }).partitionBy(PartitionStrategy.EdgePartition2D)

        louvainGraph
    }
}
