package org.tekfive.jfk

/**
 * Optional interface for transforming values and providing defaults during JSON parsing.
 *
 * The [path] parameter in each method represents the location within the JSON structure
 * as a list of keys/indices leading to the current value. An empty path refers to the root.
 *
 * Example paths:
 * - `[]` — the root value
 * - `["name"]` — the "name" key at the root object
 * - `["address", "city"]` — nested key
 * - `["items", "0"]` — first element of the "items" array
 */
interface JsonTransform {

    /**
     * Transform a parsed value at the given [path]. Called for every value in the JSON tree,
     * including the root, object entries, and array elements.
     *
     * Return the value as-is to leave it unchanged, or return a different [JsonValue] to replace it.
     */
    fun transform(path: List<String>, value: JsonValue): JsonValue = value

    /**
     * Provide a default value for a missing [key] in an object at the given [path].
     * Called once per key returned by [defaultKeys] that is not already present in the parsed object.
     *
     * Return a [JsonValue] to insert as the default, or `null` to skip.
     */
    fun default(path: List<String>, key: String): JsonValue? = null

    /**
     * Return the set of keys that may have defaults for an object at the given [path].
     * Called after an object is fully parsed. Only keys not already present in the object
     * will be passed to [default].
     */
    fun defaultKeys(path: List<String>): Set<String> = emptySet()
}
