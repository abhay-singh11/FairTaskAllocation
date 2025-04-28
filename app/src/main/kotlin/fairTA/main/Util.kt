package fairTA.main

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jgrapht.graph.DefaultWeightedEdge
import org.jgrapht.graph.SimpleWeightedGraph
@Serializable
data class Coords(val x: Double, val y: Double) {
    override fun toString(): String = "%.2f %.2f".format(x, y)
}
@Serializable
data class Vertex(
    val type: String,
    val id: Int,
    val coords: Coords,
)

typealias Graph = SimpleWeightedGraph<Vertex, DefaultWeightedEdge>

class FairTAException(message: String) : Exception(message)

@OptIn(ExperimentalSerializationApi::class)
val prettyJson = Json { // this returns the JsonBuilder
    prettyPrint = true
    // optional: specify indent
    prettyPrintIndent = " "
    explicitNulls = true
}

