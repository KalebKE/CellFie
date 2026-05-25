package org.caexplorer.domain

import org.caexplorer.domain.cell.Cell
import org.caexplorer.domain.cellstate.IntegerCellState
import org.caexplorer.domain.lattice.SquareLattice
import org.caexplorer.domain.rule.implementations.*
import org.caexplorer.domain.util.Coordinate
import kotlin.test.*

class RuleTest {

    private fun createGrid(
        width: Int, height: Int,
        initState: (Coordinate) -> Int = { 0 }
    ): SquareLattice {
        return SquareLattice(width, height) { coord ->
            Cell(IntegerCellState(initState(coord)), coord)
        }
    }

    @Test
    fun lifeDeadCellWithThreeNeighborsBorn() {
        val rule = Life()
        // 3x3 grid, center dead, 3 live neighbors
        val lattice = createGrid(3, 3) { coord ->
            if (coord in listOf(Coordinate(0, 0), Coordinate(0, 1), Coordinate(0, 2))) 1 else 0
        }
        val center = lattice.getCell(1, 1)!!
        val neighbors = lattice.getNeighbors(center)
        val newState = rule.nextState(center, neighbors)
        assertEquals(1, newState.toInt())
    }

    @Test
    fun lifeLiveCellWithTwoNeighborsSurvives() {
        val rule = Life()
        val lattice = createGrid(3, 3) { coord ->
            if (coord in listOf(Coordinate(1, 1), Coordinate(0, 0), Coordinate(0, 1))) 1 else 0
        }
        val center = lattice.getCell(1, 1)!!
        val neighbors = lattice.getNeighbors(center)
        val newState = rule.nextState(center, neighbors)
        assertEquals(1, newState.toInt())
    }

    @Test
    fun lifeLiveCellWithOneNeighborDies() {
        val rule = Life()
        val lattice = createGrid(3, 3) { coord ->
            if (coord in listOf(Coordinate(1, 1), Coordinate(0, 0))) 1 else 0
        }
        val center = lattice.getCell(1, 1)!!
        val neighbors = lattice.getNeighbors(center)
        val newState = rule.nextState(center, neighbors)
        assertEquals(0, newState.toInt())
    }

    @Test
    fun briansBrainThreeStates() {
        val rule = BriansBrain()
        assertEquals(3, rule.numStates)
        assertEquals("Brian's Brain", rule.displayName)
    }

    @Test
    fun cyclicCAAdvancesToNextState() {
        val rule = CyclicCA(numStates = 14)
        assertEquals(14, rule.numStates)
        // Cell at state 5 with a neighbor at state 6 should advance
        val lattice = createGrid(3, 3) { coord ->
            when (coord) {
                Coordinate(1, 1) -> 5
                Coordinate(0, 0) -> 6
                else -> 0
            }
        }
        val center = lattice.getCell(1, 1)!!
        val neighbors = lattice.getNeighbors(center)
        val newState = rule.nextState(center, neighbors)
        assertEquals(6, newState.toInt())
    }

    @Test
    fun wolframRuleProperties() {
        val rule30 = WolframRule(30)
        assertEquals("Rule 30", rule30.displayName)
        assertEquals(2, rule30.numStates)

        val rule110 = WolframRule(110)
        assertEquals("Rule 110", rule110.displayName)
    }

    @Test
    fun forestFireHasEightStates() {
        val rule = ForestFire()
        assertEquals(8, rule.numStates)
    }

    @Test
    fun wireworldHasFourStates() {
        val rule = Wireworld()
        assertEquals(4, rule.numStates)
    }

    @Test
    fun rockPaperScissorsHasThreeStates() {
        val rule = RockPaperScissors()
        assertEquals(3, rule.numStates)
    }

    @Test
    fun ruleRegistryHasRules() {
        val rules = org.caexplorer.domain.rule.RuleRegistry.getFeaturedRules()
        assertTrue(rules.size > 20, "Expected 20+ rules, got ${rules.size}")
    }

    @Test
    fun ruleRegistryIncludesWolframRules() {
        val all = org.caexplorer.domain.rule.RuleRegistry.getAll()
        assertTrue(all.size > 256, "Expected 256+ total rules, got ${all.size}")
    }

    /**
     * Test that Turing Machine "Bouncing Line" moves the head correctly.
     * The head should alternate E and W, extending a line of 1s.
     */
    @Test
    fun turingMachineBouncingLine() {
        val tm = TuringMachine(numStates = 3, programName = "Bouncing Line")
        val size = 11
        val center = size / 2
        val tapeHead = 2 // numStates - 1

        val lattice = createGrid(size, size) { coord ->
            if (coord.row == center && coord.col == center) tapeHead else 0
        }

        // Simulate 4 steps, processing all cells in order (like engine)
        repeat(4) {
            val newStates = mutableMapOf<Cell, IntegerCellState>()
            for (cell in lattice.cells) {
                val neighbors = lattice.getNeighbors(cell)
                newStates[cell] = tm.nextState(cell, neighbors) as IntegerCellState
            }
            for ((cell, state) in newStates) {
                cell.addNewState(state)
            }
        }

        // After 4 steps, the head should have moved and left 1s behind
        // Verify the head exists somewhere on the center row
        var headCount = 0
        var oneCount = 0
        for (col in 0 until size) {
            val cell = lattice.getCell(center, col)!!
            val state = cell.currentState.toInt()
            if (state == tapeHead) headCount++
            if (state == 1) oneCount++
        }
        assertEquals(1, headCount, "Should have exactly one tape head")
        assertTrue(oneCount > 0, "Should have written at least one '1' on the tape")
    }

    /**
     * Test Turing Machine works even when cells are processed in REVERSE order
     * (simulates worst-case in-place update scenario).
     */
    @Test
    fun turingMachineReverseProcessingOrder() {
        val tm = TuringMachine(numStates = 3, programName = "Bouncing Line")
        val size = 11
        val center = size / 2
        val tapeHead = 2

        val lattice = createGrid(size, size) { coord ->
            if (coord.row == center && coord.col == center) tapeHead else 0
        }

        // Simulate 4 steps, processing cells in REVERSE order
        repeat(4) {
            val reversedCells = lattice.cells.reversed()
            for (cell in reversedCells) {
                val neighbors = lattice.getNeighbors(cell)
                val newState = tm.nextState(cell, neighbors)
                cell.addNewState(newState)
            }
        }

        // Should still work correctly
        var headCount = 0
        var oneCount = 0
        for (col in 0 until size) {
            val cell = lattice.getCell(center, col)!!
            val state = cell.currentState.toInt()
            if (state == tapeHead) headCount++
            if (state == 1) oneCount++
        }
        assertEquals(1, headCount, "Should have exactly one tape head (reverse order)")
        assertTrue(oneCount > 0, "Should have written 1s on the tape (reverse order)")
    }

    /**
     * Test that Classic BB-3 (3-state, 2-symbol Busy Beaver) runs for multiple
     * generations before halting. It should write exactly 6 ones on the tape.
     * Busy Beavers are deterministic and SHOULD halt — that's their defining property.
     */
    @Test
    fun turingMachineClassicBB3Halts() {
        val tm = TuringMachine(numStates = 3, programName = "Busy Beaver", bbStates = 3)
        val size = 21
        val center = size / 2
        val tapeHead = 2

        val lattice = createGrid(size, size) { coord ->
            if (coord.row == center && coord.col == center) tapeHead else 0
        }

        // Run up to 100 generations, counting how many actually change something
        var stepsWithChange = 0
        for (step in 0 until 100) {
            val newStates = mutableMapOf<Cell, IntegerCellState>()
            for (cell in lattice.cells) {
                val neighbors = lattice.getNeighbors(cell)
                newStates[cell] = tm.nextState(cell, neighbors) as IntegerCellState
            }
            var changed = false
            for ((cell, state) in newStates) {
                if (cell.currentState.toInt() != state.toInt()) changed = true
                cell.addNewState(state)
            }
            if (changed) stepsWithChange++
            else break
        }

        // BB-3 should run for at least 5 generations (it halts around 12-14)
        assertTrue(stepsWithChange >= 5,
            "BB-3 should run for multiple steps, but only ran $stepsWithChange")

        // Count ones on the center row (head marker hides one 1 underneath)
        var oneCount = 0
        var headCount = 0
        for (col in 0 until size) {
            val cell = lattice.getCell(center, col)!!
            val s = cell.currentState.toInt()
            if (s == 1) oneCount++
            if (s == tapeHead) headCount++
        }
        // BB-3 writes 6 ones total on the tape
        assertEquals(1, headCount, "Should have exactly one head marker")
        assertTrue(oneCount >= 5 && oneCount <= 6,
            "Classic BB-3 should produce 5-6 visible ones, but got $oneCount")
    }

    @Test
    fun turingMachineBB2Halts() {
        val tm = TuringMachine(numStates = 3, programName = "Busy Beaver", bbStates = 2)
        val size = 21
        val center = size / 2
        val tapeHead = 2

        val lattice = createGrid(size, size) { coord ->
            if (coord.row == center && coord.col == center) tapeHead else 0
        }

        var stepsWithChange = 0
        for (step in 0 until 100) {
            val newStates = mutableMapOf<Cell, IntegerCellState>()
            for (cell in lattice.cells) {
                val neighbors = lattice.getNeighbors(cell)
                newStates[cell] = tm.nextState(cell, neighbors) as IntegerCellState
            }
            var changed = false
            for ((cell, state) in newStates) {
                if (cell.currentState.toInt() != state.toInt()) changed = true
                cell.addNewState(state)
            }
            if (changed) stepsWithChange++
            else break
        }

        assertTrue(stepsWithChange in 5..10,
            "BB-2 should halt around 6-7 steps, but ran $stepsWithChange")

        var oneCount = 0
        var headCount = 0
        for (col in 0 until size) {
            val cell = lattice.getCell(center, col)!!
            val s = cell.currentState.toInt()
            if (s == 1) oneCount++
            if (s == tapeHead) headCount++
        }
        assertEquals(1, headCount, "Should have exactly one head marker")
        // BB-2 writes 4 ones; head may rest on one, hiding it
        assertTrue(oneCount in 3..4,
            "BB-2 should have 3-4 visible ones, but got $oneCount")
    }

    @Test
    fun turingMachineBB4Halts() {
        val tm = TuringMachine(numStates = 3, programName = "Busy Beaver", bbStates = 4)
        val size = 51
        val center = size / 2
        val tapeHead = 2

        val lattice = createGrid(size, size) { coord ->
            if (coord.row == center && coord.col == center) tapeHead else 0
        }

        var stepsWithChange = 0
        for (step in 0 until 200) {
            val newStates = mutableMapOf<Cell, IntegerCellState>()
            for (cell in lattice.cells) {
                val neighbors = lattice.getNeighbors(cell)
                newStates[cell] = tm.nextState(cell, neighbors) as IntegerCellState
            }
            var changed = false
            for ((cell, state) in newStates) {
                if (cell.currentState.toInt() != state.toInt()) changed = true
                cell.addNewState(state)
            }
            if (changed) stepsWithChange++
            else break
        }

        assertTrue(stepsWithChange in 50..200,
            "BB-4 should halt around 107-108 steps, but ran $stepsWithChange")

        var oneCount = 0
        var headCount = 0
        for (col in 0 until size) {
            val cell = lattice.getCell(center, col)!!
            val s = cell.currentState.toInt()
            if (s == 1) oneCount++
            if (s == tapeHead) headCount++
        }
        assertEquals(1, headCount, "Should have exactly one head marker, but got $headCount")
        assertEquals(13, oneCount,
            "BB-4 should produce 13 ones on tape, but got $oneCount")
    }
}
