package org.tekfive.jfk

/**
 * Builds a [JsonObject] using the JFK object DSL.
 */
fun json(block: JsonObjectBuilder.() -> Unit): JsonObject {
    val builder = JsonObjectBuilder()
    builder.block()
    return builder.build()
}

/**
 * Mutable builder used by [json] for object literals.
 */
class JsonObjectBuilder {
    private val map = LinkedHashMap<String, JsonValue>()

    /** Sets this property name to a JSON string or null. */
    infix fun String.set(value: String?) {
        map[this] = if (value == null) {
            JsonNull
        } else {
            JsonString(value)
        }
    }

    /** Sets this property name to a JSON number or null. */
    infix fun String.set(value: Number?) {
        map[this] = if (value == null) {
            JsonNull
        } else {
            JsonNumber(value)
        }
    }

    /** Sets this property name to a JSON boolean. */
    infix fun String.set(value: Boolean) { map[this] = JsonBool(value) }

    /** Sets this property name to JSON null. */
    infix fun String.set(value: Nothing?) { map[this] = JsonNull }

    /** Sets this property name to a JSON value or JSON null. */
    infix fun String.set(value: JsonValue?) { map[this] = value ?: JsonNull }

    /** Sets this property name to a serialized [ToJsonObject] or JSON null. */
    infix fun String.set(value: ToJsonObject?) { map[this] = value?.toJsonObject() ?: JsonNull }

    /** Sets this property name to an array of serialized [ToJsonObject] values. */
    infix fun String.set(values: Collection<*>) { map[this] = JsonArray(values) }

    /** Sets this property name to an enum constant name or JSON null. */
    infix fun String.setEnum(value: Enum<*>?) { map[this] = value?.let { JsonString(it.name) } ?: JsonNull }

    /** Returns the built [JsonObject]. */
    fun build(): JsonObject = JsonObject(map.toMap())
}

/**
 * Builds a [JsonArray] from vararg elements using JFK conversion rules.
 */
fun jsonArray(vararg elements: Any?): JsonArray =
    JsonArray(elements.map { it.toJsonValue() })

/**
 * Builds a [JsonArray] using the JFK array DSL.
 */
fun jsonArray(block: JsonArrayBuilder.() -> Unit): JsonArray {
    val builder = JsonArrayBuilder()
    builder.block()
    return builder.build()
}

/**
 * Mutable builder used by [jsonArray] for array literals.
 */
class JsonArrayBuilder {
    private val elements = mutableListOf<JsonValue>()

    /** Appends a JSON string. */
    fun add(value: String) { elements += JsonString(value) }

    /** Appends a JSON number. */
    fun add(value: Number) { elements += JsonNumber(value) }

    /** Appends a JSON boolean. */
    fun add(value: Boolean) { elements += JsonBool(value) }

    /** Appends JSON null. */
    fun addNull() { elements += JsonNull }

    /** Appends an existing JSON value. */
    fun add(value: JsonValue) { elements += value }

    /** Appends a serialized [ToJsonObject]. */
    fun add(value: ToJsonObject) { elements += value.toJsonObject() }

    /** Appends an object built by [JsonObjectBuilder]. */
    fun addObject(block: JsonObjectBuilder.() -> Unit) { elements += json(block) }

    /** Appends an array built by [JsonArrayBuilder]. */
    fun addArray(block: JsonArrayBuilder.() -> Unit) { elements += jsonArray(block) }

    /** Appends this string as a JSON string. */
    operator fun String.unaryPlus() { elements += JsonString(this) }

    /** Appends this JSON value. */
    operator fun JsonValue.unaryPlus() { elements += this }

    /** Returns the built [JsonArray]. */
    fun build(): JsonArray = JsonArray(elements.toList())
}

internal fun Any?.toJsonValue(): JsonValue = JsonValue.toJsonValue(this)
