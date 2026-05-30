package org.tekfive.jfk

import org.tekfive.jfk.JsonValue.Companion.toJsonValue
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator
import kotlin.reflect.KProperty


/**
 * Returns true when this nullable value is either Kotlin `null` or [JsonNull].
 */
fun JsonValue?.isNullValue(): Boolean {
    return this == null || isNull
}

/**
 * Root type for all JFK JSON tree values.
 */
sealed interface JsonValue {

    /**
     * Path used in required-accessor error messages.
     */
    var _accessPath: List<String>

    /**
     * Returns the property named [key] if this value supports object access, otherwise [JsonNull].
     */
    operator fun get(key: String): JsonValue {
        JsonNull._accessPath = _accessPath + key
        return JsonNull
    }

    /**
     * Returns the property named after [prop].
     */
    operator fun get(prop: KProperty<*>): JsonValue = get(prop.name)

    /**
     * Returns the array element at [index] if this value supports array access, otherwise [JsonNull].
     */
    operator fun get(index: Int): JsonValue {
        JsonNull._accessPath = _accessPath + index.toString()
        return JsonNull
    }

    /**
     * Navigates a dot-separated object path from this value.
     */
    fun at(path: String): JsonValue {
        var current: JsonValue = this
        for (segment in path.split('.')) {
            current = current[segment]
        }
        return current
    }

    // Strict accessors — return null if the type doesn't match
    /** Returns this value as a [JsonObject], or `null` when it is not an object. */
    val obj: JsonObject? get() = null
    /** Returns this value as a [JsonArray], or `null` when it is not an array. */
    val array: JsonArray? get() = null
    /** Returns this value as a [String], or `null` when it is not a string. */
    val string: String? get() = null
    /** Returns this value as an [Int], or `null` when it is not an integral number in range. */
    val int: Int? get() = null
    /** Returns this value as a [Long], or `null` when it is not an integral number in range. */
    val long: Long? get() = null
    /** Returns this value as a [Double], or `null` when it is not a number. */
    val double: Double? get() = null
    /** Returns this value as a [Boolean], or `null` when it is not a boolean. */
    val boolean: Boolean? get() = null
    /** Returns true when this value is [JsonNull]. */
    val isNull: Boolean get() = false
    /** Returns the number of object entries or array elements, or `0` for scalar values. */
    val size: Int get() = 0

    // Lax accessors — coerce JsonString values to the target type
    /** Returns this value as a string, coercing compatible scalar values where supported. */
    val laxString: String? get() = string
    /** Returns this value as an int, coercing compatible strings. */
    val laxInt: Int? get() = int ?: (this as? JsonString)?.value?.toIntOrNull()
    /** Returns this value as a long, coercing compatible strings. */
    val laxLong: Long? get() = long ?: (this as? JsonString)?.value?.toLongOrNull()
    /** Returns this value as a finite double, coercing compatible strings. */
    val laxDouble: Double? get() = double ?: (this as? JsonString)?.value?.toDoubleOrNull()
        ?.takeIf { !it.isNaN() && !it.isInfinite() }
    /** Returns this value as a boolean, coercing `true` or `false` strings. */
    val laxBoolean: Boolean? get() = boolean ?: (this as? JsonString)?.value?.toBooleanStrictOrNull()

    // Req accessors — return the lax value or throw IllegalStateException with path and type info
    /** Returns [obj] or throws with path context. */
    val reqObj: JsonObject get() = obj ?: throwReq("Object")
    /** Returns [array] or throws with path context. */
    val reqArray: JsonArray get() = array ?: throwReq("Array")
    /** Returns [laxString] or throws with path context. */
    val reqString: String get() = laxString ?: throwReq("String")
    /** Returns [laxInt] or throws with path context. */
    val reqInt: Int get() = laxInt ?: throwReq("Int")
    /** Returns [laxLong] or throws with path context. */
    val reqLong: Long get() = laxLong ?: throwReq("Long")
    /** Returns [laxDouble] or throws with path context. */
    val reqDouble: Double get() = laxDouble ?: throwReq("Double")
    /** Returns [laxBoolean] or throws with path context. */
    val reqBoolean: Boolean get() = laxBoolean ?: throwReq("Boolean")


    /**
     * Returns [obj] or delegates failure handling to [notFound].
     */
    fun reqObj(notFound:() -> Nothing): JsonObject {
        return obj
            ?: notFound()
    }

    /**
     * Returns [array] or delegates failure handling to [notFound].
     */
    fun reqArray(notFound:() -> Nothing): JsonArray {
        return array
            ?: notFound()
    }

    /**
     * Returns [laxString] or delegates failure handling to [notFound].
     */
    fun reqString(notFound:() -> Nothing): String {
        return laxString
            ?: notFound()
    }

    /**
     * Returns [laxInt] or delegates failure handling to [notFound].
     */
    fun reqInt(notFound:() -> Nothing): Int {
        return laxInt
            ?: notFound()
    }

    /**
     * Returns [laxLong] or delegates failure handling to [notFound].
     */
    fun reqLong(notFound:() -> Nothing): Long {
        return laxLong
            ?: notFound()
    }

    /**
     * Returns [laxDouble] or delegates failure handling to [notFound].
     */
    fun reqDouble(notFound:() -> Nothing): Double {
        return laxDouble
            ?: notFound()
    }

    /**
     * Returns [laxBoolean] or delegates failure handling to [notFound].
     */
    fun reqBoolean(notFound:() -> Nothing): Boolean {
        return laxBoolean
            ?: notFound()
    }

    /**
     * Converts this [JsonValue] to a plain Kotlin value: [JsonObject] becomes a [Map],
     * [JsonArray] becomes a [List], and primitives become their Kotlin equivalents.
     */
    fun toValue(): Any? {
        return when (this) {
            is JsonObject -> toMap()
            is JsonArray -> toList()
            is JsonString -> value
            is JsonNumber -> value
            is JsonBool -> value
            is JsonNull -> null
        }
    }

    /**
     * Serializes this value to JSON.
     *
     * @param indent number of spaces per level for pretty output; `null` or `0` writes compact JSON.
     */
    fun toJsonString(indent: Int? = null): String = JsonWriter.write(this, indent ?: 0)

    companion object {
        /**
         * Converts a Kotlin value to the corresponding [JsonValue].
         *
         * Detects recursive object graphs for collections and maps.
         *
         * @param visitedValues recursion detection stack; callers normally omit this.
         */
        fun toJsonValue(value: Any?, visitedValues: MutableList<Any> = mutableListOf()): JsonValue {
            return if (value == null) {
                JsonNull
            } else {
                if (visitedValues.any { it === value }) {
                    throw IllegalArgumentException("Cannot encode recursive object trees for: $value (${value.javaClass.kotlin.qualifiedName})")
                }

                visitedValues.add(value)
                try {
                    when (value) {
                        is JsonValue -> value
                        is Boolean -> JsonBool(value)
                        is Number -> JsonNumber(value)
                        is String -> JsonString(value)
                        is ToJsonObject -> value.toJsonObject()
                        is ToJsonValue -> value.toJsonValue()
                        is Collection<*> -> JsonArray(value.map { toJsonValue(it, visitedValues) })
                        is Map<*, *> -> JsonObject(value.map { (key, value) ->
                            key.toString() to toJsonValue(value, visitedValues)
                        }.toMap())
                        is Pair<*, *> -> JsonObject(mapOf(
                            "first" to toJsonValue(value.first, visitedValues),
                            "second" to toJsonValue(value.second, visitedValues),
                        ))
                        else -> JsonString(value.toString())
                    }
                } finally {
                    visitedValues.removeAt(visitedValues.lastIndex)
                }
            }
        }
    }
}

private fun JsonValue.throwReq(expected: String): Nothing {
    val pathStr = if (_accessPath.isEmpty()) "<root>" else _accessPath.joinToString(".")
    val actual = when (this) {
        is JsonObject -> "JsonObject"
        is JsonArray -> "JsonArray"
        is JsonString -> "JsonString(\"$value\")"
        is JsonNumber -> "JsonNumber($value)"
        is JsonBool -> "JsonBool($value)"
        is JsonNull -> "JsonNull"
    }
    throw IllegalStateException("Required $expected at '$pathStr' but found $actual")
}



/**
 * JSON value that can contain nested scalar property paths.
 */
sealed interface JsonContainer : JsonValue {
    /**
     * Returns all dot-separated paths that resolve to scalar values below this container.
     */
    fun getScalarPropertyPaths(fieldPathStack: List<String> = listOf()): List<String>

    /**
     * Returns the value at a dot-separated path, or [JsonNull] when no value exists.
     */
    fun getValueAtPath(path: String): JsonValue
}

/**
 * Null-safe object property access.
 */
operator fun JsonObject?.get(path: String): JsonValue? {
    return this?.get(path)
}

/**
 * Mutable JSON object preserving insertion order.
 *
 * @param properties initial properties converted with JFK conversion rules.
 */
class JsonObject(properties: Map<String, Any?> = emptyMap()) : JsonContainer {

    private val _entries: MutableMap<String, JsonValue> = properties.map { (key, value) -> key to toJsonValue(value) }.toMap().toMutableMap()

    override var _accessPath: List<String> = emptyList()

    /** Object entries keyed by property name. */
    val entries: Map<String, JsonValue> get() = _entries

    override val obj = this

    override operator fun get(key: String): JsonValue {
        val value = _entries[key] ?: JsonNull
        value._accessPath = _accessPath + key
        return value
    }

    override operator fun get(prop: KProperty<*>): JsonValue = get(prop.name)

    override val size: Int get() = _entries.size

    /** Returns the named property as an object, or `null`. */
    fun obj(name: String): JsonObject? = get(name).obj
    /** Returns the named property as an array, or `null`. */
    fun array(name: String): JsonArray? = get(name).array
    /** Returns the named property as a string, or `null`. */
    fun string(name: String): String? = get(name).string
    /** Returns the named property as a long, or `null`. */
    fun long(name: String): Long? = get(name).long
    /** Returns the named property as a double, or `null`. */
    fun double(name: String): Double? = get(name).double
    /** Returns the named property as a boolean, or `null`. */
    fun boolean(name: String): Boolean? = get(name).boolean

    /** Returns the named property as a string with lax coercion. */
    fun laxString(name: String): String? = get(name).laxString
    /** Returns the named property as a long with lax coercion. */
    fun laxLong(name: String): Long? = get(name).laxLong
    /** Returns the named property as a double with lax coercion. */
    fun laxDouble(name: String): Double? = get(name).laxDouble
    /** Returns the named property as a boolean with lax coercion. */
    fun laxBoolean(name: String): Boolean? = get(name).laxBoolean

    /** Returns the named property as a required object. */
    fun reqObj(name: String): JsonObject = get(name).reqObj
    /** Returns the named property as a required array. */
    fun reqArray(name: String): JsonArray = get(name).reqArray
    /** Returns the named property as a required string. */
    fun reqString(name: String): String = get(name).reqString
    /** Returns the named property as a required long. */
    fun reqLong(name: String): Long = get(name).reqLong
    /** Returns the named property as a required double. */
    fun reqDouble(name: String): Double = get(name).reqDouble
    /** Returns the named property as a required boolean. */
    fun reqBoolean(name: String): Boolean = get(name).reqBoolean

    /** Sets [key] to [value] converted with JFK conversion rules. */
    operator fun set(key: String, value: Any?) { _entries[key] = toJsonValue(value) }

    /** Returns true when [key] is present, even if its value is [JsonNull]. */
    fun containsKey(key: String): Boolean = _entries.containsKey(key)
    /** Removes [key] and returns the previous value, if any. */
    fun remove(key: String): JsonValue? = _entries.remove(key)

    /** Object property names in insertion order. */
    val keys: Set<String> get() = _entries.keys
    /** Object values in insertion order. */
    val values: Collection<JsonValue> get() = _entries.values

    /** Returns true when [key] is present. */
    operator fun contains(key: String): Boolean = key in _entries

    override fun equals(other: Any?): Boolean =
        this === other || (other is JsonObject && _entries == other._entries)

    override fun hashCode(): Int = _entries.hashCode()

    override fun toString(): String = "JsonObject(entries=$_entries)"

    /**
     * Converts this object to a plain Kotlin map.
     */
    fun toMap(): Map<String, Any?> {
        return _entries.mapValues { (_, v) -> v.toValue() }
    }

    /**
     * Returns this object unchanged.
     */
    fun toJsonObject(): JsonObject = this

    override fun getScalarPropertyPaths(fieldPathStack: List<String>): List<String> {
        val fieldPaths = mutableListOf<String>()

        for ((name, child) in entries) {
            when (child) {
                is JsonObject -> {
                    fieldPaths.addAll(child.getScalarPropertyPaths(fieldPathStack + name))
                }

                is JsonArray -> {
                    fieldPaths.addAll(child.getScalarPropertyPaths(fieldPathStack + name))
                }

                else -> {
                    fieldPaths.add((fieldPathStack + name).joinToString("."))
                }
            }
        }

        return fieldPaths.distinct()
    }

    override fun toJsonString(indent: Int?): String = JsonWriter.write(this, indent ?: 0)

    override fun getValueAtPath(path: String): JsonValue {
        return getValueAtPath(path.split('.').map { it.trim() }.filter { it.isNotBlank() }.toMutableList())
    }

    internal fun getValueAtPath(path: MutableList<String>): JsonValue {
        if (path.isEmpty()) {
            return JsonNull
        }

        val name = path.removeFirst()

        val value = get(name)
        return if (path.isEmpty() || value is JsonNull) {
            value
        } else if (value is JsonObject) {
            value.getValueAtPath(path)
        } else if (value is JsonArray) {
            value.getValueAtPath(path)
        } else {
            JsonNull
        }
    }


    companion object {
        /**
         * Builds an object from already-converted JSON [entries].
         */
        operator fun invoke(entries: Map<String, JsonValue>): JsonObject {
            return JsonObject(LinkedHashMap(entries))
        }
    }

}

/**
 * Returns distinct scalar property paths across a list of JSON objects.
 */
fun List<JsonObject>.getScalarPropertyPaths(): List<String> {
    return this.flatMap { it.getScalarPropertyPaths() }.distinct()
}


/**
 * Mutable JSON array preserving element order.
 *
 * @param values initial elements converted with JFK conversion rules.
 */
class JsonArray(values: List<Any?> = emptyList()) : JsonContainer {

    private val _items: MutableList<JsonValue> = values.map { toJsonValue(it) }.toMutableList()

    override var _accessPath: List<String> = emptyList()

    /** Array elements in order. */
    val items: List<JsonValue> get() = _items

    override val array: JsonArray get() = this

    override operator fun get(index: Int): JsonValue {
        val value = if (index in _items.indices) _items[index] else JsonNull
        value._accessPath = _accessPath + index.toString()
        return value
    }
    override val size: Int get() = _items.size

    /** Replaces the element at [index] with [value] converted with JFK conversion rules. */
    operator fun set(index: Int, value: Any?) { _items[index] = toJsonValue(value) }

    /** Appends an existing JSON value. */
    fun add(value: JsonValue) { _items.add(value) }
    /** Appends a JSON string. */
    fun add(value: String) { _items.add(JsonString(value)) }
    /** Appends a JSON number. */
    fun add(value: Number) { _items.add(JsonNumber(value)) }
    /** Appends a JSON boolean. */
    fun add(value: Boolean) { _items.add(JsonBool(value)) }
    /** Appends a serialized [ToJsonObject]. */
    fun add(value: ToJsonObject) { _items.add(value.toJsonObject()) }
    /** Appends JSON null. */
    fun addNull() { _items.add(JsonNull) }
    /** Removes and returns the element at [index]. */
    fun removeAt(index: Int): JsonValue = _items.removeAt(index)

    /** Converts this array to a plain Kotlin list. */
    fun toList(): List<Any?> = _items.map { it.toValue() }

    /** Returns object elements, skipping non-object elements. */
    fun toObjList(): List<JsonObject> = _items.mapNotNull { it.obj }
    /** Maps object elements to [T], skipping non-objects and failed mappings. */
    fun <T> toLaxList(fromJson: FromJsonObject<T>): List<T> where T : Any = toObjList().mapNotNull { fromJson.fromJsonOptional(it) }

    /** Returns all elements that can be read as booleans using lax accessors. */
    fun toLaxBooleanList(): List<Boolean> = _items.mapNotNull { it.laxBoolean }
    /** Returns all elements that can be read as strings using lax accessors. */
    fun toLaxStringList(): List<String> = _items.mapNotNull { it.laxString }
    /** Returns all elements that can be read as ints using lax accessors. */
    fun toLaxIntList(): List<Int> = _items.mapNotNull { it.laxInt }
    /** Returns all elements that can be read as longs using lax accessors. */
    fun toLaxLongList(): List<Long> = _items.mapNotNull { it.laxLong }
    /** Returns all elements that can be read as doubles using lax accessors. */
    fun toLaxDoubleList(): List<Double> = _items.mapNotNull { it.laxDouble }

    /** Returns all elements as required objects. */
    fun toReqObjList(): List<JsonObject> = _items.map { it.reqObj }
    /** Maps all elements to [T], requiring every element to be an object. */
    fun <T> toList(fromJson: FromJsonObject<T>): List<T> where T : Any = toReqObjList().map { fromJson.fromJson(it) }

    /** Returns all elements as required booleans. */
    fun toReqBooleanList(): List<Boolean> = _items.map { it.reqBoolean }
    /** Returns all elements as required strings. */
    fun toReqStringList(): List<String> = _items.map { it.reqString }
    /** Returns all elements as required ints. */
    fun toReqIntList(): List<Int> = _items.map { it.reqInt }
    /** Returns all elements as required longs. */
    fun toReqLongList(): List<Long> = _items.map { it.reqLong }
    /** Returns all elements as required doubles. */
    fun toReqDoubleList(): List<Double> = _items.map { it.reqDouble }


    /** Returns true when [element] is present. */
    operator fun contains(element: JsonValue): Boolean = element in _items
    /** Iterates array elements in order. */
    operator fun iterator(): Iterator<JsonValue> = _items.iterator()

    override fun equals(other: Any?): Boolean =
        this === other || (other is JsonArray && _items == other._items)

    override fun hashCode(): Int = _items.hashCode()

    override fun toString(): String = "JsonArray(elements=$_items)"

    override fun getScalarPropertyPaths(fieldPathStack: List<String>): List<String> {
        val fieldPaths = mutableListOf<String>()

        for (child in items) {
            if (child is JsonObject) {
                fieldPaths.addAll(child.getScalarPropertyPaths(fieldPathStack))
            } else if (child is JsonArray) {
                fieldPaths.addAll(child.getScalarPropertyPaths(fieldPathStack))
            } else {
                fieldPaths.add(fieldPathStack.joinToString("."))
            }
        }

        return fieldPaths.distinct()
    }

    override fun getValueAtPath(path: String): JsonValue {
        return getValueAtPath(path.split('.').map { it.trim() }.filter { it.isNotBlank() }.toMutableList())
    }

    internal fun getValueAtPath(path: MutableList<String>): JsonValue {
        if (path.isEmpty()) {
            return JsonNull
        }
        
        val values = mutableListOf<JsonValue>()
        for (item in items) {
            if (item is JsonObject) {
                val value = item.getValueAtPath( mutableListOf<String>().also { it.addAll(path) })
                if (value !is JsonNull) {
                    values.add(value)
                }
            } else if (item is JsonArray) {
                val value = item.getValueAtPath( mutableListOf<String>().also { it.addAll(path) })
                if (value !is JsonNull) {
                    if (value is JsonArray) {
                        values.addAll(value.items)
                    } else {
                        values.add(value)
                    }
                }
            }
        }

        return if (values.isEmpty()) {
            JsonNull
        } else if (values.size == 1) {
            values[0]
        } else {
            JsonArray(values)
        }
    }
    
    companion object {
        /**
         * Builds an array from already-converted JSON [elements].
         */
        operator fun invoke(elements: List<JsonValue>): JsonArray =
            JsonArray(elements.toMutableList())
    }
}

/**
 * JSON string value.
 *
 * @property value raw string value.
 */
data class JsonString(val value: String) : JsonValue {
    override var _accessPath: List<String> = emptyList()
    override val string: String get() = value
    override val laxString: String get() = value
}

/**
 * JSON number value.
 *
 * @property value finite numeric value.
 */
data class JsonNumber(val value: Number) : JsonValue {
    init {
        val double = value.toDouble()
        require(!double.isNaN() && !double.isInfinite()) { "JSON numbers must be finite: $value" }
    }

    override var _accessPath: List<String> = emptyList()
    override val int: Int? get() = value.integralLongOrNull()
        ?.takeIf { it in Int.MIN_VALUE..Int.MAX_VALUE }
        ?.toInt()
    override val long: Long? get() = value.integralLongOrNull()
    override val double: Double? get() = value.toDouble()
    override val laxString: String get() = value.toString()
}

private fun Number.integralLongOrNull(): Long? {
    return when (this) {
        is Byte, is Short, is Int, is Long -> toLong()
        is Float, is Double -> {
            val double = toDouble()
            if (double.isFiniteLong()) double.toLong() else null
        }
        else -> {
            val double = toDouble()
            if (double.isFiniteLong()) double.toLong() else null
        }
    }
}

private fun Double.isFiniteLong(): Boolean {
    return !isNaN() && !isInfinite() &&
        this >= Long.MIN_VALUE.toDouble() &&
        this <= Long.MAX_VALUE.toDouble() &&
        this % 1.0 == 0.0
}

/**
 * JSON boolean value.
 *
 * @property value raw boolean value.
 */
data class JsonBool(val value: Boolean) : JsonValue {
    override var _accessPath: List<String> = emptyList()
    override val boolean: Boolean get() = value
    override val laxString: String get() = value.toString()
}

/**
 * JSON null singleton.
 */
object JsonNull : JsonValue {
    private val _threadLocalPath = ThreadLocal.withInitial<List<String>> { emptyList() }
    override var _accessPath: List<String>
        get() = _threadLocalPath.get()
        set(value) { _threadLocalPath.set(value) }
    override val isNull: Boolean get() = true

    override fun equals(other: Any?): Boolean = other is JsonNull
    override fun hashCode(): Int = 0
    override fun toString(): String = "JsonNull"
}
