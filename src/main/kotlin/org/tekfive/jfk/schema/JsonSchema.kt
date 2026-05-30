package org.tekfive.jfk.schema

import org.tekfive.jfk.FromJsonObject
import org.tekfive.jfk.JsonObject
import org.tekfive.jfk.JsonValue
import org.tekfive.jfk.ToJsonObject
import org.tekfive.jfk.asRequiredJsonObject

/**
 * A general-purpose JSON Schema representation.
 *
 * All keywords are nullable — only non-null fields are emitted during serialization.
 * This matches how JSON Schema works: any keyword can appear on any schema node.
 *
 * Uses Kotlin-friendly field names (e.g., `ref` instead of `$ref`);
 * the writer/reader handles translation to/from `$`-prefixed JSON keys.
 *
 * @property schema `$schema` URI.
 * @property id `$id` URI.
 * @property ref `$ref` reference path or URI.
 * @property defs `$defs` definitions keyed by name.
 * @property title human-readable schema title.
 * @property description human-readable schema description.
 * @property type single JSON Schema type.
 * @property types union of JSON Schema types.
 * @property enum allowed JSON values.
 * @property const required constant JSON value.
 * @property default default JSON value.
 * @property minLength minimum string length.
 * @property maxLength maximum string length.
 * @property pattern regular expression constraint for strings.
 * @property format semantic string format.
 * @property minimum inclusive numeric minimum.
 * @property maximum inclusive numeric maximum.
 * @property exclusiveMinimum exclusive numeric minimum.
 * @property exclusiveMaximum exclusive numeric maximum.
 * @property multipleOf numeric divisibility constraint.
 * @property properties object property schemas.
 * @property required required object property names.
 * @property additionalProperties object additionalProperties constraint.
 * @property minProperties minimum object property count.
 * @property maxProperties maximum object property count.
 * @property items array item schema.
 * @property minItems minimum array item count.
 * @property maxItems maximum array item count.
 * @property uniqueItems whether array items must be unique.
 * @property allOf allOf composition schemas.
 * @property anyOf anyOf composition schemas.
 * @property oneOf oneOf composition schemas.
 * @property not negated schema.
 * @property hasConst true when `const` was explicitly present, including `const: null`.
 * @property hasDefault true when `default` was explicitly present, including `default: null`.
 * @property validationErrors non-fatal errors collected while permissively reading a schema.
 */
data class JsonSchema(
    // Meta
    val schema: String? = null,
    val id: String? = null,
    val ref: String? = null,
    val defs: Map<String, JsonSchema>? = null,
    val title: String? = null,
    val description: String? = null,

    // Type
    val type: SchemaType? = null,
    val types: List<SchemaType>? = null,
    val enum: List<JsonValue>? = null,
    val const: JsonValue? = null,
    val default: JsonValue? = null,

    // String constraints
    val minLength: Int? = null,
    val maxLength: Int? = null,
    val pattern: String? = null,
    val format: String? = null,

    // Numeric constraints
    val minimum: Number? = null,
    val maximum: Number? = null,
    val exclusiveMinimum: Number? = null,
    val exclusiveMaximum: Number? = null,
    val multipleOf: Number? = null,

    // Object constraints
    val properties: Map<String, JsonSchema>? = null,
    val required: List<String>? = null,
    val additionalProperties: AdditionalProperties? = null,
    val minProperties: Int? = null,
    val maxProperties: Int? = null,

    // Array constraints
    val items: JsonSchema? = null,
    val minItems: Int? = null,
    val maxItems: Int? = null,
    val uniqueItems: Boolean? = null,

    // Composition
    val allOf: List<JsonSchema>? = null,
    val anyOf: List<JsonSchema>? = null,
    val oneOf: List<JsonSchema>? = null,
    val not: JsonSchema? = null,

    // Flags for distinguishing `const: null` from absent `const`
    val hasConst: Boolean = false,
    val hasDefault: Boolean = false,

    // Parse-time validation errors collected while permissively reading a schema.
    val validationErrors: List<String> = emptyList(),
) : ToJsonObject {

    /**
     * Serializes this schema to its JSON object representation.
     */
    override fun toJsonObject(): JsonObject = JsonSchemaWriter.write(this)

    /**
     * Resolve a `$defs` reference path like `#/$defs/Name`.
     * Returns null if the path is invalid or the definition doesn't exist.
     */
    fun resolve(refPath: String): JsonSchema? {
        if (!refPath.startsWith("#/\$defs/")) return null
        val name = refPath.removePrefix("#/\$defs/")
        return defs?.get(name)
    }

    companion object : FromJsonObject<JsonSchema> {
        /**
         * Parses a [JsonObject] as a JSON Schema model.
         */
        fun parse(json: JsonObject): JsonSchema = JsonSchemaReader.read(json)

        /**
         * Parses a JSON object string as a JSON Schema model.
         */
        fun parse(input: String): JsonSchema = parse(input.asRequiredJsonObject())

        /**
         * Deserializes [json] as a JSON Schema model.
         */
        override fun fromJson(json: JsonObject, treatEmptyStringAsNull: Boolean, vararg overrides: Pair<kotlin.reflect.KProperty<*>, Any?>): JsonSchema = JsonSchemaReader.read(json)
    }
}

/**
 * Resolves a `$ref` against the given root schema.
 * If this schema has a `ref`, returns the resolved definition; otherwise returns this schema.
 */
fun JsonSchema.resolveRef(root: JsonSchema): JsonSchema {
    val refPath = ref ?: return this
    return root.resolve(refPath) ?: this
}
