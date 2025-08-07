package vm.element

import com.google.gson.JsonArray
import com.google.gson.JsonObject

/**
 * Simple wrapper around a {@link JsonArray} which lazily converts {@link JsonObject} elements to
 * subclasses of {@link Element}. Subclasses need only implement {@link #basicGet(JsonArray, int)}
 * to return an {@link Element} subclass for the {@link JsonObject} at a given index.
 */
abstract class ElementList<T>(private val array: JsonArray) : Iterable<T> {

    operator fun get(index: Int): T {
        return basicGet(array, index)
    }

    fun isEmpty(): Boolean {
        return size() == 0
    }

    override fun iterator(): Iterator<T> {
        return object : Iterator<T> {
            var index = 0

            override fun hasNext(): Boolean {
                return index < size()
            }

            override fun next(): T {
                return get(index++)
            }
        }
    }

    fun size(): Int {
        return array.size()
    }

    protected abstract fun basicGet(array: JsonArray, index: Int): T
}
