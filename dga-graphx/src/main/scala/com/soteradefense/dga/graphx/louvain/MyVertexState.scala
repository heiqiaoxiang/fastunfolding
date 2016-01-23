package com.soteradefense.dga.graphx.louvain

/**
  * Created by wzp on 16-1-23.
  */
class MyVertexState extends Serializable{
    var community = -1L
    var tot = -1L
    var in = -1L
    var nodeWeight = 0L
    var changed = false

    override def toString = s"community:${community}, tot:${tot}, in:${in}, nodeWeight:${nodeWeight}, change:${change}"
}
