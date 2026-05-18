package org.caexplorer.data

/**
 * Data class representing a saved simulation state.
 */
data class SimulationFileData(
    val ruleName: String,
    val latticeType: String,
    val width: Int,
    val height: Int,
    val cellStates: IntArray,
    val colorSchemeIndex: Int,
    val generation: Long,
    val version: Int = 1
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SimulationFileData) return false
        return ruleName == other.ruleName &&
                latticeType == other.latticeType &&
                width == other.width &&
                height == other.height &&
                cellStates.contentEquals(other.cellStates) &&
                colorSchemeIndex == other.colorSchemeIndex &&
                generation == other.generation &&
                version == other.version
    }

    override fun hashCode(): Int {
        var result = ruleName.hashCode()
        result = 31 * result + latticeType.hashCode()
        result = 31 * result + width
        result = 31 * result + height
        result = 31 * result + cellStates.contentHashCode()
        result = 31 * result + colorSchemeIndex
        result = 31 * result + generation.hashCode()
        result = 31 * result + version
        return result
    }
}

/**
 * Serialize a simulation state to a text-based format.
 */
fun serializeSimulation(data: SimulationFileData): String = buildString {
    appendLine("CAExplorerMP v${data.version}")
    appendLine("rule=${data.ruleName}")
    appendLine("lattice=${data.latticeType}")
    appendLine("width=${data.width}")
    appendLine("height=${data.height}")
    appendLine("colorScheme=${data.colorSchemeIndex}")
    appendLine("generation=${data.generation}")
    append("states=${data.cellStates.joinToString(",")}")
}

/**
 * Deserialize a simulation state from text. Returns null on parse failure.
 */
fun deserializeSimulation(text: String): SimulationFileData? {
    return try {
        val lines = text.lines()
        if (lines.size < 8) return null

        val header = lines[0]
        if (!header.startsWith("CAExplorerMP v")) return null
        val version = header.removePrefix("CAExplorerMP v").trim().toIntOrNull() ?: return null

        fun lineValue(line: String, prefix: String): String? {
            if (!line.startsWith("$prefix=")) return null
            return line.removePrefix("$prefix=")
        }

        val ruleName = lineValue(lines[1], "rule") ?: return null
        val latticeType = lineValue(lines[2], "lattice") ?: return null
        val width = lineValue(lines[3], "width")?.toIntOrNull() ?: return null
        val height = lineValue(lines[4], "height")?.toIntOrNull() ?: return null
        val colorSchemeIndex = lineValue(lines[5], "colorScheme")?.toIntOrNull() ?: return null
        val generation = lineValue(lines[6], "generation")?.toLongOrNull() ?: return null
        val statesStr = lineValue(lines[7], "states") ?: return null
        val cellStates = statesStr.split(",").map { it.trim().toInt() }.toIntArray()

        if (cellStates.size != width * height) return null

        SimulationFileData(
            ruleName = ruleName,
            latticeType = latticeType,
            width = width,
            height = height,
            cellStates = cellStates,
            colorSchemeIndex = colorSchemeIndex,
            generation = generation,
            version = version
        )
    } catch (_: Exception) {
        null
    }
}
