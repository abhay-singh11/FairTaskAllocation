package fairTA.data

import fairTA.main.Graph
import fairTA.main.Vertex

data class Instance (
    val instanceName: String,
    val graph: Graph,
    val numSource: Int,
    val numTargets: Int,
    val sourceVertices: List<Vertex>,
    val targetVertices: List<Vertex>,
)