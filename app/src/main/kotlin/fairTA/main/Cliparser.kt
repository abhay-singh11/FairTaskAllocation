package fairTA.main

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.convert
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.validate
import com.github.ajalt.clikt.parameters.types.double
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File

private val log = KotlinLogging.logger {}
class Cliparser : CliktCommand() {

    private val numSource = 20
    private val numTarget = 500

    val instanceName: String by option(
        "-n",
        help = "instance name"
    ).default("instance_${numSource}_${numTarget}_5.txt")

    val instancePath: String by option(
        "-path",
        help = "instance path"
    ).default("./data/instances/instance_${numSource}_$numTarget/").validate {
        require(File(instancePath + instanceName).exists()) {
            "file does not exist!!!"
        }
    }

    val pNorm: String by option(
        "-p",
        help = "p value (integer >= 2 or 'inf')"
    ).convert { value ->
        if (value == "inf") {
            value
        } else {
            val intVal = value.toIntOrNull()
                ?: fail("p must be an integer >= 2 or 'inf'")
            require(intVal >= 2) { "p should be an integer greater than or equal to 2" }
            intVal.toString()
        }
    }.default("2")

    val fairnessCoefficient: Double by option(
        "-fc",
        help = "fairness value"
    ).double().default(0.5).validate {
        require(it in 0.0..1.0) {
            "fairness Coefficient should be between 0 and 1"
        }
    }


    val timeLimitInSeconds: Double by option(
        "-t",
        help = "time limit in seconds for CPLEX"
    ).double().default(3600.0).validate {
        require(it > 0.0) {
            "time limit should be positive"
        }
    }

    val outputPath: String by option(
        "-r",
        help = "path to file with to store result"
    ).default("./data/results/instance_${numSource}_$numTarget/")

    override fun run() {
        log.debug { "reading command line arguments..." }
    }

}