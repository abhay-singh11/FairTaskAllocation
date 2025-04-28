package fairTA.data

import fairTA.main.Vertex
import kotlinx.serialization.Serializable

@Serializable
data class Result(
    val instanceName: String,
    val numSource: Int,
    val numTarget: Int,
    val pNorm: String,
    val fairnessCoefficient: Double,
    val assignmentCosts: List<Double>? = null,
    val objectiveValue: Double? = null,
    val computationTimeInSec: Double,
    val optimalityGapPercent: Double? = null,
    val assignments: List<List<Int>>? = null,
    val sourceVertices: List<Vertex>,
    val targetVertices: List<Vertex>,
)
