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
    fun forestFireHasThreeStates() {
        val rule = ForestFire()
        assertEquals(3, rule.numStates)
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
}
