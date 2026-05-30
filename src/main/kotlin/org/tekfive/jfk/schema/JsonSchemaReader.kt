package org.tekfive.jfk.schema

import org.tekfive.jfk.*

/**
 * Parses a [JsonObject] into a [JsonSchema], handling `$`-prefixed keys,
 * `type` as string or array, and `const: null` vs absent `const`.
 * Unknown keywords are recorded in [JsonSchema.validationErrors].
 */
internal object JsonSchemaReader {

    fun read(json: JsonObject): JsonSchema {
        return read(json, "#")
    }

    private fun read(json: JsonObject, path: String): JsonSchema {
        val validationErrors = mutableListOf<String>()
        validationErrors.addAll(findUnknownKeywords(json, path))

        val schemaVal = readString(json, "\$schema", path, validationErrors)
        val id = readString(json, "\$id", path, validationErrors)
        val ref = readString(json, "\$ref", path, validationErrors)
        val defs = readSchemaMap(json, "\$defs", path, validationErrors)
        val title = readString(json, "title", path, validationErrors)
        val description = readString(json, "description", path, validationErrors)

        // Type: can be a string or an array of strings
        val typeValue = json["type"]
        val singleType: SchemaType?
        val unionTypes: List<SchemaType>?
        when {
            typeValue is JsonString -> {
                singleType = SchemaType.entries.firstOrNull { it.value == typeValue.value }
                if (singleType == null) {
                    validationErrors.add("$path.type: unknown schema type '${typeValue.value}'.")
                }
                unionTypes = null
            }
            typeValue is JsonArray -> {
                singleType = null
                unionTypes = typeValue.items.mapIndexedNotNull { index, value ->
                    when (value) {
                        is JsonString -> {
                            val schemaType = SchemaType.entries.firstOrNull { it.value == value.value }
                            if (schemaType == null) {
                                validationErrors.add("$path.type[$index]: unknown schema type '${value.value}'.")
                            }
                            schemaType
                        }
                        else -> {
                            validationErrors.add("$path.type[$index]: expected string schema type.")
                            null
                        }
                    }
                }.ifEmpty { null }
            }
            typeValue is JsonNull -> {
                singleType = null
                unionTypes = null
            }
            else -> {
                validationErrors.add("$path.type: expected string or array of strings.")
                singleType = null
                unionTypes = null
            }
        }

        val enumValues = readJsonValueArray(json, "enum", path, validationErrors)
        val hasConst = json.containsKey("const")
        val constValue = if (hasConst) json["const"] else null
        val hasDefault = json.containsKey("default")
        val defaultValue = if (hasDefault) json["default"] else null

        // String constraints
        val minLength = readInt(json, "minLength", path, validationErrors)
        val maxLength = readInt(json, "maxLength", path, validationErrors)
        val pattern = readString(json, "pattern", path, validationErrors)
        val format = readString(json, "format", path, validationErrors)

        // Numeric constraints
        val minimum = readNumber(json, "minimum", path, validationErrors)
        val maximum = readNumber(json, "maximum", path, validationErrors)
        val exclusiveMinimum = readNumber(json, "exclusiveMinimum", path, validationErrors)
        val exclusiveMaximum = readNumber(json, "exclusiveMaximum", path, validationErrors)
        val multipleOf = readNumber(json, "multipleOf", path, validationErrors)

        // Object constraints
        val properties = readSchemaMap(json, "properties", path, validationErrors)
        val required = readStringArray(json, "required", path, validationErrors)
        val additionalProperties = readAdditionalProperties(json, path, validationErrors)
        val minProperties = readInt(json, "minProperties", path, validationErrors)
        val maxProperties = readInt(json, "maxProperties", path, validationErrors)
        if (required != null && properties != null) {
            required.filter { !properties.containsKey(it) }
                .forEach { missingProperty ->
                    validationErrors.add("$path.required: '$missingProperty' is required but not defined in properties.")
                }
        }

        // Array constraints
        val itemsKeyword = if (json.containsKey("items")) "items" else "item"
        val items = when (val value = json[itemsKeyword]) {
            is JsonObject -> read(value, "$path.$itemsKeyword")
            is JsonNull -> null
            else -> {
                validationErrors.add("$path.$itemsKeyword: expected object schema.")
                null
            }
        }
        val minItems = readInt(json, "minItems", path, validationErrors)
        val maxItems = readInt(json, "maxItems", path, validationErrors)
        val uniqueItems = readBoolean(json, "uniqueItems", path, validationErrors)

        // Composition
        val allOf = readSchemaArray(json, "allOf", path, validationErrors)
        val anyOf = readSchemaArray(json, "anyOf", path, validationErrors)
        val oneOf = readSchemaArray(json, "oneOf", path, validationErrors)
        val not = when (val value = json["not"]) {
            is JsonObject -> read(value, "$path.not")
            is JsonNull -> null
            else -> {
                validationErrors.add("$path.not: expected object schema.")
                null
            }
        }

        return JsonSchema(
            schema = schemaVal,
            id = id,
            ref = ref,
            defs = defs,
            title = title,
            description = description,
            type = singleType,
            types = unionTypes,
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
            properties = properties,
            required = required,
            additionalProperties = additionalProperties,
            minProperties = minProperties,
            maxProperties = maxProperties,
            items = items,
            minItems = minItems,
            maxItems = maxItems,
            uniqueItems = uniqueItems,
            allOf = allOf,
            anyOf = anyOf,
            oneOf = oneOf,
            not = not,
            hasConst = hasConst,
            hasDefault = hasDefault,
            validationErrors = validationErrors,
        )
    }

    private fun readNumber(json: JsonObject, key: String, path: String, validationErrors: MutableList<String>): Number? {
        val value = json[key]
        return when (value) {
            is JsonNumber -> value.value
            is JsonNull -> null
            else -> {
                validationErrors.add("$path.$key: expected number.")
                null
            }
        }
    }

    private fun readAdditionalProperties(
        json: JsonObject,
        path: String,
        validationErrors: MutableList<String>,
    ): AdditionalProperties? {
        if (!json.containsKey("additionalProperties")) return null
        val value = json["additionalProperties"]
        return when (value) {
            is JsonBool -> AdditionalProperties.BooleanValue(value.value)
            is JsonObject -> AdditionalProperties.SchemaValue(read(value, "$path.additionalProperties"))
            else -> {
                validationErrors.add("$path.additionalProperties: expected boolean or object schema.")
                null
            }
        }
    }

    private fun readSchemaArray(
        json: JsonObject,
        key: String,
        path: String,
        validationErrors: MutableList<String>,
    ): List<JsonSchema>? {
        if (!json.containsKey(key)) return null
        return when (val value = json[key]) {
            is JsonArray -> value.items.mapIndexedNotNull { index, item ->
                when (item) {
                    is JsonObject -> read(item, "$path.$key[$index]")
                    else -> {
                        validationErrors.add("$path.$key[$index]: expected object schema.")
                        null
                    }
                }
            }.ifEmpty { null }
            is JsonNull -> null
            else -> {
                validationErrors.add("$path.$key: expected array.")
                null
            }
        }
    }

    private fun readSchemaMap(
        json: JsonObject,
        key: String,
        path: String,
        validationErrors: MutableList<String>,
    ): Map<String, JsonSchema>? {
        if (!json.containsKey(key)) return null
        return when (val value = json[key]) {
            is JsonObject -> value.entries.mapNotNull { (name, item) ->
                when (item) {
                    is JsonObject -> name to read(item, "$path.$key.$name")
                    else -> {
                        validationErrors.add("$path.$key.$name: expected object schema.")
                        null
                    }
                }
            }.toMap()
            is JsonNull -> null
            else -> {
                validationErrors.add("$path.$key: expected object schema.")
                null
            }
        }
    }

    private fun readString(json: JsonObject, key: String, path: String, validationErrors: MutableList<String>): String? {
        val value = json[key]
        return when (value) {
            is JsonString -> value.value
            is JsonNull -> null
            else -> {
                validationErrors.add("$path.$key: expected string.")
                null
            }
        }
    }

    private fun readInt(json: JsonObject, key: String, path: String, validationErrors: MutableList<String>): Int? {
        val value = json[key]
        return when (value) {
            is JsonNumber -> value.int ?: run {
                validationErrors.add("$path.$key: expected integer.")
                null
            }
            is JsonNull -> null
            else -> {
                validationErrors.add("$path.$key: expected integer.")
                null
            }
        }
    }

    private fun readBoolean(json: JsonObject, key: String, path: String, validationErrors: MutableList<String>): Boolean? {
        val value = json[key]
        return when (value) {
            is JsonBool -> value.value
            is JsonNull -> null
            else -> {
                validationErrors.add("$path.$key: expected boolean.")
                null
            }
        }
    }

    private fun readStringArray(
        json: JsonObject,
        key: String,
        path: String,
        validationErrors: MutableList<String>,
    ): List<String>? {
        if (!json.containsKey(key)) return null
        return when (val value = json[key]) {
            is JsonArray -> value.items.mapIndexedNotNull { index, item ->
                when (item) {
                    is JsonString -> item.value
                    else -> {
                        validationErrors.add("$path.$key[$index]: expected string.")
                        null
                    }
                }
            }.ifEmpty { null }
            is JsonNull -> null
            else -> {
                validationErrors.add("$path.$key: expected array.")
                null
            }
        }
    }

    private fun readJsonValueArray(
        json: JsonObject,
        key: String,
        path: String,
        validationErrors: MutableList<String>,
    ): List<JsonValue>? {
        if (!json.containsKey(key)) return null
        return when (val value = json[key]) {
            is JsonArray -> value.items
            is JsonNull -> null
            else -> {
                validationErrors.add("$path.$key: expected array.")
                null
            }
        }
    }

    private fun findUnknownKeywords(json: JsonObject, path: String): List<String> {
        return json.keys
            .filter { it !in knownKeywords }
            .map { key -> "$path: unknown schema keyword '$key'." }
    }

    private val knownKeywords = setOf(
        "\$schema",
        "\$id",
        "\$ref",
        "\$defs",
        "title",
        "description",
        "type",
        "enum",
        "const",
        "default",
        "minLength",
        "maxLength",
        "pattern",
        "format",
        "minimum",
        "maximum",
        "exclusiveMinimum",
        "exclusiveMaximum",
        "multipleOf",
        "properties",
        "required",
        "additionalProperties",
        "minProperties",
        "maxProperties",
        "items",
        "item",
        "minItems",
        "maxItems",
        "uniqueItems",
        "allOf",
        "anyOf",
        "oneOf",
        "not",
    )
}
