package fairTA.solver

import fairTA.data.Instance
import ilog.concert.IloLinearNumExpr
import ilog.concert.IloNumVar
import ilog.cplex.IloCplex
import ilog.cplex.IloCplex.Callback.Context
import ilog.cplex.IloCplexModeler
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlin.math.pow

private val log = KotlinLogging.logger {}


class FairTACallback(
    private val instance: Instance,
    private val sourceCost: Map<Int, IloNumVar>,
    private var fairnessFactor: IloNumVar,
    private var conicAuxiliaryVariable: Map<Int, IloNumVar>,
    private val pNorm: String,
    private val pValue: Int? = pNorm.toIntOrNull(),
) : IloCplex.Callback.Function {

    override fun invoke(context: Context) {
        if (context.inCandidate()) {
            if (pNorm != "inf") {
                addOuterApproximations(context)
            }
        }
    }

    private fun addOuterApproximations(context: Context){
        /* Add outer approximation for z <= x^alpha * y^(1-alpha)
        where   alpha  = 1/pValue
                z: sourceCost   x: AuxVar   y: FairnessFactor (l_eps)
                Tangent equation at (x0, y0, z0),
                (alpha*y0*z0) x + (1-alpha)z0*x0 y >= x0*y0 z   */
        val m: IloCplexModeler = context.cplex
        val tolerances = 1e-4
        val alpha: Double = 1.0/ pValue!!
        (0 until instance.numSource).forEach { source ->
            val x = context.getCandidatePoint(conicAuxiliaryVariable[source])
            val y = context.getCandidatePoint(fairnessFactor)
            val z = context.getCandidatePoint(sourceCost[source])
            if (z - x.pow(alpha)*y.pow(1.0-alpha) > tolerances) {
                val projectionPoints = getProjectionPoints(x, y, z)
                projectionPoints.forEach { (x0, y0, z0) ->
                    val cutExpr: IloLinearNumExpr = m.linearNumExpr()
                    cutExpr.addTerms(
                        listOf(
                            conicAuxiliaryVariable[source],
                            fairnessFactor,
                            sourceCost[source]).toTypedArray(),
                        listOf(
                            alpha*y0*z0,
                            (1-alpha)*z0*x0,
                            -x0*y0).toDoubleArray()
                    )
                    context.rejectCandidate(m.ge(cutExpr, 0.0))
                    log.debug { "adding OA for source $source" }
                    log.debug { cutExpr }
                    cutExpr.clear()
                }
            }
        }
    }

    private fun getProjectionPoints(x: Double, y: Double, z: Double): List<List<Double>> {
        /* z <= x^alpha * y^(1-alpha)
           alpha  = 1/pValue
        */
        val tolerances = 1e-5
        val alpha: Double = 1.0/ pValue!!
        val projectionPoints: MutableList<List<Double>> = mutableListOf()
        projectionPoints.add(listOf(x, y, x.pow(alpha)*y.pow(1.0-alpha) ))
        if (y > tolerances) projectionPoints.add(listOf( z.pow(pValue) / (y.pow(pValue - 1.0)), y, z ))
        if (x > tolerances) projectionPoints.add(listOf( x, z.pow(pValue/(pValue-1.0)) / x.pow(1.0 / (pValue - 1.0)), z ))

        return projectionPoints
    }
}