package org.caexplorer.analysis

/**
 * Registry of all available CA analysis tools.
 */
object AnalysisRegistry {
    private val analyses = mutableListOf<Analysis>()

    init {
        register(PopulationAnalysis())
        register(DensityAnalysis())
        register(EntropyAnalysis())
        register(ActivityAnalysis())
        register(ClusterAnalysis())
        register(CenterOfMassAnalysis())
        register(MutualInformationAnalysis())
        register(HammingDistanceAnalysis())

        // Dimensional analyses
        register(BoxCountingDimensionAnalysis())
        register(CorrelationDimensionAnalysis())
        register(LargestNeighborhoodAnalysis())

        // Visual analyses
        register(SliceAnalysis())
        register(SymmetryAnalysis())
        register(SpeedAnalysis())
    }

    fun register(analysis: Analysis) {
        analyses.add(analysis)
    }

    fun getAll(): List<Analysis> = analyses.toList()

    fun getByCategory(category: AnalysisCategory): List<Analysis> =
        analyses.filter { it.category == category }
}
