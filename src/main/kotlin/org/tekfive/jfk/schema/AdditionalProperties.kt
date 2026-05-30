package org.tekfive.jfk.schema

/**
 * Represents the `additionalProperties` keyword in JSON Schema,
 * which can be either a boolean or a nested schema.
 */
sealed class AdditionalProperties {
    /**
     * Boolean form of `additionalProperties`.
     *
     * @property value boolean additionalProperties value.
     */
    data class BooleanValue(val value: Boolean) : AdditionalProperties()

    /**
     * Schema form of `additionalProperties`.
     *
     * @property schema schema for additional properties.
     */
    data class SchemaValue(val schema: JsonSchema) : AdditionalProperties()
}
