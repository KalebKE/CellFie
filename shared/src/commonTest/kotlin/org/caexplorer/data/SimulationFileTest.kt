package org.caexplorer.data

import kotlin.test.*

class SimulationFileTest {

    private fun sampleData() = SimulationFileData(
        ruleName = "Life",
        latticeType = "Square (Moore)",
        width = 3,
        height = 3,
        cellStates = intArrayOf(0, 1, 0, 1, 1, 1, 0, 1, 0),
        colorSchemeIndex = 2,
        generation = 42,
        version = 1
    )

    @Test
    fun serializeAndDeserializeRoundTrip() {
        val original = sampleData()
        val text = serializeSimulation(original)
        val deserialized = deserializeSimulation(text)

        assertNotNull(deserialized)
        assertEquals(original, deserialized)
    }

    @Test
    fun deserializeInvalidInputReturnsNull() {
        assertNull(deserializeSimulation(""))
        assertNull(deserializeSimulation("garbage data"))
        assertNull(deserializeSimulation("CAExplorerMP v1\nrule=Life"))  // too few lines
        assertNull(deserializeSimulation(
            "CAExplorerMP v1\nrule=Life\nlattice=Square\nwidth=3\nheight=3\n" +
                    "colorScheme=0\ngeneration=0\nstates=0,1" // wrong size: 2 != 3*3
        ))
    }

    @Test
    fun serializePreservesAllFields() {
        val data = sampleData()
        val text = serializeSimulation(data)

        assertTrue(text.contains("rule=Life"))
        assertTrue(text.contains("lattice=Square (Moore)"))
        assertTrue(text.contains("width=3"))
        assertTrue(text.contains("height=3"))
        assertTrue(text.contains("colorScheme=2"))
        assertTrue(text.contains("generation=42"))
        assertTrue(text.contains("states=0,1,0,1,1,1,0,1,0"))
    }

    @Test
    fun versionHeaderIsCorrect() {
        val data = sampleData()
        val text = serializeSimulation(data)
        val firstLine = text.lines().first()
        assertEquals("CAExplorerMP v1", firstLine)
    }
}
