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
