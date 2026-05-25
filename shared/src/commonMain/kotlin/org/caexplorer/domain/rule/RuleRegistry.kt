package org.caexplorer.domain.rule

import org.caexplorer.domain.rule.implementations.*

/**
 * Registry of all available CA rules, organized by category.
 */
object RuleRegistry {
    private val rules = mutableListOf<Rule>()

    init {
        registerBuiltInRules()
    }

    private fun registerBuiltInRules() {
        // Elementary (1D)
        for (i in 0..255) {
            register(WolframRule(i))
        }

        // Life-like
        register(Life())
        register(LifeLike.HIGHLIFE)
        register(LifeLike.SEEDS)
        register(LifeLike.DAY_AND_NIGHT)
        register(LifeLike.DIAMOEBA)
        register(LifeLike.LONG_LIFE)
        register(LifeLike.STAINS)
        register(LifeLike.REPLICATOR)

        // Classic rules
        register(CyclicCA())
        register(BriansBrain())
        register(Wireworld())
        register(ForestFire())
        register(Diffusion())
        register(DiffusionLimitedAggregation())
        register(IsingModel())
        register(RockPaperScissors())

        // Additional rules
        register(LangtonsAnt())
        register(ContinuousCA.DEFAULT)
        register(ContinuousCA.CLASS_IV)
        register(ContinuousCA.CHAOTIC)
        register(RealSpirals())
        register(Spirals())
        register(MajorityVote(numStates = 2))
        register(MajorityVote(numStates = 3))
        register(NeuralNetCA())
        register(NeuralNet())
        register(GrowingSeed())
        register(LavaLamp())
        register(Anneal())
        register(MazeRule())

        // Totalistic rules
        register(OuterTotalisticRule(
            ruleNumber = 224, numStates = 2,
            displayName = "Outer Totalistic",
            description = "Generic outer totalistic rule using a configurable rule number"
        ))
        register(Totalistic2DRule(
            ruleNumber = 224, numStates = 2,
            displayName = "Totalistic 2D",
            description = "Generic 2D totalistic rule including cell value in the sum"
        ))

        // Visual rules
        register(Snowflake())
        register(SnowflakeDust())
        register(SnowflakeMaze())
        register(PulsingSnowflake())
        register(Lightning())
        register(ThunderStorm())
        register(Fireworks())
        register(CollidingCyclones())
        register(EpilepticBlobs())

        // Fractal rules
        register(Fractal())
        register(FractalIteration())
        register(FractalThreshold())
        register(MovingFractal())

        // Physics rules
        register(Q2RIsingModel())
        register(Nucleation())

        // Sorting rules
        register(IntegerAverage())
        register(IntegerSort())
        register(CopyRandomNeighbor())
        register(SumModuloN())

        // Life variant rules
        register(Bunnies())
        register(CrystalLife())
        register(DrunkGliders())
        register(TunnellingSpaceships())
        register(WaterSkimmers())

        // Exotic rules
        register(CyclicPulse())
        register(ElectricLoops())
        register(SuperLoops())
        register(GalacticFlashWeb())
        register(SelfishCA())
        register(ObesityModel())
        register(Symmetry())

        // Misc rules
        register(RandomUpdate())
        register(LangtonLambda())
        register(PrimeDeath())
        register(PrimePlague())

        // Missing rules ported from original Java CAExplorer
        register(Julia())
        register(HexLife())
        register(TriLife())
        register(ComplexLife())
        register(ComplexContinuousCA())
        register(AlternateContinuousCA())
        register(ReversibleRuleNumber(90))
        register(MajorityWins())
        register(MinorityWins())
        register(MajorityProbablyWins())
        register(PistonPrime())
        register(House())
        register(ChutesLaddersAndShifts())
        register(CoolClassIV())
        register(PrettyClassIV())
        register(SatansStaircase())
        register(CellularMarketModel())
        register(RealSort())
        register(ChainLinkFence())
        register(TuringMachine())
    }

    fun register(rule: Rule) {
        rules.add(rule)
    }

    fun getAll(): List<Rule> = rules.toList()

    fun getByCategory(category: RuleCategory): List<Rule> =
        rules.filter { it.category == category }

    fun getByName(name: String): Rule? =
        rules.find { it.displayName == name }

    fun getCategories(): List<RuleCategory> =
        rules.map { it.category }.distinct().sortedBy { it.displayName }

    /**
     * Non-elementary rules (excludes the 256 Wolfram rules for cleaner browsing).
     */
    fun getFeaturedRules(): List<Rule> =
        rules.filter { it.category != RuleCategory.ELEMENTARY }
}
