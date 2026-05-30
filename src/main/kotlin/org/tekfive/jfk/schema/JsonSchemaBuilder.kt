package org.tekfive.jfk.schema

import org.tekfive.jfk.JsonValue

/**
 * DSL entry point for building a [JsonSchema].
 *
 * ```
 * val schema = jsonSchema {
 *     schema = "https://json-schema.org/draft/2020-12/schema"
 *     type = SchemaType.OBJECT
 *     properties {
 *         "name" to stringSchema { minLength = 1 }
 *         "age" to integerSchema { minimum = 0 }
 *     }
 *     required("name", "age")
 * }
 * ```
 */
fun jsonSchema(block: JsonSchemaBuilder.() -> Unit): JsonSchema {
    val builder = JsonSchemaBuilder()
    builder.block()
    return builder.build()
}

/** Shorthand for a string-typed schema. */
fun stringSchema(block: JsonSchemaBuilder.() -> Unit = {}): JsonSchema {
    val builder = JsonSchemaBuilder()
    builder.type = SchemaType.STRING
    builder.block()
    return builder.build()
}

/** Shorthand for an integer-typed schema. */
fun integerSchema(block: JsonSchemaBuilder.() -> Unit = {}): JsonSchema {
    val builder = JsonSchemaBuilder()
    builder.type = SchemaType.INTEGER
    builder.block()
    return builder.build()
}

/** Shorthand for a number-typed schema. */
fun numberSchema(block: JsonSchemaBuilder.() -> Unit = {}): JsonSchema {
    val builder = JsonSchemaBuilder()
    builder.type = SchemaType.NUMBER
    builder.block()
    return builder.build()
}

/** Shorthand for a boolean-typed schema. */
fun booleanSchema(block: JsonSchemaBuilder.() -> Unit = {}): JsonSchema {
    val builder = JsonSchemaBuilder()
    builder.type = SchemaType.BOOLEAN
    builder.block()
    return builder.build()
}

/** Shorthand for an array-typed schema. */
fun arraySchema(block: JsonSchemaBuilder.() -> Unit = {}): JsonSchema {
    val builder = JsonSchemaBuilder()
    builder.type = SchemaType.ARRAY
    builder.block()
    return builder.build()
}

/** Shorthand for an object-typed schema. */
fun objectSchema(block: JsonSchemaBuilder.() -> Unit = {}): JsonSchema {
    val builder = JsonSchemaBuilder()
    builder.type = SchemaType.OBJECT
    builder.block()
    return builder.build()
}

/** Shorthand for a null-typed schema. */
fun nullSchema(): JsonSchema = JsonSchema(type = SchemaType.NULL)

/** Shorthand for a `$ref` schema. */
fun refSchema(ref: String): JsonSchema = JsonSchema(ref = ref)

/**
 * Mutable builder backing the JSON Schema DSL.
 */
class JsonSchemaBuilder {
    /** `$schema` URI. */
    var schema: String? = null
    /** `$id` URI. */
    var id: String? = null
    /** `$ref` reference path or URI. */
    var ref: String? = null
    /** Human-readable title. */
    var title: String? = null
    /** Human-readable description. */
    var description: String? = null
    /** Single JSON Schema type. */
    var type: SchemaType? = null
    /** Semantic string format. */
    var format: String? = null

    /** Minimum string length. */
    var minLength: Int? = null
    /** Maximum string length. */
    var maxLength: Int? = null
    /** Regular expression constraint for strings. */
    var pattern: String? = null

    /** Inclusive numeric minimum. */
    var minimum: Number? = null
    /** Inclusive numeric maximum. */
    var maximum: Number? = null
    /** Exclusive numeric minimum. */
    var exclusiveMinimum: Number? = null
    /** Exclusive numeric maximum. */
    var exclusiveMaximum: Number? = null
    /** Numeric divisibility constraint. */
    var multipleOf: Number? = null

    /** Minimum array item count. */
    var minItems: Int? = null
    /** Maximum array item count. */
    var maxItems: Int? = null
    /** Whether array items must be unique. */
    var uniqueItems: Boolean? = null

    /** Minimum object property count. */
    var minProperties: Int? = null
    /** Maximum object property count. */
    var maxProperties: Int? = null

    private var types: List<SchemaType>? = null
    private var enumValues: List<JsonValue>? = null
    private var constValue: JsonValue? = null
    private var hasConst: Boolean = false
    private var defaultValue: JsonValue? = null
    private var hasDefault: Boolean = false
    private var propertiesMap: Map<String, JsonSchema>? = null
    private var requiredList: List<String>? = null
    private var additionalProps: AdditionalProperties? = null
    private var itemsSchema: JsonSchema? = null
    private var allOfList: List<JsonSchema>? = null
    private var anyOfList: List<JsonSchema>? = null
    private var oneOfList: List<JsonSchema>? = null
    private var notSchema: JsonSchema? = null
    private var defsMap: Map<String, JsonSchema>? = null

    /** Set union types like `["string", "null"]`. */
    fun types(vararg schemaTypes: SchemaType) {
        types = schemaTypes.toList()
    }

    /** Set enum values. */
    fun enum(vararg values: JsonValue) {
        enumValues = values.toList()
    }

    /** Set a const value (including null). */
    fun const(value: JsonValue) {
        constValue = value
        hasConst = true
    }

    /** Set a default value (including null). */
    fun default(value: JsonValue) {
        defaultValue = value
        hasDefault = true
    }

    /** Define properties using the [PropertiesBuilder] DSL. */
    fun properties(block: PropertiesBuilder.() -> Unit) {
        val builder = PropertiesBuilder()
        builder.block()
        propertiesMap = builder.build()
    }

    /** Set required property names. */
    fun required(vararg names: String) {
        requiredList = names.toList()
    }

    /** Set additionalProperties to a boolean. */
    fun additionalProperties(value: Boolean) {
        additionalProps = AdditionalProperties.BooleanValue(value)
    }

    /** Set additionalProperties to a schema. */
    fun additionalProperties(schema: JsonSchema) {
        additionalProps = AdditionalProperties.SchemaValue(schema)
    }

    /** Set the items schema for arrays. */
    fun items(schema: JsonSchema) {
        itemsSchema = schema
    }

    /** Set the items schema for arrays using a builder. */
    fun items(block: JsonSchemaBuilder.() -> Unit) {
        itemsSchema = jsonSchema(block)
    }

    /** Define allOf composition. */
    fun allOf(block: CompositionBuilder.() -> Unit) {
        val builder = CompositionBuilder()
        builder.block()
        allOfList = builder.build()
    }

    /** Define anyOf composition. */
    fun anyOf(block: CompositionBuilder.() -> Unit) {
        val builder = CompositionBuilder()
        builder.block()
        anyOfList = builder.build()
    }

    /** Define oneOf composition. */
    fun oneOf(block: CompositionBuilder.() -> Unit) {
        val builder = CompositionBuilder()
        builder.block()
        oneOfList = builder.build()
    }

    /** Set the not schema. */
    fun not(schema: JsonSchema) {
        notSchema = schema
    }

    /** Set the not schema using a builder. */
    fun not(block: JsonSchemaBuilder.() -> Unit) {
        notSchema = jsonSchema(block)
    }

    /** Define `$defs` using the [DefsBuilder] DSL. */
    fun defs(block: DefsBuilder.() -> Unit) {
        val builder = DefsBuilder()
        builder.block()
        defsMap = builder.build()
    }

    internal fun build(): JsonSchema = JsonSchema(
        schema = schema,
        id = id,
        ref = ref,
        defs = defsMap,
        title = title,
        description = description,
        type = type,
        types = types,
        enum = enumValues,
        const = constValue,
        default = defaultValue,
        minLength = minLength,
        maxLength = maxLength,
        pattern = pattern,
        format = format,
        minimum = minimum,
        maximum = maximum,
        exclusiveMinimum = exclusiveMinimum,
        exclusiveMaximum = exclusiveMaximum,
        multipleOf = multipleOf,
        properties = propertiesMap,
        required = requiredList,
        additionalProperties = additionalProps,
        minProperties = minProperties,
        maxProperties = maxProperties,
        items = itemsSchema,
        minItems = minItems,
        maxItems = maxItems,
        uniqueItems = uniqueItems,
        allOf = allOfList,
        anyOf = anyOfList,
        oneOf = oneOfList,
        not = notSchema,
        hasConst = hasConst,
        hasDefault = hasDefault,
        validationErrors = emptyList(),
    )
}

/**
 * Builder for object property schemas.
 */
class PropertiesBuilder {
    private val map = LinkedHashMap<String, JsonSchema>()

    /**
     * Adds a property schema for this property name.
     */
    infix fun String.to(schema: JsonSchema) {
        map[this] = schema
    }

    internal fun build(): Map<String, JsonSchema> = map.toMap()
}

/**
 * Builder for `$defs` schema definitions.
 */
class DefsBuilder {
    private val map = LinkedHashMap<String, JsonSchema>()

    /**
     * Adds a definition schema for this definition name.
     */
    infix fun String.to(schema: JsonSchema) {
        map[this] = schema
    }

    internal fun build(): Map<String, JsonSchema> = map.toMap()
}

/**
 * Builder for composition keyword schema lists.
 */
class CompositionBuilder {
    private val schemas = mutableListOf<JsonSchema>()

    /**
     * Adds a schema built with [JsonSchemaBuilder].
     */
    fun schema(block: JsonSchemaBuilder.() -> Unit) {
        schemas += jsonSchema(block)
    }

    /**
     * Adds an existing [JsonSchema].
     */
    fun schema(schema: JsonSchema) {
        schemas += schema
    }

    internal fun build(): List<JsonSchema> = schemas.toList()
}
