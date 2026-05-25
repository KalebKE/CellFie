package org.caexplorer

import org.caexplorer.domain.cell.Cell
import org.caexplorer.domain.cellstate.Complex
import org.caexplorer.domain.cellstate.ComplexCellState
import org.caexplorer.domain.cellstate.IntegerCellState
import org.caexplorer.domain.cellstate.RealValuedState
import org.caexplorer.domain.colorscheme.*
import org.caexplorer.domain.lattice.*
import org.caexplorer.domain.rule.*
import org.caexplorer.domain.rule.implementations.*
import org.caexplorer.engine.SimulationConfig
import org.jetbrains.skia.*
import java.io.File
import kotlin.math.floor
import kotlin.random.Random
import kotlin.test.Test

/**
 * Generates PNG screenshots of representative CA rules for the README.
 *
 * Run with: ./gradlew :shared:desktopTest --tests "org.caexplorer.ScreenshotGenerator"
 */
class ScreenshotGenerator {

    private val projectDir = File(System.getProperty("user.dir")).let { dir ->
        // Gradle may set user.dir to the subproject; walk up to find project root
        var d = dir
        while (!File(d, "README.md").exists() && d.parentFile != null) d = d.parentFile
        d
    }
    private val outputDir = File(projectDir, "screenshots").also { it.mkdirs() }
    private val scaleFactor = 2

    // =====================================================================
    // 1D Elementary Rules — each generation becomes a row in the image
    // =====================================================================

    @Test
    fun generateElementaryScreenshots() {
        val ruleNumbers = listOf(30, 90, 110)
        val width = 401
        val generations = 200

        for (ruleNum in ruleNumbers) {
            generate1DScreenshot(
                rule = WolframRule(ruleNum),
                width = width,
                generations = generations,
                colorScheme = BlackAndWhiteColorScheme(),
                filename = "rule_$ruleNum.png"
            )
        }
    }

    private fun generate1DScreenshot(
        rule: WolframRule,
        width: Int,
        generations: Int,
        colorScheme: ColorScheme,
        filename: String
    ) {
        IntegerCellState.numStates = 2

        val lattice = Standard1DLattice(width) { coord ->
            Cell(IntegerCellState(if (coord.col == width / 2) 1 else 0), coord)
        }

        val imgWidth = width * scaleFactor
        val imgHeight = generations * scaleFactor
        val bitmap = Bitmap()
        bitmap.allocPixels(ImageInfo.makeN32Premul(imgWidth, imgHeight))
        val canvas = Canvas(bitmap)

        // Fill background with empty color
        val bgColor = colorScheme.getColor(0, 2)
        canvas.clear(composeColorToArgb(bgColor))

        // Draw initial state as row 0
        drawRow(canvas, lattice.cells, 0, colorScheme, 2)

        // Advance and draw each generation
        for (gen in 1 until generations) {
            advanceGeneration(lattice, rule)
            drawRow(canvas, lattice.cells, gen, colorScheme, 2)
        }

        savePng(bitmap, filename)
        println("Generated: $filename ($width x $generations, scale ${scaleFactor}x)")
    }

    // =====================================================================
    // 2D Rules — capture the grid state after N generations
    // =====================================================================

    @Test
    fun generateLifeScreenshot() {
        generate2DScreenshot(
            rule = Life(),
            colorScheme = CyberpunkColorScheme(),
            filename = "game_of_life.png",
            width = 300, height = 300,
            generations = 200,
            density = 0.25
        )
    }

    @Test
    fun generateBriansBrainScreenshot() {
        generate2DScreenshot(
            rule = BriansBrain(),
            colorScheme = NeonColorScheme(),
            filename = "brians_brain.png",
            width = 300, height = 300,
            generations = 150,
            density = 0.30
        )
    }

    @Test
    fun generateCyclicCAScreenshot() {
        // Cyclic CA needs all states in the initial grid (including state 0)
        val rule = CyclicCA()
        val width = 300
        val height = 300
        val numStates = rule.numStates  // 14
        IntegerCellState.numStates = numStates

        val rng = Random(42)
        val lattice = createLattice(LatticeType.SQUARE_MOORE, width, height) { coord ->
            Cell(IntegerCellState(rng.nextInt(numStates)), coord) // uniform across all states
        }

        val config = SimulationConfig(
            rule = rule, lattice = lattice,
            colorScheme = NeonColorScheme(), numWorkers = 1
        )

        repeat(80) { advanceGeneration(config) }

        val colors = computeColors(config)
        renderAndSave(colors, width, height, "cyclic_ca.png")
        println("Generated: cyclic_ca.png ($width x $height, 80 gens, $numStates states)")
    }

    @Test
    fun generateLangtonsAntScreenshot() {
        val rule = LangtonsAnt()
        val width = 120
        val height = 120
        IntegerCellState.numStates = rule.numStates

        val lattice = VonNeumannLattice(width, height) { coord ->
            val state = if (coord.row == height / 2 && coord.col == width / 2) 2 else 0
            Cell(IntegerCellState(state), coord)
        }

        val config = SimulationConfig(
            rule = rule,
            lattice = lattice,
            colorScheme = RainbowColorScheme(), // unused, colors computed manually
            numWorkers = 1
        )

        repeat(12000) { advanceGeneration(config) }

        // Custom color mapping for Langton's Ant
        val cells = config.lattice.cells
        val colors = IntArray(cells.size) { i ->
            when (cells[i].currentState.toInt()) {
                0 -> 0xFF1A1A2E.toInt()    // dark navy (white ground)
                1 -> 0xFFE94560.toInt()     // vibrant red-pink (black ground / flipped)
                else -> 0xFF00FF41.toInt()  // bright green (ant)
            }
        }
        renderAndSave(colors, width, height, "langtons_ant.png", scale = 4)
        println("Generated: langtons_ant.png ($width x $height, 12000 gens)")
    }

    @Test
    fun generateReadmeCategoryScreenshots() {
        generate2DCenterSeedScreenshot(
            rule = Snowflake(),
            colorScheme = BlueDiamondColorScheme(),
            filename = "totalistic_snowflake.png",
            width = 300,
            height = 300,
            generations = 180
        )

        generate2DScreenshot(
            rule = ElectricLoops(),
            colorScheme = CyberpunkColorScheme(),
            filename = "totalistic_electric_loops.png",
            width = 300,
            height = 300,
            generations = 110,
            density = 0.42
        )

        generate2DScreenshot(
            rule = ContinuousCA.CLASS_IV,
            colorScheme = PastelColorScheme(),
            filename = "continuous_class_iv.png",
            width = 300,
            height = 300,
            generations = 140,
            density = 1.0
        )

        generateRealValuedScreenshot(
            rule = RealSpirals(),
            colorScheme = NeonColorScheme(),
            filename = "real_spirals.png",
            width = 300,
            height = 300,
            generations = 150,
            maxValue = 1000.0
        )

        generateRealValuedScreenshot(
            rule = ThunderStorm(),
            colorScheme = BlueDiamondColorScheme(),
            filename = "thunderstorm.png",
            width = 300,
            height = 300,
            generations = 90,
            maxValue = 1000.0
        )

        generate2DScreenshot(
            rule = NeuralNetCA(),
            colorScheme = CyberpunkColorScheme(),
            filename = "lenia_growth.png",
            width = 300,
            height = 300,
            generations = 155,
            density = 0.55
        )

        generate2DScreenshot(
            rule = MajorityVote(numStates = 5),
            colorScheme = SupercarColorScheme(),
            filename = "majority_vote.png",
            width = 300,
            height = 300,
            generations = 60,
            density = 1.0
        )

        generate2DScreenshot(
            rule = CellularMarketModel(numStates = 10, temperature = 0.65, noise = 0.02),
            colorScheme = RainbowColorScheme(),
            filename = "cellular_market_model.png",
            width = 300,
            height = 300,
            generations = 80,
            density = 1.0
        )

        generateWireworldScreenshot()

        generate2DScreenshot(
            rule = IsingModel(temperature = 2.15),
            colorScheme = BlackAndWhiteColorScheme(),
            filename = "ising_model.png",
            width = 300,
            height = 300,
            generations = 140,
            density = 0.50
        )

        generateForestFireScreenshot()
        generateMandelbrotScreenshot()
        generateTuringMachineScreenshot()

        generate2DCenterSeedScreenshot(
            rule = DiffusionLimitedAggregation(),
            colorScheme = BlackAndWhiteColorScheme(),
            filename = "diffusion_limited_aggregation.png",
            width = 300,
            height = 300,
            generations = 130
        )
    }

    // =====================================================================
    // Helpers
    // =====================================================================

    private fun generate2DScreenshot(
        rule: Rule,
        colorScheme: ColorScheme,
        filename: String,
        width: Int,
        height: Int,
        generations: Int,
        density: Double,
        latticeType: LatticeType = LatticeType.SQUARE_MOORE
    ) {
        val numStates = (rule as? IntegerRule)?.numStates ?: 2
        IntegerCellState.numStates = numStates

        val isRealRule = rule is RealRule
        val rng = Random(42)

        val lattice = createLattice(latticeType, width, height) { coord ->
            if (isRealRule) {
                Cell(RealValuedState(rng.nextDouble()), coord)
            } else {
                val state = if (rng.nextDouble() < density) {
                    if (numStates > 2) rng.nextInt(1, numStates) else 1
                } else 0
                Cell(IntegerCellState(state), coord)
            }
        }

        val config = SimulationConfig(
            rule = rule,
            lattice = lattice,
            colorScheme = colorScheme,
            numWorkers = 1
        )

        repeat(generations) { advanceGeneration(config) }

        val colors = computeColors(config)
        renderAndSave(colors, width, height, filename)
        println("Generated: $filename ($width x $height, $generations gens)")
    }

    private fun generate2DCenterSeedScreenshot(
        rule: Rule,
        colorScheme: ColorScheme,
        filename: String,
        width: Int,
        height: Int,
        generations: Int,
        latticeType: LatticeType = LatticeType.SQUARE_MOORE
    ) {
        val numStates = (rule as? IntegerRule)?.numStates ?: 2
        IntegerCellState.numStates = numStates

        val lattice = createLattice(latticeType, width, height) { coord ->
            val state = if (coord.row == height / 2 && coord.col == width / 2) numStates - 1 else 0
            Cell(IntegerCellState(state), coord)
        }

        val config = SimulationConfig(
            rule = rule,
            lattice = lattice,
            colorScheme = colorScheme,
            numWorkers = 1
        )

        repeat(generations) { advanceGeneration(config) }

        val colors = computeColors(config)
        renderAndSave(colors, width, height, filename)
        println("Generated: $filename ($width x $height, $generations gens, center seed)")
    }

    private fun generateRealValuedScreenshot(
        rule: RealRule,
        colorScheme: ColorScheme,
        filename: String,
        width: Int,
        height: Int,
        generations: Int,
        maxValue: Double
    ) {
        val rng = Random(42)
        val lattice = createLattice(LatticeType.SQUARE_MOORE, width, height) { coord ->
            Cell(RealValuedState(rng.nextDouble(0.0, maxValue), 0.0, maxValue), coord)
        }

        val config = SimulationConfig(
            rule = rule,
            lattice = lattice,
            colorScheme = colorScheme,
            numWorkers = 1
        )

        repeat(generations) { advanceGeneration(config) }

        val colors = computeColors(config)
        renderAndSave(colors, width, height, filename)
        println("Generated: $filename ($width x $height, $generations gens, real-valued)")
    }

    private fun generateWireworldScreenshot() {
        val rule = Wireworld()
        val width = 300
        val height = 220
        val states = IntArray(width * height) { Wireworld.EMPTY }

        fun set(row: Int, col: Int, state: Int) {
            if (row in 0 until height && col in 0 until width) {
                states[row * width + col] = state
            }
        }

        fun horizontal(row: Int, startCol: Int, endCol: Int) {
            for (col in startCol..endCol) set(row, col, Wireworld.CONDUCTOR)
        }

        fun vertical(col: Int, startRow: Int, endRow: Int) {
            for (row in startRow..endRow) set(row, col, Wireworld.CONDUCTOR)
        }

        horizontal(55, 24, 250)
        horizontal(110, 24, 250)
        horizontal(165, 24, 250)
        vertical(70, 55, 165)
        vertical(150, 55, 165)
        vertical(230, 55, 165)
        horizontal(82, 70, 150)
        horizontal(138, 150, 230)
        vertical(110, 82, 110)
        vertical(190, 110, 138)

        listOf(
            55 to 36,
            110 to 92,
            165 to 188,
            82 to 126,
            138 to 204
        ).forEach { (row, col) ->
            set(row, col - 1, Wireworld.TAIL)
            set(row, col, Wireworld.HEAD)
        }

        IntegerCellState.numStates = rule.numStates
        val lattice = createLattice(LatticeType.SQUARE_MOORE, width, height) { coord ->
            Cell(IntegerCellState(states[coord.row * width + coord.col]), coord)
        }
        val config = SimulationConfig(rule, lattice, RainbowColorScheme(), numWorkers = 1)

        repeat(70) { advanceGeneration(config) }

        val colors = IntArray(config.lattice.cells.size) { i ->
            when (config.lattice.cells[i].currentState.toInt()) {
                Wireworld.HEAD -> 0xFF00D7FF.toInt()
                Wireworld.TAIL -> 0xFFFF4D00.toInt()
                Wireworld.CONDUCTOR -> 0xFFFFFF33.toInt()
                else -> 0xFF07070D.toInt()
            }
        }
        renderAndSave(colors, width, height, "wireworld.png")
        println("Generated: wireworld.png ($width x $height, 70 gens)")
    }

    private fun generateForestFireScreenshot() {
        val rule = ForestFire(growthProbability = 0.02, lightningProbability = 0.0004)
        val width = 300
        val height = 300
        val rng = Random(42)

        IntegerCellState.numStates = rule.numStates
        val lattice = createLattice(LatticeType.SQUARE_MOORE, width, height) { coord ->
            val state = when {
                coord.col in 120..124 && coord.row in 35..260 -> ForestFire.BURNING
                coord.col in 125..128 && coord.row in 35..260 -> ForestFire.SMOLDERING
                rng.nextDouble() < 0.76 -> ForestFire.MATURE_TREE
                rng.nextDouble() < 0.88 -> ForestFire.YOUNG_TREE
                rng.nextDouble() < 0.96 -> ForestFire.SAPLING
                else -> ForestFire.BARE_GROUND
            }
            Cell(IntegerCellState(state), coord)
        }
        val config = SimulationConfig(rule, lattice, FireColorScheme(), numWorkers = 1)

        repeat(34) { advanceGeneration(config) }

        val colors = IntArray(config.lattice.cells.size) { i ->
            when (config.lattice.cells[i].currentState.toInt()) {
                ForestFire.SEEDLING -> 0xFF2C7A2C.toInt()
                ForestFire.SAPLING -> 0xFF46A546.toInt()
                ForestFire.YOUNG_TREE -> 0xFF69C35A.toInt()
                ForestFire.MATURE_TREE -> 0xFF0D5F25.toInt()
                ForestFire.BURNING -> 0xFFFFD447.toInt()
                ForestFire.SMOLDERING -> 0xFFFF5B1A.toInt()
                ForestFire.ASHES -> 0xFF666666.toInt()
                else -> 0xFF160E0A.toInt()
            }
        }
        renderAndSave(colors, width, height, "forest_fire.png")
        println("Generated: forest_fire.png ($width x $height, 34 gens)")
    }

    private fun generateMandelbrotScreenshot() {
        val width = 320
        val height = 240
        val maxIterations = 90
        val rule = FractalIteration(gridSize = width, maxIterations = maxIterations)
        val lattice = createLattice(LatticeType.SQUARE_MOORE, width, height) { coord ->
            Cell(rule.createInitialState(), coord)
        }
        val config = SimulationConfig(rule, lattice, CyberpunkColorScheme(), numWorkers = 1)

        repeat(maxIterations) { advanceGeneration(config) }

        val colors = IntArray(config.lattice.cells.size) { i ->
            val complex = (config.lattice.cells[i].currentState as? ComplexCellState)?.state ?: Complex.ZERO
            if (complex.imaginary > 2.0) {
                val t = (complex.real / maxIterations.toDouble()).coerceIn(0.0, 1.0)
                gradientArgb(
                    t,
                    0xFF15102B.toInt(),
                    0xFF1BA8FF.toInt(),
                    0xFFFFEA00.toInt()
                )
            } else {
                0xFF05050A.toInt()
            }
        }
        renderAndSave(colors, width, height, "fractal_mandelbrot.png", scale = 3)
        println("Generated: fractal_mandelbrot.png ($width x $height, $maxIterations gens)")
    }

    private fun generateTuringMachineScreenshot() {
        val rule = TuringMachine(numStates = 3, programName = "Expanding Square")
        val width = 180
        val height = 180
        IntegerCellState.numStates = rule.numStates

        val lattice = createLattice(LatticeType.SQUARE_MOORE, width, height) { coord ->
            val state = if (coord.row == height / 2 && coord.col == width / 2) rule.numStates - 1 else 0
            Cell(IntegerCellState(state), coord)
        }
        val config = SimulationConfig(rule, lattice, RainbowColorScheme(), numWorkers = 1)

        repeat(2600) { advanceGeneration(config) }

        val headState = rule.numStates - 1
        val colors = IntArray(config.lattice.cells.size) { i ->
            when (config.lattice.cells[i].currentState.toInt()) {
                headState -> 0xFFFF3366.toInt()
                1 -> 0xFF00E5FF.toInt()
                else -> 0xFF080A12.toInt()
            }
        }
        renderAndSave(colors, width, height, "turing_machine_expanding_square.png", scale = 4)
        println("Generated: turing_machine_expanding_square.png ($width x $height, 2600 gens)")
    }

    /** Advance all cells by one generation. */
    private fun advanceGeneration(config: SimulationConfig) {
        advanceGeneration(config.lattice, config.rule)
    }

    private fun advanceGeneration(lattice: Lattice, rule: Rule) {
        val cells = lattice.cells
        val newStates = Array(cells.size) { i ->
            rule.nextState(cells[i], lattice.getNeighbors(cells[i]))
        }
        for (i in cells.indices) {
            cells[i].addNewState(newStates[i])
        }
    }

    /** Compute ARGB color array from current cell states. */
    private fun computeColors(config: SimulationConfig): IntArray {
        val cells = config.lattice.cells
        val scheme = config.colorScheme
        val numStates = (config.rule as? IntegerRule)?.numStates ?: 2
        val isRealRule = config.rule is RealRule

        return IntArray(cells.size) { i ->
            if (isRealRule) {
                val state = cells[i].currentState as? RealValuedState
                val realValue = state?.state ?: 0.0
                val minValue = state?.emptyState ?: 0.0
                val maxValue = state?.fullState ?: 1.0
                val percent = if (maxValue == minValue) 0.0 else (realValue - minValue) / (maxValue - minValue)
                val color = scheme.getColor(percent.coerceIn(0.0, 1.0))
                composeColorToArgb(color)
            } else {
                val state = cells[i].currentState.toInt()
                val color = scheme.getColor(state, numStates)
                composeColorToArgb(color)
            }
        }
    }

    /** Draw a single row of cells onto the canvas. */
    private fun drawRow(
        canvas: Canvas, cells: Array<Cell>, row: Int,
        colorScheme: ColorScheme, numStates: Int
    ) {
        val paint = Paint()
        for (col in cells.indices) {
            val state = cells[col].currentState.toInt()
            val color = colorScheme.getColor(state, numStates)
            paint.color = composeColorToArgb(color)
            canvas.drawRect(
                Rect.makeXYWH(
                    (col * scaleFactor).toFloat(),
                    (row * scaleFactor).toFloat(),
                    scaleFactor.toFloat(),
                    scaleFactor.toFloat()
                ),
                paint
            )
        }
    }

    /** Render a color array to a scaled PNG and save. */
    private fun renderAndSave(colors: IntArray, width: Int, height: Int, filename: String, scale: Int = scaleFactor) {
        val imgWidth = width * scale
        val imgHeight = height * scale
        val bitmap = Bitmap()
        bitmap.allocPixels(ImageInfo.makeN32Premul(imgWidth, imgHeight))
        val canvas = Canvas(bitmap)

        val paint = Paint()
        for (row in 0 until height) {
            for (col in 0 until width) {
                paint.color = colors[row * width + col]
                canvas.drawRect(
                    Rect.makeXYWH(
                        (col * scale).toFloat(),
                        (row * scale).toFloat(),
                        scale.toFloat(),
                        scale.toFloat()
                    ),
                    paint
                )
            }
        }

        savePng(bitmap, filename)
    }

    private fun savePng(bitmap: Bitmap, filename: String) {
        val image = Image.makeFromBitmap(bitmap)
        val data = image.encodeToData(EncodedImageFormat.PNG) ?: error("Failed to encode PNG")
        File(outputDir, filename).writeBytes(data.bytes)
    }

    private fun gradientArgb(percent: Double, vararg colors: Int): Int {
        val t = percent.coerceIn(0.0, 1.0)
        if (colors.isEmpty()) return 0xFF000000.toInt()
        if (colors.size == 1) return colors[0]

        val scaled = t * (colors.size - 1)
        val idx = floor(scaled).toInt().coerceIn(0, colors.size - 2)
        val local = scaled - idx
        return interpolateArgb(colors[idx], colors[idx + 1], local)
    }

    private fun interpolateArgb(from: Int, to: Int, percent: Double): Int {
        fun channel(color: Int, shift: Int) = (color ushr shift) and 0xFF
        val r = channel(from, 16) + ((channel(to, 16) - channel(from, 16)) * percent).toInt()
        val g = channel(from, 8) + ((channel(to, 8) - channel(from, 8)) * percent).toInt()
        val b = channel(from, 0) + ((channel(to, 0) - channel(from, 0)) * percent).toInt()
        return (0xFF shl 24) or (r.coerceIn(0, 255) shl 16) or
            (g.coerceIn(0, 255) shl 8) or b.coerceIn(0, 255)
    }

    private fun composeColorToArgb(color: androidx.compose.ui.graphics.Color): Int {
        val r = (color.red * 255).toInt()
        val g = (color.green * 255).toInt()
        val b = (color.blue * 255).toInt()
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }
}
