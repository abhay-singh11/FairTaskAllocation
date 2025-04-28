package fairTA.data

object Parameters {

    var instanceName: String = "instance_5_100_1"
        private set

    var instancePath: String = "./data/CustomInstances/"
        private set

    var pNorm: String = "2"
        private set

    var fairnessCoefficient: Double = 1.0
        private set

    var outputFile: String = ""
        private set

    var timeLimitInSeconds: Double = 3600.0
        private set


    fun initialize(
        instanceName: String,
        instancePath: String,
        pNorm: String,
        fairnessCoefficient: Double,
        outputFile: String,
        timeLimitInSeconds: Double
    ){
        Parameters.instanceName = instanceName
        Parameters.instancePath = instancePath
        Parameters.pNorm = pNorm
        Parameters.fairnessCoefficient = fairnessCoefficient
        Parameters.outputFile = outputFile
        Parameters.timeLimitInSeconds = timeLimitInSeconds
    }
}