package fairTA.main

import java.io.File
import java.nio.file.Files
import kotlin.random.Random
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.notExists

fun generateRandomCoords(xRange: ClosedFloatingPointRange<Double>, yRange: ClosedFloatingPointRange<Double>): Coords {
    return Coords(
        x = Random.nextDouble(xRange.start, xRange.endInclusive),
        y = Random.nextDouble(yRange.start, yRange.endInclusive)
    )
}

fun generateInstances(
    numSources: Int = 20,
    numTargets: Int = 1000,
    numInstances: Int = 50
) {

    val xRange = 0.0..80.0
    val yRange = 0.0..100.0

    // Generate shared source coordinates
    val sourceCoords = mutableSetOf<Coords>()
    while (sourceCoords.size < numSources) {
        sourceCoords.add(generateRandomCoords(xRange, yRange))
    }

    repeat(numInstances) { i ->
        val targetCoords = mutableSetOf<Coords>()

        // Avoid overlapping with source coordinates
        while (targetCoords.size < numTargets) {
            val coord = generateRandomCoords(xRange, yRange)
            if (coord !in sourceCoords) {
                targetCoords.add(coord)
            }
        }

        val directory: Path = Paths.get("data", "instances", "instance_${numSources}_${numTargets}")
        if (directory.notExists()){ Files.createDirectory(directory) }
        val filepath = directory.resolve( "instance_${numSources}_${numTargets}_${i+1}.txt")
        val file = File(filepath.toUri())
        file.printWriter().use { out ->
            out.println("numSource numTargets")
            out.println("$numSources $numTargets")
            out.println("Source Coordinates")
            sourceCoords.forEach { out.println(it) }
            out.println("Target Coordinates")
            targetCoords.forEach { out.println(it) }
            out.print("EOF")
        }
    }
}

fun main(){
    generateInstances(numSources = 2, numTargets = 10, numInstances = 1)
}