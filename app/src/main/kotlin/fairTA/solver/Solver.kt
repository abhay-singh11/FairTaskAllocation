package fairTA.solver

import fairTA.data.Instance
import fairTA.data.Parameters
import fairTA.data.Result
import fairTA.main.FairTAException
import fairTA.main.Graph
import ilog.concert.IloIntVar
import ilog.concert.IloLinearNumExpr
import ilog.concert.IloNumVar
import ilog.cplex.IloCplex
import kotlin.math.pow
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.PrintStream
import kotlin.math.round
import kotlin.properties.Delegates

private val log = KotlinLogging.logger {}
class Solver(
    private val instance: Instance,
    private val cplex: IloCplex,
    config: Parameters,
    private val graph: Graph = instance.graph
) {
    private val pNorm = config.pNorm
    private val pValue = pNorm.toIntOrNull()
    private val fairnessCoefficient = config.fairnessCoefficient
    private val timeLimitInSeconds = config.timeLimitInSeconds
    private var computationTime by Delegates.notNull<Double>()
    private lateinit var edgeVariable: Map<Int, Map<Int, IloIntVar>>
    private lateinit var sourceCost: Map<Int, IloNumVar>
    private lateinit var fairnessFactor: IloNumVar
    private lateinit var conicAuxiliaryVariable: Map<Int, IloNumVar>

    init {
        addVariables()
        addConstraints()
        addObjective()
        setupCallback()
//        cplex.exportModel("model.lp")
    }

    private fun addVariables() {
        // Add edge variable x_ij between each source i and target j
        edgeVariable = (0 until instance.numSource).associateWith { sourceIdx->
            (0 until instance.numTargets).associateWith { targetIdx->
                cplex.boolVar("x_${sourceIdx}_${targetIdx}")
            }
        }
        // Add li variables for each source i
        sourceCost = (0 until  instance.numSource).associateWith { sourceIdx->
            cplex.numVar(0.0, Double.POSITIVE_INFINITY, "l_$sourceIdx")
        }

        fairnessFactor = cplex.numVar(0.0, Double.POSITIVE_INFINITY, "l_eps")

        conicAuxiliaryVariable = (0 until instance.numSource).associateWith { sourceIdx->
            cplex.numVar(0.0, Double.POSITIVE_INFINITY, "k_$sourceIdx")
        }
    }


    private fun addConstraints() {
        addSourceCostDefinition()
        addTargetVisitConstraints()
        addFairnessConstraints()
    }

    private fun addSourceCostDefinition() {
        // Add sourceCost constraints li = SUM(c_ij * x_ij) for all i
        instance.sourceVertices.forEachIndexed{sourceIdx, sourceVertex ->
            val costExpr: IloLinearNumExpr = cplex.linearNumExpr()
            instance.targetVertices.forEachIndexed{targetIdx, targetVertex ->
                val edge = graph.getEdge(sourceVertex, targetVertex)
                costExpr.addTerm(edgeVariable[sourceIdx]!![targetIdx], -graph.getEdgeWeight(edge))
            }
            costExpr.addTerm(1.0, sourceCost[sourceIdx])
            cplex.addEq(costExpr, 0.0, "sourceCost_$sourceIdx")
            costExpr.clear()
        }
    }

    private fun addTargetVisitConstraints(){
        // Add target visit constraints SUM(x_ij) = 1 for all j
        (0 until instance.numTargets).forEach { targetIdx->
            val visitExpr: IloLinearNumExpr = cplex.linearNumExpr()
            (0 until instance.numSource).forEach { sourceIdx->
                visitExpr.addTerm(1.0, edgeVariable[sourceIdx]!![targetIdx])
            }
            cplex.addEq(visitExpr, 1.0, "targetVisit_$targetIdx")
            visitExpr.clear()
        }
    }

    private fun addFairnessConstraints() {
        // add infinity norm constraints if pNorm is inf
        if (pNorm == "inf") {
            addInfinityNormConstraints()
            return
        }

        /*
        (1- eps + n^(1-1/p)*eps) ||l||_p <= ||l||_1
        let epsBar = ( 1 + eps*( n^(1-1/p) - 1 )
        l_eps = ||l||_1 / epsBar (fairnessFactorExpr)
         */
        val epsBar = 1.0 + fairnessCoefficient * (instance.numSource.toDouble().pow(1.0 - (1.0 / pValue!!)) - 1.0)
        /* l_eps * epsBar = ||l||_1  */
        val fairnessFactorExpr: IloLinearNumExpr = cplex.linearNumExpr()
        fairnessFactorExpr.addTerms(
            (0 until instance.numSource).map { sourceCost[it] }.toTypedArray(),
            List(instance.numSource) {-1.0}.toDoubleArray()
        )
        fairnessFactorExpr.addTerm(epsBar, fairnessFactor)
        cplex.addEq(fairnessFactorExpr, 0.0, "FairnessFactorConstraint")
        fairnessFactorExpr.clear()

        /* sum_i k_i = L_eps */
        val conicAuxiliaryVariableExpr: IloLinearNumExpr = cplex.linearNumExpr()
        conicAuxiliaryVariableExpr.addTerms(
            (0 until instance.numSource).map { conicAuxiliaryVariable[it] }.toTypedArray(),
            List(instance.numSource) {1.0}.toDoubleArray()
        )
        conicAuxiliaryVariableExpr.addTerm(-1.0, fairnessFactor)
        cplex.addEq(conicAuxiliaryVariableExpr, 0.0, "AuxVariableConstraint")
        conicAuxiliaryVariableExpr.clear()

        /* Add OA of each conic constraint around (1, 1, 1)
        *  z <= x^alpha * y^(1-alpha)
        *  z: sourceCost    x: AuxVar   y: FairnessFactor(l_eps)
        *  Tangent equation at (x0, y0, z0),
        *  (alpha*y0*z0) x + (1-alpha)z0*x0 y >= x0*y0 z
        * */
        val alpha: Double = 1.0/pValue
        (0 until instance.numSource).forEach { source ->
            val tangentExpr: IloLinearNumExpr = cplex.linearNumExpr()
            tangentExpr.addTerms(
                listOf(
                    conicAuxiliaryVariable[source],
                    fairnessFactor,
                    sourceCost[source]).toTypedArray(),
                listOf(alpha, 1.0-alpha, -1.0).toDoubleArray()
            )
            cplex.addGe(tangentExpr, 0.0, "tangent_$source")
            tangentExpr.clear()
        }
    }

    private fun addInfinityNormConstraints() {
        val epsBar: Double = 1.0 + fairnessCoefficient * (instance.numSource.toDouble() - 1.0)
        /* l_eps * epsBar = ||l||_1  */
        val fairnessFactorExpr: IloLinearNumExpr = cplex.linearNumExpr()
        fairnessFactorExpr.addTerms(
            (0 until instance.numSource).map { sourceCost[it] }.toTypedArray(),
            List(instance.numSource) {-1.0}.toDoubleArray()
        )
        fairnessFactorExpr.addTerm(epsBar, fairnessFactor)
        cplex.addEq(fairnessFactorExpr, 0.0, "FairnessFactorConstraint")
        fairnessFactorExpr.clear()
        /* for each i, x_i <= l_eps */
        (0 until instance.numSource).forEach { source ->
            val minMaxExpr: IloLinearNumExpr = cplex.linearNumExpr()
            minMaxExpr.addTerms(
                listOf(sourceCost[source], fairnessFactor).toTypedArray(),
                listOf(1.0, -1.0).toDoubleArray()
            )
            cplex.addLe(minMaxExpr, 0.0, "sourceConstraint_$source")
            minMaxExpr.clear()
        }
    }

    private fun addObjective() {
        // SUM(li)
        val objExr = cplex.linearNumExpr()
        objExr.addTerms(
            (0 until instance.numSource).map { sourceCost[it] }.toTypedArray(),
            List(instance.numSource) { 1.0 }.toDoubleArray()
        )
        cplex.addMinimize(objExr)
        objExr.clear()
    }

    private fun setupCallback() {
        val cb = FairTACallback(
            pNorm = pNorm,
            instance = instance,
            sourceCost = sourceCost,
            fairnessFactor = fairnessFactor,
            conicAuxiliaryVariable = conicAuxiliaryVariable,
        )
        val contextMask = IloCplex.Callback.Context.Id.Candidate
        cplex.use(cb, contextMask)
    }

    fun getInfeasibleResult(): Result {
        val result = Result(
            instanceName = instance.instanceName,
            numSource = instance.numSource,
            numTarget = instance.numTargets,
            pNorm = pNorm,
            fairnessCoefficient = fairnessCoefficient,
            computationTimeInSec = round(computationTime*100.0)/100.0,
            sourceVertices = instance.sourceVertices,
            targetVertices = instance.targetVertices
        )
        return result
    }
    private fun getResult(): Result {
        val assignments = (0 until instance.numSource).map { source->
            edgeVariable[source]!!.filter { cplex.getValue(it.value) > 0.9 }.keys.toList()
        }
        val assignmentCosts = (0 until instance.numSource).map { round( cplex.getValue(sourceCost[it]!!)*10000)/10000 }
        val result = Result(
            instanceName = instance.instanceName,
            numSource = instance.numSource,
            numTarget = instance.numTargets,
            pNorm = pNorm,
            fairnessCoefficient = fairnessCoefficient,
            assignmentCosts = assignmentCosts,
            objectiveValue = cplex.objValue,
            computationTimeInSec = round(computationTime*100.0)/100.0,
            optimalityGapPercent = round(cplex.mipRelativeGap*10000.0)/100.0,
            assignments = assignments,
            sourceVertices = instance.sourceVertices,
            targetVertices = instance.targetVertices
        )
        log.info { "objective value ${result.objectiveValue}" }
        return result
    }

    fun solve(): Result {
        cplex.setParam(IloCplex.Param.MIP.Display, 3)
        cplex.setParam(IloCplex.Param.TimeLimit, timeLimitInSeconds)
        cplex.setParam(IloCplex.Param.MIP.Strategy.Search, IloCplex.MIPSearch.Traditional)
        cplex.setParam(IloCplex.Param.MIP.Tolerances.MIPGap, 0.001)
//        cplex.setOut(PrintStream(object : java.io.OutputStream() {
//            override fun write(b: Int) {
//            }
//        }
//        ))
//        cplex.setParam(IloCplex.Param.Emphasis.MIP, IloCplex.MIPEmphasis.Optimality)

        val startTime = cplex.cplexTime
        if (!cplex.solve()){
            computationTime = cplex.cplexTime.minus(startTime)
            throw FairTAException("Fair Task Allocation is infeasible for fairness coefficient: $fairnessCoefficient and pNorm: $pNorm")
        }
        computationTime = cplex.cplexTime.minus(startTime)
        log.info {"Allocation costs: ${cplex.getValues(sourceCost.values.toTypedArray()).toList()}"}
        log.info {"Total cost: ${cplex.getValues(sourceCost.values.toTypedArray()).toList().sumOf { it }}" }

        log.info { "computation time: $computationTime" }
        return getResult()
    }


}