package org.tekfive.jfk.schema

/**
 * JSON Schema type values as defined by the specification.
 *
 * @property value JSON Schema string representation.
 */
enum class SchemaType(val value: String) {
    /** The `string` type. */
    STRING("string"),
    /** The `number` type. */
    NUMBER("number"),
    /** The `integer` type. */
    INTEGER("integer"),
    /** The `boolean` type. */
    BOOLEAN("boolean"),
    /** The `object` type. */
    OBJECT("object"),
    /** The `array` type. */
    ARRAY("array"),
    /** The `null` type. */
    NULL("null");

    companion object {
        /**
         * Resolves a [SchemaType] from its JSON Schema string [value].
         *
         * @throws IllegalArgumentException if [value] is not a known schema type.
         */
        fun fromValue(value: String): SchemaType =
            entries.firstOrNull { it.value == value }
                ?: throw IllegalArgumentException("Unknown schema type: $value")
    }
}
