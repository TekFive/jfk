package org.tekfive.jfk

import java.io.InputStream
import java.io.Reader
import java.io.Writer

/**
 * Entry point for parsing JSON into the JFK tree model.
 */
object Json {

    /**
     * Parses [input] into a [JsonValue].
     *
     * @param transform optional parse-time value transformer/default provider.
     * @param config parser security and size limits.
     * @throws JsonParseException when the input is not valid JSON or exceeds [config].
     */
    fun parse(
        input: String,
        transform: JsonTransform? = null,
        config: JsonParserConfig = JsonParserConfig.DEFAULT,
    ): JsonValue = JsonParser(input, transform, config).parse()

    /**
     * Parses JSON read from [reader] into a [JsonValue].
     *
     * @param transform optional parse-time value transformer/default provider.
     * @param config parser security and size limits.
     * @throws JsonParseException when the input is not valid JSON or exceeds [config].
     */
    fun parse(
        reader: Reader,
        transform: JsonTransform? = null,
        config: JsonParserConfig = JsonParserConfig.DEFAULT,
    ): JsonValue = JsonParser(reader, transform, config).parse()

    /**
     * Parses UTF-8-compatible text read from [input] into a [JsonValue].
     *
     * @param transform optional parse-time value transformer/default provider.
     * @param config parser security and size limits.
     * @throws JsonParseException when the input is not valid JSON or exceeds [config].
     */
    fun parse(
        input: InputStream,
        transform: JsonTransform? = null,
        config: JsonParserConfig = JsonParserConfig.DEFAULT,
    ): JsonValue = parse(input.bufferedReader(), transform, config)
}

/**
 * Writes this JSON value to [writer].
 *
 * @param indent number of spaces per level for pretty output; `0` writes compact JSON.
 */
fun JsonValue.writeTo(writer: Writer, indent: Int = 0) {
    JsonWriter.writeTo(this, writer, indent)
}

/**
 * Parses this string as a [JsonObject], returning `null` if parsing fails or the root is not an object.
 */
fun String.asJsonObject(transform: JsonTransform? = null): JsonObject? =
    try {
        Json.parse(this, transform) as? JsonObject
    } catch (e: JsonParseException)
    {
        null
    }

/**
 * Parses this string as a [JsonObject].
 *
 * @throws IllegalArgumentException if parsing fails or the root is not an object.
 */
fun String.asRequiredJsonObject(transform: JsonTransform? = null): JsonObject =
    asJsonObject(transform)
        ?: throw IllegalArgumentException("String could not be parsed as a JsonObject")

/**
 * Parses this string as a [JsonArray], returning `null` if parsing fails or the root is not an array.
 */
fun String.asJsonArray(transform: JsonTransform? = null): JsonArray? =
    try { Json.parse(this, transform) as? JsonArray } catch (_: JsonParseException) { null }

/**
 * Parses this string as a [JsonArray].
 *
 * @throws IllegalArgumentException if parsing fails or the root is not an array.
 */
fun String.asRequiredJsonArray(transform: JsonTransform? = null): JsonArray =
    asJsonArray(transform)
        ?: throw IllegalArgumentException("String could not be parsed as a JsonArray")

/**
 * Maps this object to [T] using [from].
 */
fun <T : Any> JsonObject.fromJson(from: FromJsonObject<T>): T = from.fromJson(this)

/**
 * Maps this object to [T] using [from], returning `null` for [JsonMappingException].
 */
fun <T : Any> JsonObject.fromJsonOrNull(from: FromJsonObject<T>): T? =
    try { from.fromJson(this) } catch (_: JsonMappingException) { null }

/**
 * Parses this string as a JSON object and maps it to [T], returning `null` on parse or mapping failure.
 */
fun <T : Any> String.fromJson(from: FromJsonObject<T>, transform: JsonTransform? = null): T? {
    val obj = asJsonObject(transform) ?: return null
    return try { from.fromJson(obj) } catch (_: JsonMappingException) { null }
}

/**
 * Parses this string as a JSON object and maps it to [T].
 *
 * @throws IllegalArgumentException if the string is not a JSON object.
 * @throws JsonMappingException if object mapping fails.
 */
fun <T : Any> String.fromJsonOrThrow(from: FromJsonObject<T>, transform: JsonTransform? = null): T {
    val obj = asRequiredJsonObject(transform)
    return from.fromJson(obj)
}

/**
 * Converts this map to a [JsonObject] using JFK conversion rules.
 */
fun Map<String, Any?>.toJsonObject(): JsonObject = toJsonValue() as JsonObject

/**
 * Converts this list to a [JsonArray] using JFK conversion rules.
 */
fun List<Any?>.toJsonArray(): JsonArray {
    return toJsonValue() as JsonArray
}

private fun JsonValue.toKotlin(): Any? = when (this) {
    is JsonNull -> null
    is JsonBool -> value
    is JsonNumber -> value
    is JsonString -> value
    is JsonObject -> toMap()
    is JsonArray -> toList()
}
