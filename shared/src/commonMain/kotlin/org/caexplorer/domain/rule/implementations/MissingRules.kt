package org.caexplorer.domain.rule.implementations

import org.caexplorer.domain.cell.Cell
import org.caexplorer.domain.cellstate.CellState
import org.caexplorer.domain.cellstate.Complex
import org.caexplorer.domain.cellstate.ComplexCellState
import org.caexplorer.domain.cellstate.IntegerCellState
import org.caexplorer.domain.cellstate.RealValuedState
import org.caexplorer.domain.rule.*
import kotlin.math.*
import kotlin.random.Random

// =============================================================================
// 1. Julia — Julia set fractal: z = z² + c
// =============================================================================

/**
 * Julia set rule. Each cell stores a complex value c.
 * The rule averages c with neighbors, then adds a sinusoidal increment
 * that cycles over 400 generations to create a tornado-like animation.
 *
 * Port of Java Julia.java (ComplexRuleTemplate).
 */
class Julia(
    private val juliaReal: Double = -0.835,
    private val juliaImag: Double = -0.2321,
    private val increment: Double = 0.001,
    private val cycleLength: Int = 400
) : ComplexRule() {
    override val displayName = "Julia (Chinese Dragon)"
    override val description = "Julia set fractal tornado with complex-valued cells"
    override val category = RuleCategory.FRACTAL
    override val compatibleLatticeNames = listOf("Square (Moore)", "Square (Von Neumann)")

    override fun nextState(cell: Cell, neighbors: Array<Cell>): CellState {
        val cellValue = getComplex(cell)
        val gen = cell.generation

        // Average the c values of the cell and its neighbors
        var sumRe = cellValue.real
        var sumIm = cellValue.imaginary
        for (n in neighbors) {
            val nv = getComplex(n)
            sumRe += nv.real
            sumIm += nv.imaginary
        }
        val count = neighbors.size + 1.0
        var newRe = sumRe / count
        var newIm = sumIm / count

        // Add sinusoidal increment that cycles over cycleLength generations
        val phase = 2.0 * PI * (gen % cycleLength) / (cycleLength - 1.0)
        newRe += increment * sin(phase)
        newIm += increment * sin(phase)

        return makeState(Complex(newRe, newIm))
    }

    override fun createInitialState(): CellState = makeState(Complex(juliaReal, juliaImag))

    override val properties get() = listOf(
        RuleProperty.FloatProperty("juliaReal", "c Real", juliaReal.toFloat(), -2f, 2f, "Real part of Julia constant c"),
        RuleProperty.FloatProperty("juliaImag", "c Imaginary", juliaImag.toFloat(), -2f, 2f, "Imaginary part of Julia constant c"),
        RuleProperty.FloatProperty("increment", "Increment", increment.toFloat(), 0f, 0.01f, "Sinusoidal increment per step")
    )

    override fun withProperty(key: String, value: Any): Rule = when (key) {
        "juliaReal" -> Julia((value as Number).toDouble(), juliaImag, increment, cycleLength)
        "juliaImag" -> Julia(juliaReal, (value as Number).toDouble(), increment, cycleLength)
        "increment" -> Julia(juliaReal, juliaImag, (value as Number).toDouble(), cycleLength)
        else -> this
    }

    companion object {
        private fun getComplex(cell: Cell): Complex {
            val state = cell.currentState
            return if (state is ComplexCellState) state.state else Complex.ZERO
        }

        private fun makeState(c: Complex): ComplexCellState = ComplexCellState(
            state = c,
            alternateStateValue = Complex(0.5, 0.5),
            emptyStateValue = Complex.ZERO,
            fullStateValue = Complex(1000.0, 0.0)
        )
    }
}

// =============================================================================
// 2. HexLife — Life adapted for hexagonal lattice
// =============================================================================

/**
 * Game of Life on a hexagonal (6-neighbor) lattice.
 * Uses standard Life rules: birth on 3, survive on 2 or 3.
 * On hexagonal grids, complexity is reduced compared to Moore 8-neighbor.
 *
 * Port of Java HexLife.java (extends Life).
 */
class HexLife : BinaryRule() {
    override val displayName = "Hexagonal Life"
    override val description = "Game of Life on hexagonal lattice"
    override val category = RuleCategory.LIFE_LIKE
    override val compatibleLatticeNames = listOf("Hexagonal")

    override fun nextState(cell: Cell, neighbors: Array<Cell>): CellState {
        val alive = cell.currentState.toInt() != 0
        var liveNeighbors = 0
        for (n in neighbors) {
            if (n.currentState.toInt() != 0) liveNeighbors++
        }
        val nextAlive = if (alive) {
            liveNeighbors == 2 || liveNeighbors == 3
        } else {
            liveNeighbors == 3
        }
        return IntegerCellState(if (nextAlive) 1 else 0)
    }

    override fun createInitialState(): CellState = IntegerCellState(0)
}

// =============================================================================
// 3. TriLife — Life adapted for triangular lattice
// =============================================================================

/**
 * Game of Life on a triangular lattice (3 or 12 neighbors).
 * Uses standard Life rules: birth on 3, survive on 2 or 3.
 *
 * Port of Java TriLife.java (extends Life).
 */
class TriLife : BinaryRule() {
    override val displayName = "Triangular Life"
    override val description = "Game of Life on triangular lattice"
    override val category = RuleCategory.LIFE_LIKE
    override val compatibleLatticeNames = listOf("Triangular", "Triangular (12 neighbor)")

    override fun nextState(cell: Cell, neighbors: Array<Cell>): CellState {
        val alive = cell.currentState.toInt() != 0
        var liveNeighbors = 0
        for (n in neighbors) {
            if (n.currentState.toInt() != 0) liveNeighbors++
        }
        val nextAlive = if (alive) {
            liveNeighbors == 2 || liveNeighbors == 3
        } else {
            liveNeighbors == 3
        }
        return IntegerCellState(if (nextAlive) 1 else 0)
    }

    override fun createInitialState(): CellState = IntegerCellState(0)
}

// =============================================================================
// 4. ComplexLife — Life using complex numbers with quadratic averaging
// =============================================================================

/**
 * Life-like simulation using complex numbers. Takes the average of the cell
 * and its neighbors, applies the equation: slope * avg² + yIntercept,
 * then keeps only the fractional modulus (within the unit circle).
 *
 * Default slope = 0.9+0.9i, yIntercept = 0.39+0.98i.
 *
 * Port of Java ComplexLife.java (ComplexRuleTemplate).
 */
class ComplexLife(
    private val slopeRe: Double = 0.9,
    private val slopeIm: Double = 0.9,
    private val interceptRe: Double = 0.39,
    private val interceptIm: Double = 0.98
) : ComplexRule() {
    override val displayName = "Complex Life"
    override val description = "Life-like CA using complex numbers with quadratic averaging"
    override val category = RuleCategory.COMPLEX
    override val compatibleLatticeNames = listOf("Square (Moore)", "Square (Von Neumann)")

    override fun nextState(cell: Cell, neighbors: Array<Cell>): CellState {
        val cellVal = getComplex(cell)
        val slope = Complex(slopeRe, slopeIm)
        val yIntercept = Complex(interceptRe, interceptIm)

        // Average the cell and its neighbors
        var avgRe = cellVal.real
        var avgIm = cellVal.imaginary
        for (n in neighbors) {
            val nv = getComplex(n)
            avgRe += nv.real
            avgIm += nv.imaginary
        }
        val count = neighbors.size + 1.0
        val avg = Complex(avgRe / count, avgIm / count)

        // y = slope * avg² + yIntercept
        val avgSquared = avg * avg
        val transformed = slope * avgSquared + yIntercept

        // Keep fractional modulus: same angle, mod(modulus) within unit circle
        val theta = transformed.argument
        val newModulus = transformed.modulus - floor(transformed.modulus)
        val tanTheta = tan(theta)
        val newReal = newModulus / sqrt(tanTheta * tanTheta + 1.0)
        val newImaginary = newReal * tanTheta

        return makeState(Complex(newReal, newImaginary))
    }

    override fun createInitialState(): CellState = makeState(Complex.ZERO)

    override val properties get() = listOf(
        RuleProperty.FloatProperty("slopeRe", "Slope Re", slopeRe.toFloat(), -10f, 10f),
        RuleProperty.FloatProperty("slopeIm", "Slope Im", slopeIm.toFloat(), -10f, 10f),
        RuleProperty.FloatProperty("interceptRe", "Intercept Re", interceptRe.toFloat(), -2f, 2f),
        RuleProperty.FloatProperty("interceptIm", "Intercept Im", interceptIm.toFloat(), -2f, 2f)
    )

    override fun withProperty(key: String, value: Any): Rule = when (key) {
        "slopeRe" -> ComplexLife((value as Number).toDouble(), slopeIm, interceptRe, interceptIm)
        "slopeIm" -> ComplexLife(slopeRe, (value as Number).toDouble(), interceptRe, interceptIm)
        "interceptRe" -> ComplexLife(slopeRe, slopeIm, (value as Number).toDouble(), interceptIm)
        "interceptIm" -> ComplexLife(slopeRe, slopeIm, interceptRe, (value as Number).toDouble())
        else -> this
    }

    companion object {
        private fun getComplex(cell: Cell): Complex {
            val state = cell.currentState
            return if (state is ComplexCellState) state.state else Complex.ZERO
        }

        private fun makeState(c: Complex): ComplexCellState = ComplexCellState(
            state = c,
            alternateStateValue = Complex(0.5, 0.5),
            emptyStateValue = Complex.ZERO,
            fullStateValue = Complex(1.0, 0.0)
        )
    }
}

// =============================================================================
// 5. ComplexContinuousCA — Complex continuous CA
// =============================================================================

/**
 * Complex continuous CA. Takes the average of the cell and its neighbors,
 * applies a linear equation: slope * avg + yIntercept, then keeps
 * the fractional modulus within the unit circle, fixing the sign of
 * the real part based on the angle.
 *
 * Default slope = 1.0+1.0i, yIntercept = 0.6+0.5i.
 *
 * Port of Java ComplexContinuousCA.java (ComplexRuleTemplate).
 */
class ComplexContinuousCA(
    private val slopeRe: Double = 1.0,
    private val slopeIm: Double = 1.0,
    private val interceptRe: Double = 0.6,
    private val interceptIm: Double = 0.5
) : ComplexRule() {
    override val displayName = "Complex Continuous CA"
    override val description = "Continuous CA with complex-valued linear equation"
    override val category = RuleCategory.COMPLEX
    override val compatibleLatticeNames = listOf("Square (Moore)", "Square (Von Neumann)", "1D (radius 1)")

    override fun nextState(cell: Cell, neighbors: Array<Cell>): CellState {
        val cellVal = getComplex(cell)
        val slope = Complex(slopeRe, slopeIm)
        val yIntercept = Complex(interceptRe, interceptIm)

        // Average the cell and its neighbors
        var avgRe = cellVal.real
        var avgIm = cellVal.imaginary
        for (n in neighbors) {
            val nv = getComplex(n)
            avgRe += nv.real
            avgIm += nv.imaginary
        }
        val count = neighbors.size + 1.0
        val avg = Complex(avgRe / count, avgIm / count)

        // y = slope * avg + yIntercept
        val transformed = slope * avg + yIntercept

        // Keep fractional modulus within unit circle
        val theta = transformed.argument
        val newModulus = transformed.modulus - floor(transformed.modulus)
        val tanTheta = tan(theta)
        var newReal = newModulus / sqrt(tanTheta * tanTheta + 1.0)
        val newImaginary = newReal * tanTheta

        // Fix the sign of real part based on angle
        if ((theta > PI / 2.0 && theta < 1.5 * PI)
            || (theta < -PI / 2.0 && theta > -1.5 * PI)
        ) {
            newReal *= -1.0
        }

        return makeState(Complex(newReal, newImaginary))
    }

    override fun createInitialState(): CellState = makeState(Complex.ZERO)

    override val properties get() = listOf(
        RuleProperty.FloatProperty("slopeRe", "Slope Re", slopeRe.toFloat(), -10f, 10f),
        RuleProperty.FloatProperty("slopeIm", "Slope Im", slopeIm.toFloat(), -10f, 10f),
        RuleProperty.FloatProperty("interceptRe", "Intercept Re", interceptRe.toFloat(), -2f, 2f),
        RuleProperty.FloatProperty("interceptIm", "Intercept Im", interceptIm.toFloat(), -2f, 2f)
    )

    override fun withProperty(key: String, value: Any): Rule = when (key) {
        "slopeRe" -> ComplexContinuousCA((value as Number).toDouble(), slopeIm, interceptRe, interceptIm)
        "slopeIm" -> ComplexContinuousCA(slopeRe, (value as Number).toDouble(), interceptRe, interceptIm)
        "interceptRe" -> ComplexContinuousCA(slopeRe, slopeIm, (value as Number).toDouble(), interceptIm)
        "interceptIm" -> ComplexContinuousCA(slopeRe, slopeIm, interceptRe, (value as Number).toDouble())
        else -> this
    }

    companion object {
        private fun getComplex(cell: Cell): Complex {
            val state = cell.currentState
            return if (state is ComplexCellState) state.state else Complex.ZERO
        }

        private fun makeState(c: Complex): ComplexCellState = ComplexCellState(
            state = c,
            alternateStateValue = Complex(0.5, 0.5),
            emptyStateValue = Complex.ZERO,
            fullStateValue = Complex(1.0, 0.0)
        )
    }
}

// =============================================================================
// 6. AlternateContinuousCA — Alternate continuous variant
// =============================================================================

/**
 * Takes the average of the neighbors (not including the cell),
 * applies y = slope * avg + yIntercept, keeps only the fractional part.
 * Similar to ContinuousCA but excludes the cell from the average.
 *
 * Default slope = 1.40, yIntercept = 0.99.
 *
 * Port of Java AlternateContinuousCA.java (RealRuleTemplate).
 */
class AlternateContinuousCA(
    private val slope: Double = 1.40,
    private val yIntercept: Double = 0.99
) : RealRule() {
    override val displayName = "Alternate Continuous CA"
    override val description = "Continuous CA using linear equation on neighbor average (excludes cell)"
    override val category = RuleCategory.CONTINUOUS
    override val compatibleLatticeNames = listOf("Square (Moore)", "Square (Von Neumann)", "1D (radius 1)")

    override fun nextState(cell: Cell, neighbors: Array<Cell>): CellState {
        // Average the neighbors only (not the cell itself)
        var avg = 0.0
        for (n in neighbors) {
            avg += (n.currentState as? RealValuedState)?.state ?: n.currentState.toInt().toDouble()
        }
        // Original divides by (neighbors.length + 1) — matching Java behavior
        avg /= (neighbors.size + 1)

        val returnValue = slope * avg + yIntercept
        return RealValuedState(returnValue - floor(returnValue))
    }

    override fun createInitialState(): CellState = RealValuedState(0.0)

    override val properties get() = listOf(
        RuleProperty.FloatProperty("slope", "Slope", slope.toFloat(), -10f, 10f, "Slope of the linear equation"),
        RuleProperty.FloatProperty("yIntercept", "Y-Intercept", yIntercept.toFloat(), 0f, 1f, "Y-intercept of the equation")
    )

    override fun withProperty(key: String, value: Any): Rule = when (key) {
        "slope" -> AlternateContinuousCA((value as Number).toDouble(), yIntercept)
        "yIntercept" -> AlternateContinuousCA(slope, (value as Number).toDouble())
        else -> this
    }
}

// =============================================================================
// 7. ReversibleRuleNumber — Reversible Wolfram rules using XOR
// =============================================================================

/**
 * Second-order reversible CA. Applies a standard Wolfram rule then XORs
 * the result with the cell's state from the previous generation, making
 * the automaton reversible.
 *
 * Port of Java ReversibleRuleNumber.java (MultiGenerationBinaryRuleTemplate).
 */
class ReversibleRuleNumber(val ruleNumber: Int = 90) : BinaryRule() {
    override val displayName = "Reversible Rule $ruleNumber"
    override val description = "Reversible (second-order) Wolfram rule $ruleNumber"
    override val category = RuleCategory.ELEMENTARY
    override val compatibleLatticeNames = listOf("1D (radius 1)")
    override val requiredGenerations = 2

    private val lookupTable: IntArray = IntArray(8) { i ->
        (ruleNumber shr i) and 1
    }

    override fun nextState(cell: Cell, neighbors: Array<Cell>): CellState {
        val left = neighbors[0].currentState.toInt()
        val center = cell.currentState.toInt()
        val right = neighbors[1].currentState.toInt()

        // Standard Wolfram rule lookup
        val index = (left shl 2) or (center shl 1) or right
        val regularAnswer = lookupTable[index]

        // XOR with previous generation's state to make reversible
        val previousState = cell.previousState?.toInt() ?: 0
        return IntegerCellState(regularAnswer xor previousState)
    }

    override fun createInitialState(): CellState = IntegerCellState(0)

    override val properties get() = listOf(
        RuleProperty.IntProperty("ruleNumber", "Rule Number", ruleNumber, 0, 255, "Wolfram rule number (0-255)")
    )

    override fun withProperty(key: String, value: Any): Rule = when (key) {
        "ruleNumber" -> ReversibleRuleNumber((value as Number).toInt().coerceIn(0, 255))
        else -> this
    }
}

// =============================================================================
// 8. MajorityWins — Cell becomes most common neighbor state
// =============================================================================

/**
 * Majority voting rule. The cell adopts the state most common among its
 * neighbors (excluding itself). Ties are broken randomly.
 *
 * Port of Java MajorityWins.java (IntegerRuleTemplate).
 */
class MajorityWins(override val numStates: Int = 2) : IntegerRule() {
    override val displayName = "Majority Wins"
    override val description = "Cell adopts the majority state among its neighbors"
    override val category = RuleCategory.SOCIAL
    override val compatibleLatticeNames = listOf("Square (Moore)", "Square (Von Neumann)")

    override fun nextState(cell: Cell, neighbors: Array<Cell>): CellState {
        val counts = IntArray(numStates)
        for (n in neighbors) {
            val s = n.currentState.toInt().coerceIn(0, numStates - 1)
            counts[s]++
        }

        // Find max count and collect all states with that count
        var maxCount = 0
        for (c in counts) if (c > maxCount) maxCount = c

        val winners = mutableListOf<Int>()
        for (i in 0 until numStates) {
            if (counts[i] == maxCount) winners.add(i)
        }

        val winner = if (winners.size == 1) winners[0]
        else winners[Random.nextInt(winners.size)]
        return IntegerCellState(winner)
    }

    override fun createInitialState(): CellState = IntegerCellState(0)

    override val properties get() = listOf(
        RuleProperty.IntProperty("numStates", "States", numStates, 2, 256)
    )
    override fun withProperty(key: String, value: Any): Rule = when (key) {
        "numStates" -> MajorityWins((value as Number).toInt().coerceIn(2, 256))
        else -> this
    }
}

// =============================================================================
// 9. MinorityWins — Cell becomes least common neighbor state
// =============================================================================

/**
 * Minority voting rule. The cell adopts the state least common among its
 * neighbors (excluding itself). Ties are broken randomly.
 *
 * Port of Java MinorityWins.java (IntegerRuleTemplate).
 */
class MinorityWins(override val numStates: Int = 2) : IntegerRule() {
    override val displayName = "Minority Wins"
    override val description = "Cell adopts the minority state among its neighbors"
    override val category = RuleCategory.SOCIAL
    override val compatibleLatticeNames = listOf("Square (Moore)", "Square (Von Neumann)")

    override fun nextState(cell: Cell, neighbors: Array<Cell>): CellState {
        val counts = IntArray(numStates)
        for (n in neighbors) {
            val s = n.currentState.toInt().coerceIn(0, numStates - 1)
            counts[s]++
        }

        // Find min count and collect all states with that count
        var minCount = Int.MAX_VALUE
        for (c in counts) if (c < minCount) minCount = c

        val winners = mutableListOf<Int>()
        for (i in 0 until numStates) {
            if (counts[i] == minCount) winners.add(i)
        }

        val winner = if (winners.size == 1) winners[0]
        else winners[Random.nextInt(winners.size)]
        return IntegerCellState(winner)
    }

    override fun createInitialState(): CellState = IntegerCellState(0)

    override val properties get() = listOf(
        RuleProperty.IntProperty("numStates", "States", numStates, 2, 256)
    )
    override fun withProperty(key: String, value: Any): Rule = when (key) {
        "numStates" -> MinorityWins((value as Number).toInt().coerceIn(2, 256))
        else -> this
    }
}

// =============================================================================
// 10. MajorityProbablyWins — Probabilistic majority voting
// =============================================================================

/**
 * Probabilistic majority voting rule. Each state's probability of being
 * chosen is proportional to its frequency among the cell and its neighbors.
 * The cell itself IS included in the count.
 *
 * Port of Java MajorityProbablyWins.java (IntegerRuleTemplate).
 */
class MajorityProbablyWins(override val numStates: Int = 2) : IntegerRule() {
    override val displayName = "Majority Probably Wins"
    override val description = "Probabilistic voting where odds match neighbor proportions"
    override val category = RuleCategory.PROBABILISTIC
    override val compatibleLatticeNames = listOf("Square (Moore)", "Square (Von Neumann)")

    override fun nextState(cell: Cell, neighbors: Array<Cell>): CellState {
        val counts = IntArray(numStates)
        for (n in neighbors) {
            val s = n.currentState.toInt().coerceIn(0, numStates - 1)
            counts[s]++
        }
        // Include the cell itself
        val cellState = cell.currentState.toInt().coerceIn(0, numStates - 1)
        counts[cellState]++

        val total = (neighbors.size + 1).toDouble()

        // Build cumulative probability
        val cumProb = DoubleArray(numStates)
        cumProb[0] = counts[0] / total
        for (i in 1 until numStates) {
            cumProb[i] = cumProb[i - 1] + counts[i] / total
        }

        // Pick based on random number
        val r = Random.nextDouble()
        var j = 0
        while (j < numStates - 1 && r > cumProb[j]) j++

        return IntegerCellState(j)
    }

    override fun createInitialState(): CellState = IntegerCellState(0)

    override val properties get() = listOf(
        RuleProperty.IntProperty("numStates", "States", numStates, 2, 256)
    )
    override fun withProperty(key: String, value: Any): Rule = when (key) {
        "numStates" -> MajorityProbablyWins((value as Number).toInt().coerceIn(2, 256))
        else -> this
    }
}

// =============================================================================
// 11. PistonPrime — Life variant with prime survival
// =============================================================================

/**
 * Life variant using LifeExtensionsTemplate pattern.
 * Birth on exactly 3 neighbors. Survive if the number of live neighbors
 * is prime (2, 3, 5, 7, 11, 13, ...). Non-surviving live cells decay
 * through states 2, 3, ... numStates-1 before dying (reaching 0).
 *
 * Port of Java PistonPrime.java (LifeExtensionsTemplate).
 */
class PistonPrime(override val numStates: Int = 4) : IntegerRule() {
    override val displayName = "Piston Prime"
    override val description = "Life variant: birth on 3, survive on prime neighbor counts"
    override val category = RuleCategory.LIFE_LIKE
    override val compatibleLatticeNames = listOf("Square (Moore)")

    private val birthValues = intArrayOf(3)
    private val survivalValues = intArrayOf(2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37)

    override fun nextState(cell: Cell, neighbors: Array<Cell>): CellState {
        val cellValue = cell.currentState.toInt()

        // Count neighbors with state == 1
        var numberOfOnes = 0
        for (n in neighbors) {
            if (n.currentState.toInt() == 1) numberOfOnes++
        }

        val returnValue: Int = when {
            // Dead cell: check for birth
            cellValue == 0 -> {
                if (numberOfOnes in birthValues) 1 else 0
            }
            // Alive cell (state 1): check for survival
            cellValue == 1 -> {
                if (numberOfOnes in survivalValues) 1
                else (cellValue + 1) % numStates // begin death process
            }
            // Dying cell (states 2..numStates-1): continue death
            else -> (cellValue + 1) % numStates
        }
        return IntegerCellState(returnValue)
    }

    override fun createInitialState(): CellState = IntegerCellState(0)
}

// =============================================================================
// 12. House — House-shaped patterns using 57 states
// =============================================================================

/**
 * Uses 57 states to create house-shaped patterns from a single seed.
 * Each state transition depends on specific neighbor index positions,
 * building the house structure cell by cell.
 *
 * Port of Java House.java (IntegerRuleTemplate).
 * Requires a Square (Moore) lattice with 8 neighbors indexed 0-7.
 */
class House : IntegerRule() {
    override val numStates = 57
    override val displayName = "House"
    override val description = "Creates house shapes using 57 states from a single seed"
    override val category = RuleCategory.OTHER
    override val compatibleLatticeNames = listOf("Square (Moore)")

    override fun nextState(cell: Cell, neighbors: Array<Cell>): CellState {
        val c = cell.currentState.toInt()
        if (neighbors.size < 8) return IntegerCellState(c)

        val n = IntArray(8) { neighbors[it].currentState.toInt() }

        val result = when {
            c == 56 -> 56
            n[2] == 56 -> 55
            n[0] == 56 -> 54
            n[5] == 48 -> 53
            n[0] == 54 -> 52
            n[2] == 55 -> 51
            n[2] == 51 -> 50
            n[0] == 52 -> 49
            n[7] == 49 -> 48
            n[0] == 49 && n[1] == 48 -> 47
            n[2] == 50 -> 46
            n[2] == 46 -> 45
            n[0] == 47 -> 44
            n[0] == 44 -> 43
            n[1] == 44 -> 42
            n[5] == 34 -> 41
            n[3] == 41 -> 40
            n[3] == 40 -> 39
            n[1] == 45 -> 38
            n[2] == 45 -> 37
            n[0] == 37 && n[1] == 38 -> 36
            n[1] == 39 && n[2] == 40 -> 35
            n[5] == 31 -> 34
            n[1] == 42 && n[2] == 43 -> 33
            n[1] == 33 -> 32
            n[4] == 26 -> 31
            n[3] == 31 -> 30
            n[3] == 30 -> 29
            n[1] == 36 -> 28
            n[1] == 28 -> 27
            n[3] == 25 -> 26
            n[3] == 24 -> 25
            n[5] == 21 -> 24
            n[1] == 32 -> 23
            n[1] == 23 -> 22
            n[5] == 16 -> 21
            n[1] == 26 -> 20
            n[1] == 27 -> 19
            n[1] == 19 -> 18
            n[1] == 20 -> 17
            n[5] == 13 -> 16
            n[1] == 22 -> 15
            n[1] == 15 -> 14
            n[5] == 3 -> 13
            n[1] == 17 -> 12
            n[1] == 18 -> 11
            n[1] == 11 -> 10
            n[0] == 11 -> 9
            n[7] == 9 -> 8
            n[7] == 8 -> 7
            n[7] == 7 -> 6
            n[7] == 6 -> 5
            n[1] == 12 -> 4
            n[3] == 2 -> 3
            n[2] == 14 -> 2
            n[1] == 14 -> 1
            else -> 0
        }
        return IntegerCellState(result)
    }

    override fun createInitialState(): CellState = IntegerCellState(0)
}

// =============================================================================
// 13. ChutesLaddersAndShifts — Directional flow rule
// =============================================================================

/**
 * Concatenates neighbor values (with cell in the middle) as a base-numStates
 * number, converts to base 10, takes modulo (numNeighbors+1), and returns
 * the value at that position in the combined array.
 *
 * Port of Java ChutesLaddersAndShifts.java (IntegerRuleTemplate).
 */
class ChutesLaddersAndShifts(override val numStates: Int = 35) : IntegerRule() {
    override val displayName = "Chutes, Ladders, and Shifts"
    override val description = "Concatenation-based rule producing chutes and ladders"
    override val category = RuleCategory.OTHER
    override val compatibleLatticeNames = listOf("Square (Moore)", "Square (Von Neumann)", "1D (radius 1)")

    override fun nextState(cell: Cell, neighbors: Array<Cell>): CellState {
        val cellValue = cell.currentState.toInt().coerceIn(0, numStates - 1)

        // Build combined array with cell in the middle
        val middlePosition = neighbors.size / 2
        val combined = IntArray(neighbors.size + 1)
        for (i in 0 until middlePosition) {
            combined[i] = neighbors[i].currentState.toInt().coerceIn(0, numStates - 1)
        }
        combined[middlePosition] = cellValue
        for (i in middlePosition + 1 until combined.size) {
            combined[i] = neighbors[i - 1].currentState.toInt().coerceIn(0, numStates - 1)
        }

        // Convert combined array from base-numStates to a big number, then mod by combined.size
        var number = 0L
        var placeValue = 1L
        for (i in combined.indices.reversed()) {
            number += combined[i] * placeValue
            placeValue *= numStates
            // Prevent overflow by taking mod early
            if (placeValue > Long.MAX_VALUE / numStates) {
                number %= combined.size
                placeValue = 1L
            }
        }
        val neighborPosition = (number % combined.size).toInt().coerceIn(0, combined.size - 1)

        return IntegerCellState(combined[neighborPosition])
    }

    override fun createInitialState(): CellState = IntegerCellState(0)

    override val properties get() = listOf(
        RuleProperty.IntProperty("numStates", "States", numStates, 2, 36, "Number of states (max 36)")
    )
    override fun withProperty(key: String, value: Any): Rule = when (key) {
        "numStates" -> ChutesLaddersAndShifts((value as Number).toInt().coerceIn(2, 36))
        else -> this
    }
}

// =============================================================================
// 14. CoolClassIV — Class IV automaton (totalistic rule 1329, 3 states)
// =============================================================================

/**
 * Class IV CA with 3 states using totalistic rule number 1329.
 * The rule sums the cell and all neighbors, then uses a lookup table
 * derived from the rule number in base 3.
 *
 * Port of Java CoolClassIV.java (extends Totalistic).
 */
class CoolClassIV : IntegerRule() {
    override val numStates = 3
    override val displayName = "Cool Class IV"
    override val description = "Wolfram Class IV totalistic rule 1329 with 3 states"
    override val category = RuleCategory.TOTALISTIC
    override val compatibleLatticeNames = listOf("1D (radius 1)")

    // Rule 1329 in base 3 gives the lookup table: total_sum -> new_state
    // Max sum for 1D radius-1 (3 cells) with 3 states = 2*3 = 6
    private val lookupTable: IntArray

    init {
        val maxSum = 6 // (2+1) cells * (3-1) max value
        lookupTable = IntArray(maxSum + 1)
        var ruleNum = 1329L
        for (i in 0..maxSum) {
            lookupTable[i] = (ruleNum % numStates).toInt()
            ruleNum /= numStates
        }
    }

    override fun nextState(cell: Cell, neighbors: Array<Cell>): CellState {
        var total = cell.currentState.toInt()
        for (n in neighbors) total += n.currentState.toInt()
        val clampedTotal = total.coerceIn(0, lookupTable.size - 1)
        return IntegerCellState(lookupTable[clampedTotal])
    }

    override fun createInitialState(): CellState = IntegerCellState(0)
}

// =============================================================================
// 15. PrettyClassIV — Pretty Class IV (outer totalistic, 4 states)
// =============================================================================

/**
 * Class IV CA with 4 states using a very large outer totalistic rule number.
 * The rule concatenates neighbor values (not totalistic sum) and uses a
 * lookup table from the rule number in base 4.
 *
 * Since the original rule number (27645726470709879688601734854573817858) exceeds
 * Long range, we use a simplified approach: sum neighbors and cell, apply
 * a modular lookup.
 *
 * Port of Java PrettyClassIV.java (extends RuleNumber — outer totalistic by position).
 */
class PrettyClassIV : IntegerRule() {
    override val numStates = 4
    override val displayName = "Pretty Class IV"
    override val description = "Meandering Class IV outer totalistic rule with 4 states"
    override val category = RuleCategory.TOTALISTIC
    override val compatibleLatticeNames = listOf("1D (radius 1)")

    // Precomputed lookup table from the rule number in base 4.
    // 4 states, 3 cells (radius 1): 4^3 = 64 possible neighborhoods.
    // Rule number digits (base 4) for index 0..63.
    // The original rule number 27645726470709879688601734854573817858 in base 4:
    private val lookupTable: IntArray

    init {
        // Parse the huge rule number and extract base-4 digits
        val ruleStr = "27645726470709879688601734854573817858"
        val tableSize = 64 // 4^3 neighborhoods
        lookupTable = IntArray(tableSize)
        var digits = mutableListOf<Int>()
        // Manual big-number base conversion
        var chars = ruleStr.toCharArray().toMutableList()
        while (chars.isNotEmpty() && digits.size < tableSize) {
            var remainder = 0
            val newChars = mutableListOf<Char>()
            for (ch in chars) {
                val digit = remainder * 10 + (ch - '0')
                val quotient = digit / 4
                remainder = digit % 4
                if (newChars.isNotEmpty() || quotient > 0) {
                    newChars.add('0' + quotient)
                }
            }
            digits.add(remainder)
            chars = newChars
        }
        for (i in lookupTable.indices) {
            lookupTable[i] = if (i < digits.size) digits[i] % numStates else 0
        }
    }

    override fun nextState(cell: Cell, neighbors: Array<Cell>): CellState {
        // For 1D radius-1: neighbors are [left, right], cell in middle
        // Concatenate as: left | cell | right → index in base 4
        val cellValue = cell.currentState.toInt().coerceIn(0, numStates - 1)
        var index = 0
        // Build index: iterate through neighbors in order, with cell appended at end
        for (n in neighbors) {
            index = index * numStates + n.currentState.toInt().coerceIn(0, numStates - 1)
        }
        index = index * numStates + cellValue
        val clampedIndex = index.coerceIn(0, lookupTable.size - 1)
        return IntegerCellState(lookupTable[clampedIndex])
    }

    override fun createInitialState(): CellState = IntegerCellState(0)
}

// =============================================================================
// 16. SatansStaircase — Bifurcation staircase triangles
// =============================================================================

/**
 * Creates 3D fractal staircase triangles. Uses outer totalistic rule number
 * 4067213884 with 2 states on a 1D next-nearest-neighbor lattice (radius 2).
 * Concatenates neighbor+cell values, looks up in a table derived from the
 * rule number.
 *
 * Port of Java SatansStaircase.java (extends RuleNumber).
 */
class SatansStaircase : IntegerRule() {
    override val numStates = 2
    override val displayName = "Satan's Staircase"
    override val description = "3D fractal triangular staircase patterns"
    override val category = RuleCategory.OTHER
    override val compatibleLatticeNames = listOf("1D (radius 2)")

    // Rule 4067213884 in base 2. For radius-2 (5 cells): 2^5 = 32 entries.
    private val lookupTable: IntArray

    init {
        val ruleNumber = 4067213884L
        val tableSize = 32
        lookupTable = IntArray(tableSize)
        for (i in 0 until tableSize) {
            lookupTable[i] = ((ruleNumber shr i) and 1L).toInt()
        }
    }

    override fun nextState(cell: Cell, neighbors: Array<Cell>): CellState {
        val cellValue = cell.currentState.toInt().coerceIn(0, 1)
        // Build index by concatenating neighbors + cell (as per RuleNumber convention)
        var index = 0
        for (n in neighbors) {
            index = index * numStates + n.currentState.toInt().coerceIn(0, 1)
        }
        index = index * numStates + cellValue
        val clampedIndex = index.coerceIn(0, lookupTable.size - 1)
        return IntegerCellState(lookupTable[clampedIndex])
    }

    override fun createInitialState(): CellState = IntegerCellState(0)
}

// 17. TuringMachine — See #20 below (implemented after initial port).

// =============================================================================
// 18. CellularMarketModel — Economic market simulation
// =============================================================================

/**
 * Simplified cellular market model. Each cell represents a trader who holds
 * one commodity (state). The rule uses probabilistic majority voting with
 * a configurable temperature (volatility) and noise level.
 *
 * Higher temperature = more random behavior. Lower temperature = more
 * deterministic majority following. Noise adds random state changes.
 *
 * Simplified port of Java CellularMarketModel.java (IntegerRuleTemplate).
 * The original relied on global lattice iteration and GUI sliders; this
 * version uses configurable properties instead.
 */
class CellularMarketModel(
    override val numStates: Int = 10,
    private val temperature: Double = 1.0,
    private val noise: Double = 0.0
) : IntegerRule() {
    override val displayName = "Cellular Market Model"
    override val description = "Commodities market model with probabilistic herding and noise"
    override val category = RuleCategory.SOCIAL
    override val compatibleLatticeNames = listOf("Square (Moore)", "Square (Von Neumann)")

    override fun nextState(cell: Cell, neighbors: Array<Cell>): CellState {
        val cellState = cell.currentState.toInt().coerceIn(0, numStates - 1)
        val counts = IntArray(numStates)
        for (n in neighbors) {
            val s = n.currentState.toInt().coerceIn(0, numStates - 1)
            counts[s]++
        }
        counts[cellState]++
        val total = (neighbors.size + 1).toDouble()

        var cellValue = cellState

        if (temperature != 0.0) {
            // Boltzmann-weighted probabilistic voting
            val prob = DoubleArray(numStates) { i ->
                (counts[i].toDouble() / total).pow(1.0 / temperature)
            }
            val z = prob.sum()
            for (i in prob.indices) prob[i] /= z

            // Cumulative probability
            val cumProb = DoubleArray(numStates)
            cumProb[0] = prob[0]
            for (i in 1 until numStates) cumProb[i] = cumProb[i - 1] + prob[i]

            val r = Random.nextDouble()
            var j = 0
            while (j < numStates - 1 && r > cumProb[j]) j++
            cellValue = j
        }

        // Add noise
        if (noise > 0.0 && Random.nextDouble() < noise) {
            cellValue = Random.nextInt(numStates)
        }

        return IntegerCellState(cellValue)
    }

    override fun createInitialState(): CellState = IntegerCellState(0)

    override val properties get() = listOf(
        RuleProperty.IntProperty("numStates", "States", numStates, 2, 50),
        RuleProperty.FloatProperty("temperature", "Temperature", temperature.toFloat(), 0f, 2f, "Social temperature (volatility)"),
        RuleProperty.FloatProperty("noise", "Noise", noise.toFloat(), 0f, 1f, "Probability of random state change")
    )

    override fun withProperty(key: String, value: Any): Rule = when (key) {
        "numStates" -> CellularMarketModel((value as Number).toInt().coerceIn(2, 50), temperature, noise)
        "temperature" -> CellularMarketModel(numStates, (value as Number).toDouble(), noise)
        "noise" -> CellularMarketModel(numStates, temperature, (value as Number).toDouble())
        else -> this
    }
}

// =============================================================================
// 19. RealSort — Sorting for real-valued cells
// =============================================================================

/**
 * Sorts real numbers by comparing adjacent pairs. Each cell compares its
 * value with its neighbors: if a neighbor to the "right" (higher index)
 * is smaller, they swap. This adapts the original Margolus block sort
 * to a neighbor-based rule.
 *
 * Port of Java RealSort.java (OneDimensionalRealMargolusTemplate).
 */
class RealSort : RealRule() {
    override val displayName = "Sort Reals"
    override val description = "Parallel O(N) sorting of real-valued cells"
    override val category = RuleCategory.OTHER
    override val compatibleLatticeNames = listOf("1D (radius 1)", "Square (Moore)")

    override fun nextState(cell: Cell, neighbors: Array<Cell>): CellState {
        val current = (cell.currentState as? RealValuedState)?.state
            ?: cell.currentState.toInt().toDouble()

        if (neighbors.isEmpty()) return RealValuedState(current)

        // Compare with a neighbor and swap if out of order
        // Use generation parity to alternate which pair is compared (even/odd Margolus)
        val gen = cell.generation
        val neighborIdx = gen % neighbors.size
        val neighborVal = (neighbors[neighborIdx].currentState as? RealValuedState)?.state
            ?: neighbors[neighborIdx].currentState.toInt().toDouble()

        // For 1D: if this cell's coordinate is at even position and neighbor to right is smaller,
        // take the smaller value (simulate swap)
        val row = cell.coordinate.row
        val col = cell.coordinate.col
        val position = row + col

        return if ((position + gen) % 2 == 0) {
            // This cell acts as the "west" cell in a Margolus block
            RealValuedState(minOf(current, neighborVal))
        } else {
            // This cell acts as the "east" cell in a Margolus block
            RealValuedState(maxOf(current, neighborVal))
        }
    }

    override fun createInitialState(): CellState = RealValuedState(0.0)
}

// =============================================================================
// 20. ChainLinkFence — Builds a chain link fence from a seed
// =============================================================================

/**
 * From a single seed, builds a chain link fence pattern. Cells grow by
 * incrementing from their smallest non-zero Von Neumann neighbor. When
 * reaching numStates, they wrap to 1. Cells that see a smaller diagonal
 * neighbor become 0 (empty space between links).
 *
 * Port of Java ChainLinkFence.java (IntegerRuleTemplate).
 */
class ChainLinkFence(override val numStates: Int = 10) : IntegerRule() {
    override val displayName = "Chain Link Fence"
    override val description = "Creates a chain link fence pattern from a single seed"
    override val category = RuleCategory.OTHER
    override val compatibleLatticeNames = listOf("Square (Moore)")

    override fun nextState(cell: Cell, neighbors: Array<Cell>): CellState {
        val c = cell.currentState.toInt()
        if (neighbors.size < 8) return IntegerCellState(c)

        // Von Neumann neighbors: N=1, E=3, S=5, W=7
        val northNeighbor = neighbors[1].currentState.toInt()
        val eastNeighbor = neighbors[3].currentState.toInt()
        val southNeighbor = neighbors[5].currentState.toInt()
        val westNeighbor = neighbors[7].currentState.toInt()
        val nesw = intArrayOf(northNeighbor, eastNeighbor, southNeighbor, westNeighbor)

        // Find min of N, E, S, W neighbors (excluding 0)
        var minOfNESW = numStates
        for (v in nesw) {
            if (v in 1 until minOfNESW) minOfNESW = v
        }
        minOfNESW %= numStates

        // Find min of all 8 neighbors (excluding 0)
        var minOfAll = numStates
        for (n in neighbors) {
            val v = n.currentState.toInt()
            if (v in 1 until minOfAll) minOfAll = v
        }
        minOfAll %= numStates

        val newValue = when {
            c == 1 -> 1 // State 1 stays
            minOfNESW != 0 && minOfAll >= minOfNESW -> {
                val next = minOfNESW + 1
                if (next == numStates) 1 else next
            }
            minOfAll < minOfNESW -> 0 // Smaller diagonal → empty space
            else -> 0
        }
        return IntegerCellState(newValue)
    }

    override fun createInitialState(): CellState = IntegerCellState(0)

    override val properties get() = listOf(
        RuleProperty.IntProperty("numStates", "States", numStates, 3, 256, "Number of states (distance between links)")
    )
    override fun withProperty(key: String, value: Any): Rule = when (key) {
        "numStates" -> ChainLinkFence((value as Number).toInt().coerceIn(3, 256))
        else -> this
    }
}

// =============================================================================
// 20. Turing Machine — A CA-based Turing Machine
// =============================================================================

/**
 * Turing Machine implemented as a cellular automaton.
 *
 * The lattice is the tape. Cell states 0..(numStates-2) are tape symbols.
 * State (numStates-1) is the tape head marker.
 *
 * A finite-state controller is tracked externally. Each generation:
 * 1. The head cell writes a new symbol (replaces head marker with write value)
 * 2. The cell in the move direction becomes the new head
 * 3. The finite state transitions based on the read symbol
 *
 * Each cell's nextState is purely local: it checks whether it IS the old head
 * (write the tape symbol) or whether the head is an adjacent neighbor that
 * wants to move HERE (become the new head). All other cells stay unchanged.
 *
 * MP lattice Moore neighbor order: 0=NW, 1=N, 2=NE, 3=E, 4=SE, 5=S, 6=SW, 7=W
 *
 * Port of Java TuringMachine.java by Kaleb Kircher, extended with new programs.
 */
class TuringMachine(
    override val numStates: Int = 4,
    private val programName: String = "Counting"
) : IntegerRule() {
    override val displayName = "Turing Machine"
    override val description: String get() {
        val halts = programName in listOf(
            "Busy Beaver #1", "Busy Beaver #2",
            "Classic BB-3", "Classic BB-4",
            "Subtraction"
        )
        val suffix = if (halts) " (halts after finite steps)" else ""
        return "Turing machine on a CA lattice — $programName$suffix"
    }
    override val category = RuleCategory.OTHER
    override val compatibleLatticeNames = listOf("Square (Moore)")
    override val preferredInit = "center_seed"

    // Transition table: [finiteState][readSymbol] → (writeSymbol, moveDirIndex, nextFiniteState)
    // moveDirIndex uses MP ordering: 0=NW, 1=N, 2=NE, 3=E, 4=SE, 5=S, 6=SW, 7=W
    private val transitions: Array<Array<Triple<Int, Int, Int>>>
    private val haltFlags: Array<BooleanArray>

    init {
        val symbols = numStates - 1
        val numFiniteStates = 20
        transitions = Array(numFiniteStates) { Array(symbols) { Triple(0, 3, 0) } }
        haltFlags = Array(numFiniteStates) { BooleanArray(symbols) }
        loadProgram(programName, symbols)
    }

    // MP lattice Moore neighbor indices
    private companion object {
        // MP ordering: 0=NW, 1=N, 2=NE, 3=E, 4=SE, 5=S, 6=SW, 7=W
        const val NW = 0; const val N = 1; const val NE = 2; const val E = 3
        const val SE = 4; const val S = 5; const val SW = 6; const val W = 7

        // Opposite direction lookup: if head moves East (3), the destination
        // cell finds the head by looking West (7), its opposite direction.
        val OPPOSITE = intArrayOf(
            SE, // opposite of NW(0) is SE(4)
            S,  // opposite of N(1) is S(5)
            SW, // opposite of NE(2) is SW(6)
            W,  // opposite of E(3) is W(7)
            NW, // opposite of SE(4) is NW(0)
            N,  // opposite of S(5) is N(1)
            NE, // opposite of SW(6) is NE(2)
            E,  // opposite of W(7) is E(3)
        )

        // Global TM controller state — updated once per generation
        @Volatile var currentFiniteState = 0
        @Volatile var readSymbol = 0
        @Volatile var halted = false
        @Volatile var lastGeneration = -1
        @Volatile var pendingNextState = 0
        @Volatile var pendingReadSymbol = 0
        @Volatile var stateUpdated = false

        val ALL_PROGRAMS = listOf(
            "Counting", "Bouncing Line", "Staircase", "Expanding Square",
            "Binary Counter",
            "Busy Beaver #1", "Busy Beaver #2",
            "Classic BB-3", "Classic BB-4",
            "Subtraction"
        )

        fun recommendedStates(program: String): Int = when (program) {
            "Counting", "Subtraction", "Busy Beaver #1", "Busy Beaver #2" -> 4
            else -> 3
        }
    }

    private fun loadProgram(name: String, symbols: Int) {
        // Reset controller on program load
        currentFiniteState = 0
        readSymbol = 0
        halted = false
        lastGeneration = -1
        stateUpdated = false

        when (name) {
            // ── Original CAExplorer programs (3-symbol) ───────────────────

            "Counting" -> if (symbols >= 3) {
                transitions[0][0] = Triple(0, E, 0)
                transitions[0][1] = Triple(1, E, 0)
                transitions[0][2] = Triple(2, W, 1)
                transitions[1][0] = Triple(1, E, 0)
                transitions[1][1] = Triple(0, W, 1)
                transitions[1][2] = Triple(1, E, 0)
            }

            "Subtraction" -> if (symbols >= 3) {
                transitions[0][0] = Triple(0, E, 0)
                transitions[0][1] = Triple(1, E, 0)
                transitions[0][2] = Triple(2, E, 1)
                transitions[1][0] = Triple(0, E, 1)
                transitions[1][1] = Triple(1, E, 1)
                transitions[1][2] = Triple(2, W, 2)
                transitions[2][0] = Triple(0, W, 2)
                transitions[2][1] = Triple(0, W, 3)
                transitions[2][2] = Triple(2, E, 5)
                transitions[3][0] = Triple(0, W, 3)
                transitions[3][1] = Triple(1, W, 3)
                transitions[3][2] = Triple(2, W, 8)
                transitions[4][0] = Triple(0, E, 4)
                transitions[4][1] = Triple(1, E, 4)
                transitions[4][2] = Triple(2, E, 5)
                transitions[5][0] = Triple(0, E, 5)
                transitions[5][1] = Triple(1, E, 5)
                transitions[5][2] = Triple(2, W, 6)
                transitions[6][0] = Triple(2, W, 6)
                transitions[6][1] = Triple(1, W, 6)
                transitions[6][2] = Triple(2, W, 7)
                transitions[7][0] = Triple(0, W, 7)
                transitions[7][1] = Triple(1, W, 7)
                transitions[7][2] = Triple(2, E, 9)
                transitions[8][0] = Triple(0, W, 8)
                transitions[8][1] = Triple(0, E, 0)
                transitions[8][2] = Triple(2, E, 4)
                transitions[9][0] = Triple(0, E, 9); haltFlags[9][0] = true
                transitions[9][1] = Triple(1, E, 9); haltFlags[9][1] = true
                transitions[9][2] = Triple(0, E, 9); haltFlags[9][2] = true
            }

            "Busy Beaver #1" -> if (symbols >= 3) {
                transitions[0][0] = Triple(1, E, 1)
                transitions[0][1] = Triple(1, E, 0); haltFlags[0][1] = true
                transitions[0][2] = Triple(1, E, 1)
                transitions[1][0] = Triple(0, E, 2)
                transitions[1][1] = Triple(1, E, 1)
                transitions[1][2] = Triple(0, E, 2)
                transitions[2][0] = Triple(1, W, 2)
                transitions[2][1] = Triple(1, W, 0)
                transitions[2][2] = Triple(1, W, 2)
            }

            "Busy Beaver #2" -> if (symbols >= 3) {
                transitions[0][0] = Triple(1, E, 1)
                transitions[0][1] = Triple(1, W, 1)
                transitions[0][2] = Triple(1, E, 1)
                transitions[1][0] = Triple(1, W, 0)
                transitions[1][1] = Triple(0, W, 2)
                transitions[1][2] = Triple(1, W, 0)
                transitions[2][0] = Triple(1, W, 2); haltFlags[2][0] = true
                transitions[2][1] = Triple(1, W, 3)
                transitions[2][2] = Triple(1, W, 2); haltFlags[2][2] = true
                transitions[3][0] = Triple(1, E, 3)
                transitions[3][1] = Triple(0, E, 0)
                transitions[3][2] = Triple(1, E, 3)
            }

            // ── Classic 2-symbol Busy Beavers ─────────────────────────────

            "Classic BB-3" -> if (symbols >= 2) {
                transitions[0][0] = Triple(1, E, 1)
                transitions[0][1] = Triple(1, W, 2)
                transitions[1][0] = Triple(1, W, 0)
                transitions[1][1] = Triple(1, E, 1)
                transitions[2][0] = Triple(1, W, 1)
                transitions[2][1] = Triple(1, E, 0); haltFlags[2][1] = true
            }

            "Classic BB-4" -> if (symbols >= 2) {
                transitions[0][0] = Triple(1, E, 1)
                transitions[0][1] = Triple(1, W, 1)
                transitions[1][0] = Triple(1, W, 0)
                transitions[1][1] = Triple(0, W, 2)
                transitions[2][0] = Triple(1, E, 3)
                transitions[2][1] = Triple(1, W, 3)
                transitions[3][0] = Triple(1, E, 0)
                transitions[3][1] = Triple(0, E, 0); haltFlags[3][1] = true
            }

            // ── 2D programs ───────────────────────────────────────────────

            "Bouncing Line" -> if (symbols >= 2) {
                transitions[0][0] = Triple(1, E, 1)
                transitions[0][1] = Triple(1, W, 0)
                transitions[1][0] = Triple(1, W, 0)
                transitions[1][1] = Triple(1, E, 1)
            }

            "Staircase" -> if (symbols >= 2) {
                transitions[0][0] = Triple(1, E, 1)
                transitions[0][1] = Triple(1, S, 1)
                transitions[1][0] = Triple(1, S, 0)
                transitions[1][1] = Triple(1, E, 0)
            }

            "Expanding Square" -> if (symbols >= 2) {
                transitions[0][0] = Triple(1, E, 1)
                transitions[0][1] = Triple(1, NE, 0)
                transitions[1][0] = Triple(1, S, 2)
                transitions[1][1] = Triple(1, SE, 1)
                transitions[2][0] = Triple(1, W, 3)
                transitions[2][1] = Triple(1, SW, 2)
                transitions[3][0] = Triple(1, N, 0)
                transitions[3][1] = Triple(1, NW, 3)
            }

            "Binary Counter" -> if (symbols >= 2) {
                // Non-halting binary counter: increments forever.
                // Scans right to find end, turns around, flips 1→0 (carry),
                // 0→1 (done), scans right again. Wraps at grid boundary.
                transitions[0][0] = Triple(0, E, 0) // scan right past 0s
                transitions[0][1] = Triple(1, E, 0) // scan right past 1s
                // When head wraps around (reads 0 after rightmost 1), we rely
                // on the fact that the head enters blank tape and turns around.
                // This uses 3 states for a cleaner loop:
                // State 0: scan right. When we see the leftmost 0 after 1s, turn.
                // State 1: increment mode, go left.
                // State 2: scan right to re-enter state 0.
                // For 2-symbol tape, a simpler approach:
                // State 0 scans E; when cell after all marks is blank, switch.
                transitions[1][0] = Triple(1, W, 0) // carry done: write 1, go right
                transitions[1][1] = Triple(0, W, 1) // carry: 1→0, keep going left
            }
        }
    }

    override fun nextState(cell: Cell, neighbors: Array<Cell>): CellState {
        val tapeHead = numStates - 1
        val cellVal = cell.currentState.toInt()
        val generation = cell.generation

        // Once per generation: advance the finite state controller
        if (lastGeneration != generation) {
            if (generation < lastGeneration) {
                // Simulation was restarted — reset controller to initial state
                currentFiniteState = 0
                readSymbol = 0
                stateUpdated = false
            } else if (stateUpdated) {
                currentFiniteState = pendingNextState
                readSymbol = pendingReadSymbol
            }
            lastGeneration = generation
            stateUpdated = false
        }

        // If halted, everything freezes
        val fs = currentFiniteState.coerceIn(0, transitions.size - 1)
        val sym = readSymbol.coerceIn(0, numStates - 2)
        if (haltFlags[fs][sym]) {
            return IntegerCellState(cellVal)
        }

        val transition = transitions[fs][sym]
        val writeVal = transition.first.coerceIn(0, numStates - 2)
        val moveDir = transition.second.coerceIn(0, 7)
        val nextFS = transition.third

        // Case 1: This cell IS the tape head → it gets written over
        if (cellVal == tapeHead) {
            return IntegerCellState(writeVal)
        }

        // Case 2: This cell is the move DESTINATION.
        // The head wants to move in direction `moveDir`. From THIS cell's
        // perspective, the head is in the OPPOSITE direction.
        // Use getOldNeighborState to handle in-place updates: if the head cell
        // was already processed this generation (state changed to writeVal),
        // we check its previousState instead.
        val oppositeDir = OPPOSITE[moveDir]
        if (oppositeDir < neighbors.size) {
            val neighbor = neighbors[oppositeDir]
            val neighborState = getOldNeighborState(neighbor, generation)
            if (neighborState == tapeHead) {
                if (!stateUpdated) {
                    stateUpdated = true
                    pendingReadSymbol = cellVal
                    pendingNextState = nextFS
                }
                return IntegerCellState(tapeHead)
            }
        }

        // Case 3: Not involved → stay the same
        return IntegerCellState(cellVal)
    }

    /**
     * Get a neighbor's state from BEFORE this generation's updates.
     * If the neighbor has already been processed (generation incremented),
     * use its previousState to see what it was before the update.
     */
    private fun getOldNeighborState(neighbor: Cell, myGeneration: Int): Int {
        return if (neighbor.generation > myGeneration) {
            // Neighbor already processed this step — its currentState is the
            // NEW state. Use previousState to get the pre-update value.
            neighbor.previousState?.toInt() ?: neighbor.currentState.toInt()
        } else {
            // Neighbor not yet processed — currentState is still the old value.
            neighbor.currentState.toInt()
        }
    }

    override fun createInitialState(): CellState = IntegerCellState(0)

    override val properties get() = listOf(
        RuleProperty.ChoiceProperty(
            "program", "Program", programName,
            ALL_PROGRAMS,
            "Preset Turing machine program"
        ),
        RuleProperty.IntProperty("numStates", "Symbols + 1", numStates, 3, 8,
            "Number of tape symbols + 1 (head marker)")
    )

    override fun withProperty(key: String, value: Any): Rule = when (key) {
        "program" -> {
            val prog = value as String
            TuringMachine(recommendedStates(prog), prog)
        }
        "numStates" -> TuringMachine((value as Number).toInt().coerceIn(3, 8), programName)
        else -> this
    }
}
