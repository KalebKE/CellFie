package org.caexplorer.ui.help

data class HelpSection(val title: String, val content: String)
data class HelpTopic(val title: String, val icon: String, val sections: List<HelpSection>)

object HelpContent {
    val topics: List<HelpTopic> = listOf(
        // ── Getting Started ──────────────────────────────────────────
        HelpTopic(
            title = "Getting Started",
            icon = "🚀",
            sections = listOf(
                HelpSection(
                    title = "What Is CellFie?",
                    content = """
CellFie is a cellular automaton simulator built with Kotlin and Compose Multiplatform. It lets you explore a wide variety of cellular automaton rules on different grid topologies, visualize their evolution in real time, and analyze emergent behavior with built-in analysis tools.
                    """.trimIndent()
                ),
                HelpSection(
                    title = "What Are Cellular Automata?",
                    content = """
A cellular automaton (CA) is a grid of cells, each in one of a finite number of states. At each time step every cell updates its state according to a fixed rule that depends on the states of neighboring cells. Despite their simplicity, CAs can produce strikingly complex patterns — from fractal structures to self-replicating organisms.
                    """.trimIndent()
                ),
                HelpSection(
                    title = "Quick Start",
                    content = """
• Select a rule from the rule picker (tap the rule name in the toolbar)
• Press **Space** to start the simulation
• Use the settings panel (gear icon) to change grid size, lattice type, color scheme, and speed
• Press **A** to open the analysis dashboard
• Press **D** to enter draw mode and paint cells by hand
                    """.trimIndent()
                )
            )
        ),

        // ── Controls ─────────────────────────────────────────────────
        HelpTopic(
            title = "Controls",
            icon = "🎮",
            sections = listOf(
                HelpSection(
                    title = "Keyboard Shortcuts",
                    content = """
• **Space** — Play / Pause the simulation
• **S** — Step one generation forward
• **R** — Rewind one generation back
• **G** — Toggle grid overlay
• **F** — Fit grid to window
• **D** — Toggle draw mode
• **A** — Toggle analysis panel
• **E** — Export current grid as PNG
• **+** / **-** — Speed up / slow down
• **Ctrl+S** / **⌘S** — Save simulation state
• **Ctrl+O** / **⌘O** — Load simulation state
• **?** — Show keyboard shortcuts sheet
• **H** — Open this User Guide
                    """.trimIndent()
                ),
                HelpSection(
                    title = "Toolbar Controls",
                    content = """
The top toolbar provides quick access to:
• **Rule name** — tap to open the rule picker
• **Play/Pause** — start or pause the simulation
• **Step** — advance one generation
• **Reset** — reinitialize the grid
• **Grid toggle** — show/hide cell boundaries
• **Settings** — open the configuration panel
• **Analysis** — open the analysis dashboard
• **Draw mode** — paint cells onto the grid
• **GIF record** — record an animated GIF of the simulation
                    """.trimIndent()
                ),
                HelpSection(
                    title = "Draw Mode",
                    content = """
Press **D** to enter draw mode. The simulation auto-pauses while drawing. Click or drag on the grid to toggle cell states. Press **D** again to exit — the simulation resumes if it was running before.
                    """.trimIndent()
                )
            )
        ),

        // ── Rules ────────────────────────────────────────────────────
        HelpTopic(
            title = "Rules",
            icon = "📐",
            sections = listOf(
                HelpSection(
                    title = "Overview",
                    content = """
CellFie includes a curated library of cellular automaton rules spanning many categories. Open the rule picker to browse, search, and select rules. Each rule card shows the rule's name, category, and a brief description.
                    """.trimIndent()
                ),
                HelpSection(
                    title = "Elementary (1D)",
                    content = """
Wolfram's elementary cellular automata operate on a one-dimensional row of binary cells. Each cell's next state depends on itself and its two immediate neighbors (3-cell neighborhood), giving 256 possible rules numbered 0–255. Classic examples include Rule 30 (chaotic), Rule 110 (Turing-complete), and Rule 90 (Sierpinski triangle).
                    """.trimIndent()
                ),
                HelpSection(
                    title = "Life-like",
                    content = """
Life-like rules use a 2D grid with Moore neighborhood and birth/survival notation (e.g., B3/S23 = Conway's Game of Life). A dead cell is **born** if it has exactly the listed number of live neighbors; a live cell **survives** if it meets the survival condition, otherwise it dies. Variations include HighLife (B36/S23), Day & Night, and Seeds.
                    """.trimIndent()
                ),
                HelpSection(
                    title = "Totalistic",
                    content = """
Totalistic rules generalize life-like rules to more than two states. The next state of a cell depends on the **sum** of its neighbor states rather than the individual neighbor configurations. This allows smooth gradients and wave-like phenomena.
                    """.trimIndent()
                ),
                HelpSection(
                    title = "Continuous",
                    content = """
Continuous rules use real-valued cell states (typically 0.0–1.0) instead of discrete integers. They produce smooth, organic-looking patterns — flowing textures, reaction-diffusion effects, and fluid-like dynamics.
                    """.trimIndent()
                ),
                HelpSection(
                    title = "Other Categories",
                    content = """
• **Complex-valued** — rules operating on complex numbers
• **Lattice Gas** — particle-based models of fluid dynamics
• **Probabilistic** — rules with random/stochastic elements
• **Physics** — models inspired by physical processes
• **Biological** — models of biological systems and growth
• **Social** — models of social dynamics and opinion formation
• **Fractal** — rules that produce self-similar fractal patterns
• **Neural** — neural-network-inspired rules
                    """.trimIndent()
                ),
                HelpSection(
                    title = "Rule Properties",
                    content = """
Some rules expose configurable properties (thresholds, probabilities, state counts). When a rule has properties, the settings panel shows sliders and toggles to adjust them in real time. Changes take effect on the next reset.
                    """.trimIndent()
                )
            )
        ),

        // ── Lattice Types ────────────────────────────────────────────
        HelpTopic(
            title = "Lattice Types",
            icon = "🔷",
            sections = listOf(
                HelpSection(
                    title = "What Is a Lattice?",
                    content = """
The lattice defines how cells are arranged and which cells count as "neighbors." Different lattice types change the geometry of the grid and can dramatically affect the behavior of the same rule.
                    """.trimIndent()
                ),
                HelpSection(
                    title = "Square (Moore)",
                    content = """
The standard 8-neighbor grid. Each cell considers all cells in a 3×3 square around it (including diagonals). This is the default and most commonly used neighborhood for 2D rules like Conway's Game of Life.
                    """.trimIndent()
                ),
                HelpSection(
                    title = "Square (Von Neumann)",
                    content = """
A 4-neighbor grid using only the orthogonal neighbors (up, down, left, right). No diagonals. Produces more angular, cross-shaped patterns compared to Moore neighborhood.
                    """.trimIndent()
                ),
                HelpSection(
                    title = "Hexagonal",
                    content = """
A honeycomb grid where each cell has 6 neighbors. Hexagonal lattices eliminate the directional bias inherent in square grids, producing more isotropic (rotationally symmetric) patterns.
                    """.trimIndent()
                ),
                HelpSection(
                    title = "Triangular",
                    content = """
A triangular mesh where each cell has 3 neighbors. This sparse neighborhood leads to slower propagation and distinctive crystalline patterns.
                    """.trimIndent()
                ),
                HelpSection(
                    title = "Extended Neighborhoods",
                    content = """
• **Moore Radius 2** — 24 neighbors in a 5×5 square (excluding the center)
• **Moore Radius 3** — 48 neighbors in a 7×7 square
• **Von Neumann Radius 2** — 12 neighbors in a diamond shape

Larger neighborhoods allow each cell to "see" farther, enabling long-range interactions and more complex emergent behavior.
                    """.trimIndent()
                )
            )
        ),

        // ── Analysis Tools ───────────────────────────────────────────
        HelpTopic(
            title = "Analysis Tools",
            icon = "📊",
            sections = listOf(
                HelpSection(
                    title = "Overview",
                    content = """
Press **A** to open the analysis dashboard. It provides real-time measurements of the simulation, grouped into four categories: Statistical, Spatial, Information Theory, and Dynamics.
                    """.trimIndent()
                ),
                HelpSection(
                    title = "Statistical",
                    content = """
• **Population** — counts cells in each state over time; useful for tracking growth and decay
• **Density** — fraction of non-empty (alive) cells; a quick measure of overall activity
• **Entropy** — Shannon entropy of the state distribution; higher values mean more disorder and complexity
                    """.trimIndent()
                ),
                HelpSection(
                    title = "Spatial",
                    content = """
• **Clusters** — connected component analysis; reports the number of clusters, average cluster size, and the size of the largest cluster
• **Center of Mass** — calculates the center of mass of all alive cells and tracks how it moves over time
                    """.trimIndent()
                ),
                HelpSection(
                    title = "Information Theory",
                    content = """
• **Mutual Information** — measures spatial correlations between cells and their neighbors; high values indicate structured, non-random patterns
• **Hamming Distance** — counts how many cells changed state between consecutive generations; shows the rate of change
                    """.trimIndent()
                ),
                HelpSection(
                    title = "Dynamics",
                    content = """
• **Activity** — the fraction of cells that changed state in the last step; useful for detecting when a simulation has settled into a static or oscillating pattern
                    """.trimIndent()
                )
            )
        ),

        // ── Init Patterns ────────────────────────────────────────────
        HelpTopic(
            title = "Init Patterns",
            icon = "🎯",
            sections = listOf(
                HelpSection(
                    title = "What Are Init Patterns?",
                    content = """
Initialization patterns determine how the grid is populated before the simulation starts. Different initial conditions can lead to very different outcomes for the same rule.
                    """.trimIndent()
                ),
                HelpSection(
                    title = "Available Patterns",
                    content = """
• **Auto (rule-based)** — uses the rule's preferred initialization; best default choice
• **Random 25%** — fills approximately 25% of cells randomly
• **Random 50%** — fills approximately 50% of cells randomly
• **Center Seed** — places a single live cell at the center of the grid
• **Gradient** — creates a smooth gradient fill across the grid
• **Checkerboard** — alternating filled and empty cells in a checkerboard pattern
• **Disk** — a filled circular region at the center of the grid
• **Ring** — a circular ring shape at the center
• **Symmetric Random** — a random pattern mirrored for symmetry
• **Cross** — a cross / plus shape at the center
• **Diagonal Stripes** — diagonal line patterns across the grid
                    """.trimIndent()
                ),
                HelpSection(
                    title = "Tips",
                    content = """
• **Center Seed** is ideal for elementary (1D) rules and symmetric rules
• **Random 50%** often produces the most complex behavior for life-like rules
• Try different patterns with the same rule to discover new emergent structures
                    """.trimIndent()
                )
            )
        ),

        // ── File Operations ──────────────────────────────────────────
        HelpTopic(
            title = "File Operations",
            icon = "💾",
            sections = listOf(
                HelpSection(
                    title = "Save & Load",
                    content = """
• **Save State** (**Ctrl+S** / **⌘S**) — saves the complete simulation snapshot including the rule, lattice type, grid dimensions, all cell states, color scheme, and current generation number
• **Load State** (**Ctrl+O** / **⌘O**) — restores a previously saved simulation exactly as it was

Saved files preserve everything needed to continue a simulation later or share it with others.
                    """.trimIndent()
                ),
                HelpSection(
                    title = "Export Image",
                    content = """
Press **E** to export the current grid as a PNG image. On desktop a file dialog lets you choose where to save it.
                    """.trimIndent()
                ),
                HelpSection(
                    title = "Record GIF",
                    content = """
Use the record button (⏺) in the toolbar to start recording an animated GIF. The simulation captures a frame each generation. Press the stop button to finish recording and save the file. A badge shows the current frame count while recording (max 500 frames).
                    """.trimIndent()
                )
            )
        ),

        // ── Tips & Tricks ────────────────────────────────────────────
        HelpTopic(
            title = "Tips & Tricks",
            icon = "💡",
            sections = listOf(
                HelpSection(
                    title = "Performance",
                    content = """
• Smaller grids run faster — start with 100×100 or 200×200 for experimentation
• Use **Fit to Window** (**F**) after changing grid size to re-center the view
• Close the analysis dashboard when you don't need it — analysis calculations run every generation
                    """.trimIndent()
                ),
                HelpSection(
                    title = "Interesting Combinations",
                    content = """
• Conway's Game of Life + Center Seed → watch gliders emerge from simple seeds
• Rule 30 + Center Seed → generates pseudorandom patterns from a single cell
• Continuous rules + Hexagonal lattice → smooth, organic textures
• Life-like rules + Random 50% → rich, chaotic initial dynamics that settle into structure
                    """.trimIndent()
                ),
                HelpSection(
                    title = "Exploration Tips",
                    content = """
• Use **draw mode** to manually set up interesting initial patterns
• Try the same rule with different lattice types — Moore vs. Von Neumann can produce very different behavior
• Watch the **Entropy** analysis to see how disorder evolves over time
• Pause the simulation and use **Step** to advance one generation at a time for careful observation
• Change the color scheme to highlight different features of the pattern
                    """.trimIndent()
                ),
                HelpSection(
                    title = "Color Schemes",
                    content = """
CellFie includes 12 color schemes: Rainbow, Kind of Blues, Fire, Green Ocean, Blue Diamond, Purple Haze, Water Lilies, Yellow Jacket, Black and White, White and Black, Chocolate, and Random. Change the color scheme in the settings panel to better visualize patterns — gradient schemes work well for multi-state rules, while binary schemes suit two-state rules.
                    """.trimIndent()
                )
            )
        )
    )
}
