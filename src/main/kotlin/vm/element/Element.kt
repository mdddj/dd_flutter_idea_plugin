package vm.element

import com.google.gson.JsonArray
import com.google.gson.JsonNull
import com.google.gson.JsonObject

/**
 * Superclass for all observatory elements.
 */
open class Element protected constructor(val json: JsonObject) {

    /**
     * A utility method to handle null values and JsonNull values.
     */
    protected fun getAsString(name: String): String? {
        val element = json.get(name)
        return if (element == null || element === JsonNull.INSTANCE) null else element.asString
    }

    /**
     * A utility method to handle null values and JsonNull values.
     */
    protected fun getAsInt(name: String): Int {
        val element = json.get(name)
        return if (element == null || element === JsonNull.INSTANCE) -1 else element.asInt
    }

    protected fun getAsLong(name: String): Long {
        val element = json.get(name)
        return if (element == null || element === JsonNull.INSTANCE) -1 else element.asLong
    }

    /**
     * A utility method to handle null values and JsonNull values.
     */
    protected fun getAsBoolean(name: String): Boolean {
        val element = json.get(name)
        return if (element == null || element === JsonNull.INSTANCE) false else element.asBoolean
    }


    /**
     * Return a specific JSON member as a list of integers.
     */
    protected fun getListInt(memberName: String): List<Int> {
        return jsonArrayToListInt(json.getAsJsonArray(memberName))!!
    }

    /**
     * Return a specific JSON member as a list of strings.
     */
    protected fun getListString(memberName: String): List<String> {
        return jsonArrayToListString(json.getAsJsonArray(memberName))!!
    }

    /**
     * Return a specific JSON member as a list of list of integers.
     */
    protected fun getListListInt(memberName: String): List<List<Int>>? {
        val array = json.getAsJsonArray(memberName)
        if (array == null) {
            return null
        }
        val size = array.size()
        val result = ArrayList<List<Int>>()
        for (index in 0 until size) {
            jsonArrayToListInt(array.get(index).asJsonArray)?.let { result.add(it) }
        }
        return result
    }

    private fun jsonArrayToListInt(array: JsonArray?): List<Int>? {
        if (array == null) {
            return null
        }
        val size = array.size()
        val result = ArrayList<Int>()
        for (index in 0 until size) {
            result.add(array.get(index).asInt)
        }
        return result
    }

    private fun jsonArrayToListString(array: JsonArray?): List<String> {
        if (array == null) {
            return listOf()
        }
        val size = array.size()
        val result = ArrayList<String>()
        for (index in 0 until size) {
            val elem = array.get(index)
            if (elem !== JsonNull.INSTANCE) {
                result.add(elem.asString)
            }
        }
        return result
    }

    override fun toString(): String {
        return json.toString()
    }

}
