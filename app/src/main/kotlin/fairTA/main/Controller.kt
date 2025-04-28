package fairTA.main

import fairTA.data.Instance
import fairTA.data.InstanceDto
import fairTA.data.Parameters
import fairTA.solver.Solver
import ilog.cplex.IloCplex
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.encodeToString
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import kotlin.io.path.notExists

private val log = KotlinLogging.logger {}


class Controller {
    private lateinit var cplex: IloCplex
    private lateinit var instance: Instance
    private lateinit var outputFile: String

    fun parseArgs(args: Array<String>){
        val parser = Cliparser()
        parser.main(args)
        val outputDir = Paths.get(parser.outputPath)
        if (outputDir.notExists()) { Files.createDirectory(outputDir) }
        outputFile = parser.outputPath + parser.instanceName.split('.').first()+
                "-p-${parser.pNorm}-fc-${(parser.fairnessCoefficient*100).toInt()}.json"
        Parameters.initialize(
            instanceName = parser.instanceName,
            instancePath = parser.instancePath,
            pNorm = parser.pNorm,
            fairnessCoefficient = parser.fairnessCoefficient,
            outputFile = parser.outputPath,
            timeLimitInSeconds = parser.timeLimitInSeconds
        )
    }

    private fun initCPLEX() {
        cplex = IloCplex()
    }

    fun populateInstance() {
        instance = InstanceDto(
            Parameters.instanceName,
            Parameters.instancePath
        ).getInstance()
    }

    fun run() {
        initCPLEX()
        val solver = Solver(instance, cplex, Parameters)
        try{
            val result = solver.solve()
            val json = prettyJson.encodeToString(result)
            File(outputFile).writeText(json)
        } catch (e: FairTAException){
            val result = solver.getInfeasibleResult()
            val json = prettyJson.encodeToString(result)
            File(outputFile).writeText(json)
            log.info { e }
        }
    }

}