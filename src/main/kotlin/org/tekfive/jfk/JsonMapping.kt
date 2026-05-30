package org.tekfive.jfk

/**
 * Exception thrown when a [JsonObject] cannot be mapped to a target Kotlin type.
 *
 * @property path dot/bracket path to the failing JSON value.
 * @property expected human-readable expected type or shape.
 * @property actual actual JSON value encountered at [path].
 * @param detail optional custom exception message.
 */
class JsonMappingException(
    val path: String,
    val expected: String,
    val actual: JsonValue,
    detail: String? = null,
) : Exception(
    detail ?: "Required $expected at '${path.ifEmpty { "<root>" }}' but found ${describeValue(actual)}"
) {
    companion object {
        internal fun describeValue(value: JsonValue): String = when (value) {
            is JsonObject -> "JsonObject (${value.size} entries)"
            is JsonArray -> "JsonArray (${value.size} elements)"
            is JsonString -> "JsonString(\"${value.value}\")"
            is JsonNumber -> "JsonNumber(${value.value})"
            is JsonBool -> "JsonBool(${value.value})"
            is JsonNull -> "JsonNull"
        }
    }
}
