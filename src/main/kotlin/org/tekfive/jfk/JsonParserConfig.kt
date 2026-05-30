package org.tekfive.jfk

/**
 * Configuration for [JsonParser] security limits.
 *
 * All limits have sensible defaults. Set a value to 0 to disable that limit.
 */
data class JsonParserConfig(
    /** Maximum nesting depth for objects and arrays. Default: 512. */
    val maxDepth: Int = 512,
    /** Maximum length of a JSON string value in characters. Default: 10MB. */
    val maxStringLength: Int = 10_000_000,
    /** Maximum length of a JSON number literal in characters. Default: 1000. */
    val maxNumberLength: Int = 1_000,
    /** Maximum number of entries in a single JSON object. Default: 100,000. */
    val maxObjectEntries: Int = 100_000,
    /** Maximum number of elements in a single JSON array. Default: 1,000,000. */
    val maxArrayElements: Int = 1_000_000,
    /** Whether to reject objects with duplicate keys. Default: true. */
    val rejectDuplicateKeys: Boolean = true,
) {
    companion object {
        /** Default parser configuration. */
        val DEFAULT = JsonParserConfig()
    }
}
