package org.caexplorer.domain.rule.implementations

import org.caexplorer.domain.cell.Cell
import org.caexplorer.domain.cellstate.CellState
import org.caexplorer.domain.cellstate.Complex
import org.caexplorer.domain.cellstate.ComplexCellState
import org.caexplorer.domain.rule.ComplexRule
import org.caexplorer.domain.rule.RuleCategory

// =============================================================================
// Fractal (Mandelbrot Set Iteration)
// =============================================================================

/**
 * Mandelbrot set fractal iteration: z_{n+1} = z_n^2 + c.
 *
 * Each cell's complex state represents z. The constant c is derived from
 * the cell's grid coordinates mapped to the complex plane. Cells iterate
 * the fractal equation each generation.
 *
 * Simplified port of Java Fractal.
 */
class Fractal(
    private val centerReal: Double = -0.5,
    private val centerImag: Double = 0.0,
    private val zoom: Double = 3.0,
    private val gridSize: Int = 200
) : ComplexRule() {
    override val displayName = "Fractal (Mandelbrot)"
    override val description = "Mandelbrot set z² + c iteration on the complex plane"
    override val category = RuleCategory.FRACTAL
    override val compatibleLatticeNames = listOf("Square (Moore)", "Square (Von Neumann)")

    override fun nextState(cell: Cell, neighbors: Array<Cell>): CellState {
        val z = getComplex(cell)
        val c = coordinateToComplex(cell.coordinate.row, cell.coordinate.col)

        // z_{n+1} = z^2 + c
        val zNext = z * z + c

        // Clamp to prevent overflow
        val clamped = if (zNext.modulus > ESCAPE_RADIUS) {
            Complex(ESCAPE_RADIUS, 0.0)
        } else {
            zNext
        }

        return makeState(clamped)
    }

    override fun createInitialState(): CellState = makeState(Complex.ZERO)

    private fun coordinateToComplex(row: Int, col: Int): Complex {
        val real = centerReal + (col.toDouble() / gridSize - 0.5) * zoom
        val imag = centerImag + (row.toDouble() / gridSize - 0.5) * zoom
        return Complex(real, imag)
    }

    companion object {
        private const val ESCAPE_RADIUS = 1000.0

        fun getComplex(cell: Cell): Complex {
            val state = cell.currentState
            return if (state is ComplexCellState) state.state else Complex.ZERO
        }

        fun makeState(c: Complex): ComplexCellState = ComplexCellState(
            state = c,
            alternateStateValue = Complex(0.5, 0.5),
            emptyStateValue = Complex.ZERO,
            fullStateValue = Complex(ESCAPE_RADIUS, 0.0)
        )
    }
}

// =============================================================================
// Fractal Iteration (Iteration counter for Mandelbrot)
// =============================================================================

/**
 * Mandelbrot iteration counter: iterates z = z² + c and tracks how many
 * iterations until |z| exceeds the escape radius. The real part of the
 * complex state stores the iteration count; the imaginary part stores |z|.
 *
 * Simplified port of Java FractalIteration.
 */
class FractalIteration(
    private val centerReal: Double = -0.5,
    private val centerImag: Double = 0.0,
    private val zoom: Double = 3.0,
    private val gridSize: Int = 200,
    private val maxIterations: Int = 100
) : ComplexRule() {
    override val displayName = "Fractal Iteration"
    override val description = "Mandelbrot set iteration count visualization"
    override val category = RuleCategory.FRACTAL
    override val compatibleLatticeNames = listOf("Square (Moore)", "Square (Von Neumann)")

    override fun nextState(cell: Cell, neighbors: Array<Cell>): CellState {
        val state = Fractal.getComplex(cell)
        val iteration = cell.generation

        // Already escaped or max iterations reached — freeze
        if (state.modulus > ESCAPE_RADIUS || iteration >= maxIterations) {
            return Fractal.makeState(state)
        }

        val c = coordinateToComplex(cell.coordinate.row, cell.coordinate.col)

        // Incremental: z_{n+1} = z_n^2 + c  (O(1) per generation)
        val zNext = state * state + c

        if (zNext.modulus > ESCAPE_RADIUS) {
            // Store escaped marker: encode iteration in the real part, modulus in imaginary
            return Fractal.makeState(Complex(iteration.toDouble(), zNext.modulus))
        }

        return Fractal.makeState(zNext)
    }

    override fun createInitialState(): CellState = Fractal.makeState(Complex.ZERO)

    private fun coordinateToComplex(row: Int, col: Int): Complex {
        val real = centerReal + (col.toDouble() / gridSize - 0.5) * zoom
        val imag = centerImag + (row.toDouble() / gridSize - 0.5) * zoom
        return Complex(real, imag)
    }

    companion object {
        private const val ESCAPE_RADIUS = 2.0
    }
}

// =============================================================================
// Fractal Threshold (Binary escape threshold)
// =============================================================================

/**
 * Binary threshold on Mandelbrot set iteration: cells that escape
 * (|z| > 2 within maxIterations) are 1, others are 0.
 * Represented as complex state where real = 0.0 or 1.0.
 *
 * Simplified port of Java FractalThreshold.
 */
class FractalThreshold(
    private val centerReal: Double = -0.5,
    private val centerImag: Double = 0.0,
    private val zoom: Double = 3.0,
    private val gridSize: Int = 200,
    private val maxIterations: Int = 50
) : ComplexRule() {
    override val displayName = "Fractal Threshold"
    override val description = "Binary Mandelbrot set — escaped vs. captured cells"
    override val category = RuleCategory.FRACTAL
    override val compatibleLatticeNames = listOf("Square (Moore)", "Square (Von Neumann)")

    override fun nextState(cell: Cell, neighbors: Array<Cell>): CellState {
        val state = Fractal.getComplex(cell)

        // Already determined — flag: imaginary > 2.0 (impossible during iteration since |z| ≤ 2)
        if (state.imaginary > 2.0) {
            return Fractal.makeState(state)
        }

        val currentIter = cell.generation
        if (currentIter >= maxIterations) {
            // Didn't escape: inside the set
            return Fractal.makeState(Complex(0.0, DETERMINED_FLAG))
        }

        val c = coordinateToComplex(cell.coordinate.row, cell.coordinate.col)

        // Incremental: z_{n+1} = z_n^2 + c  (O(1) per generation)
        val zNext = state * state + c
        if (zNext.modulus > 2.0) {
            // Escaped
            return Fractal.makeState(Complex(1.0, DETERMINED_FLAG))
        }

        return Fractal.makeState(zNext)
    }

    override fun createInitialState(): CellState = Fractal.makeState(Complex.ZERO)

    private fun coordinateToComplex(row: Int, col: Int): Complex {
        val real = centerReal + (col.toDouble() / gridSize - 0.5) * zoom
        val imag = centerImag + (row.toDouble() / gridSize - 0.5) * zoom
        return Complex(real, imag)
    }

    companion object {
        private const val DETERMINED_FLAG = 3.0
    }
}

// =============================================================================
// Moving Fractal (Animated Mandelbrot with time-varying parameter)
// =============================================================================

/**
 * Animated Mandelbrot set where the center point slowly moves over time.
 * Each generation shifts the viewpoint slightly, creating a zooming or
 * panning animation.
 *
 * Simplified port of Java MovingFractal.
 */
class MovingFractal(
    private val gridSize: Int = 200,
    private val maxIterations: Int = 50
) : ComplexRule() {
    override val displayName = "Moving Fractal"
    override val description = "Animated Mandelbrot set with time-varying viewpoint"
    override val category = RuleCategory.FRACTAL
    override val compatibleLatticeNames = listOf("Square (Moore)", "Square (Von Neumann)")

    override fun nextState(cell: Cell, neighbors: Array<Cell>): CellState {
        val generation = cell.generation

        // Slowly zoom into an interesting point
        val zoom = 3.0 / (1.0 + generation * 0.02)
        val centerReal = -0.745 + generation * 0.0001
        val centerImag = 0.186 - generation * 0.0001

        val c = Complex(
            centerReal + (cell.coordinate.col.toDouble() / gridSize - 0.5) * zoom,
            centerImag + (cell.coordinate.row.toDouble() / gridSize - 0.5) * zoom
        )

        var z = Complex.ZERO
        var iter = 0
        while (iter < maxIterations && z.modulus <= 2.0) {
            z = z * z + c
            iter++
        }

        val escaped = z.modulus > 2.0
        val value = if (escaped) iter.toDouble() / maxIterations else 0.0

        return Fractal.makeState(Complex(value, if (escaped) 1.0 else 0.0))
    }

    override fun createInitialState(): CellState = Fractal.makeState(Complex.ZERO)
}
