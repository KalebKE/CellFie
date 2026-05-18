package org.caexplorer.domain.util

/**
 * A fixed-capacity circular buffer that overwrites the oldest element when full.
 * All operations are O(1). Not thread-safe — external synchronization required.
 *
 * Port of Java FiniteArrayList.
 */
class RingBuffer<T : Any>(val capacity: Int) {

    init {
        require(capacity > 0) { "Capacity must be positive, was $capacity" }
    }

    @Suppress("UNCHECKED_CAST")
    private val elements: Array<Any?> = arrayOfNulls(capacity)
    private var end: Int = capacity // Will wrap to 0 on first add
    private var numElements: Int = 0

    val size: Int get() = numElements
    val isEmpty: Boolean get() = numElements == 0
    val isNotEmpty: Boolean get() = numElements > 0

    fun add(element: T) {
        end = (end + 1) % capacity
        elements[end] = element
        if (numElements < capacity) numElements++
    }

    operator fun get(index: Int): T {
        checkIndex(index)
        return elements[mapIndex(index)] as T
    }

    operator fun set(index: Int, element: T) {
        checkIndex(index)
        elements[mapIndex(index)] = element
    }

    val first: T
        get() {
            check(numElements > 0) { "RingBuffer is empty" }
            return elements[mapIndex(0)] as T
        }

    val last: T
        get() {
            check(numElements > 0) { "RingBuffer is empty" }
            return elements[end] as T
        }

    fun setFirst(element: T) {
        check(numElements > 0) { "RingBuffer is empty" }
        elements[mapIndex(0)] = element
    }

    fun setLast(element: T) {
        check(numElements > 0) { "RingBuffer is empty" }
        elements[end] = element
    }

    fun removeFirst(): T {
        check(numElements > 0) { "RingBuffer is empty" }
        val element = elements[mapIndex(0)] as T
        numElements--
        return element
    }

    fun removeLast(): T {
        check(numElements > 0) { "RingBuffer is empty" }
        val element = elements[end] as T
        end = if (end > 0) end - 1 else capacity - 1
        numElements--
        return element
    }

    fun clear() {
        elements.fill(null)
        end = capacity
        numElements = 0
    }

    private fun mapIndex(index: Int): Int {
        var pos = (end + 1) - numElements + index
        if (pos < 0) pos += capacity
        return pos % capacity
    }

    private fun checkIndex(index: Int) {
        if (index < 0 || index >= numElements) {
            throw IndexOutOfBoundsException("Index $index out of bounds for size $numElements")
        }
    }
}
