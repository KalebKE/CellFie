package org.caexplorer

import org.caexplorer.domain.cell.Cell
import org.caexplorer.domain.cellstate.IntegerCellState
import org.caexplorer.domain.cellstate.RealValuedState
import org.caexplorer.domain.colorscheme.*
import org.caexplorer.domain.lattice.*
import org.caexplorer.domain.rule.*
import org.caexplorer.domain.rule.implementations.*
import org.caexplorer.domain.util.Coordinate
import org.caexplorer.engine.SimulationConfig
import org.caexplorer.engine.SimulationFactory
import org.jetbrains.skia.*
import java.io.File
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
                val realValue = (cells[i].currentState.value as? Double) ?: 0.0
                val color = scheme.getColor(realValue)
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

    private fun composeColorToArgb(color: androidx.compose.ui.graphics.Color): Int {
        val r = (color.red * 255).toInt()
        val g = (color.green * 255).toInt()
        val b = (color.blue * 255).toInt()
        return (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    }
}
