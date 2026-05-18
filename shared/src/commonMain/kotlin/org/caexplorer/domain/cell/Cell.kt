package org.caexplorer.domain.cell

import org.caexplorer.domain.cellstate.CellState
import org.caexplorer.domain.util.Coordinate
import org.caexplorer.domain.util.RingBuffer

/**
 * Represents a single cell in a cellular automaton lattice.
 *
 * Each cell maintains a ring-buffer history of states, supports tagging for
 * analysis overlays, and provides O(1) access to current/previous states.
 *
 * Thread safety: All mutable operations use synchronization. The simulation
 * engine partitions cells across coroutines — each cell is only updated by
 * one coroutine at a time, but may be read by analyses concurrently.
 *
 * Port of Java Cell class.
 */
class Cell(
    initialState: CellState,
    val coordinate: Coordinate,
    maxHistorySize: Int = MIN_HISTORY,
    additionalStates: List<CellState> = emptyList()
) {
    private val stateHistory: RingBuffer<CellState>

    @Volatile
    private var _currentState: CellState

    @Volatile
    private var _previousState: CellState?

    @Volatile
    var generation: Int = 0
        private set

    /** Arbitrary metadata (e.g., Margolus partition position). */
    private var metadata: MutableMap<String, Any>? = null

    init {
        val capacity = maxHistorySize.coerceAtLeast(MIN_HISTORY)
        stateHistory = RingBuffer(capacity)

        // Add any required initial generations first
        for (state in additionalStates) {
            stateHistory.add(state)
        }
        stateHistory.add(initialState)

        _currentState = initialState
        _previousState = if (stateHistory.size >= 2) stateHistory[stateHistory.size - 2] else null
        generation = stateHistory.size - 1
    }

    /** Current cell state (fast cached access). */
    val currentState: CellState get() = _currentState

    /** Previous cell state (generation - 1), or null if at generation 0. */
    val previousState: CellState? get() = _previousState

    /**
     * Add a new state to this cell, advancing the generation.
     * Propagates tagging from the previous state.
     */
    @Synchronized
    fun addNewState(state: CellState) {
        val oldState = _currentState
        _previousState = oldState
        _currentState = state
        stateHistory.add(state)

        // Propagate tagging
        if (oldState.isTagged) {
            for (obj in oldState.taggingObjects) {
                state.tag(obj)
            }
        }
        generation++
    }

    /**
     * Get the state at a specific generation, or null if no longer stored.
     */
    @Synchronized
    fun getState(atGeneration: Int): CellState? {
        if (atGeneration == generation) return _currentState
        if (atGeneration == generation - 1) return _previousState

        val offset = generation - atGeneration
        if (offset < 0 || offset >= stateHistory.size) return null

        val index = stateHistory.size - 1 - offset
        return if (index >= 0) stateHistory[index] else null
    }

    /**
     * Get the last N states (most recent last), up to history capacity.
     */
    @Synchronized
    fun getRecentStates(count: Int): List<CellState> {
        val n = count.coerceAtMost(stateHistory.size)
        val start = stateHistory.size - n
        return (start until stateHistory.size).map { stateHistory[it] }
    }

    /**
     * Replace the current state without advancing the generation.
     * Preserves tagging.
     */
    @Synchronized
    fun resetState(state: CellState) {
        val oldState = _currentState
        if (oldState.isTagged) {
            for (obj in oldState.taggingObjects) {
                state.tag(obj)
            }
        }
        _currentState = state
        if (stateHistory.isEmpty) {
            stateHistory.add(state)
        } else {
            stateHistory.setLast(state)
        }
    }

    /**
     * Rewind one generation (if possible).
     * Returns true if the rewind was successful.
     */
    @Synchronized
    fun rewind(): Boolean {
        if (stateHistory.size <= 1) return false
        _currentState = _previousState ?: return false
        _previousState = if (stateHistory.size >= 3) stateHistory[stateHistory.size - 3] else null
        stateHistory.removeLast()
        generation--
        return true
    }

    /** Whether the current state is tagged for analysis highlighting. */
    val isTagged: Boolean get() = _currentState.isTagged

    /**
     * Tag/untag this cell for analysis overlays.
     */
    @Synchronized
    fun setTagged(tagged: Boolean, taggingObject: Any) {
        if (tagged) {
            _currentState.tag(taggingObject)
        } else {
            _currentState.untag(taggingObject)
            // Also untag all historical states
            for (i in 0 until stateHistory.size) {
                stateHistory[i].untag(taggingObject)
            }
        }
    }

    /** Get/set arbitrary cell metadata. */
    @Synchronized
    fun getMetadata(key: String): Any? = metadata?.get(key)

    @Synchronized
    fun setMetadata(key: String, value: Any) {
        if (metadata == null) metadata = mutableMapOf()
        metadata!![key] = value
    }

    /** Convert current state to integer. */
    fun toInt(): Int = _currentState.toInt()

    override fun toString(): String = _currentState.toString()

    companion object {
        const val MAX_HISTORY = 1000
        const val MIN_HISTORY = 2
    }
}
