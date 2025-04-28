package fairTA.data

import fairTA.main.Coords
import fairTA.main.Graph
import fairTA.main.Vertex
import io.github.oshai.kotlinlogging.KotlinLogging
import org.jgrapht.graph.DefaultWeightedEdge
import java.io.File
import kotlin.math.hypot

private val log = KotlinLogging.logger {}

class InstanceDto(
    private val instanceName: String,
    private val filePath: String,
) {
    private val graph = Graph(DefaultWeightedEdge::class.java)
    private val numSource: Int
    private val numTargets: Int
    private val sourceVertices: List<Vertex>
    private val targetVertices: List<Vertex>



    init {
        log.debug{"starting initialization of instance $instanceName..."}
        val lines = collectLinesFromFile()
        numSource = lines[1].trim().split("\\s+".toRegex()).first().toInt()
        numTargets = lines[1].trim().split("\\s+".toRegex()).last().toInt()

        sourceVertices = (0 until numSource).map { i->
            Vertex(type = "source", id = i, coords = parseCoords(lines[i+3])).also {graph.addVertex(it)}
        }
        targetVertices = (0 until numTargets).map { j->
            Vertex(type = "target", id = j, coords = parseCoords(lines[numSource+4+j])).also { graph.addVertex(it) }
        }
        for (source in sourceVertices) {
            for (target in targetVertices) {
                val edgeLength: Double = getEdgeLength(source, target)
                val edge = DefaultWeightedEdge()
                graph.addEdge(source, target, edge)
                graph.setEdgeWeight(edge, edgeLength)
            }
        }
    }

    private fun collectLinesFromFile(): List<String> {
        return File(filePath + instanceName).readLines()
    }

    private fun parseCoords(line: String): Coords {
        val values: List<Double> = line.trim().split("\\s+".toRegex()).map {
            it.toDouble()
        }
        return Coords(values[0], values[1])
    }

    private fun getEdgeLength(v1: Vertex, v2: Vertex): Double=
        "%.4f".format(hypot(v1.coords.x - v2.coords.x, v1.coords.y - v2.coords.y)).toDouble()

    fun getInstance() = Instance(
        instanceName = instanceName,
        graph = graph,
        numSource = numSource,
        numTargets = numTargets,
        sourceVertices = sourceVertices,
        targetVertices = targetVertices
    )

}