package org.tekfive.jfk.schema

import org.tekfive.jfk.*

/**
 * Serializes a [JsonSchema] to a [JsonObject], emitting only non-null fields
 * and translating Kotlin field names to `$`-prefixed JSON Schema keys.
 */
internal object JsonSchemaWriter {

    fun write(schema: JsonSchema): JsonObject {
        val map = LinkedHashMap<String, JsonValue>()

        schema.schema?.let { map["\$schema"] = JsonString(it) }
        schema.id?.let { map["\$id"] = JsonString(it) }
        schema.ref?.let { map["\$ref"] = JsonString(it) }
        schema.defs?.let { defs ->
            val defsObj = JsonObject(defs.mapValues { (_, v) -> write(v) })
            map["\$defs"] = defsObj
        }
        schema.title?.let { map["title"] = JsonString(it) }
        schema.description?.let { map["description"] = JsonString(it) }

        // Type: single or array
        if (schema.types != null) {
            map["type"] = JsonArray(schema.types.map { JsonString(it.value) })
        } else {
            schema.type?.let { map["type"] = JsonString(it.value) }
        }

        schema.enum?.let { values ->
            map["enum"] = JsonArray(values)
        }
        if (schema.hasConst) {
            map["const"] = schema.const ?: JsonNull
        }
        if (schema.hasDefault) {
            map["default"] = schema.default ?: JsonNull
        }

        // String constraints
        schema.minLength?.let { map["minLength"] = JsonNumber(it) }
        schema.maxLength?.let { map["maxLength"] = JsonNumber(it) }
        schema.pattern?.let { map["pattern"] = JsonString(it) }
        schema.format?.let { map["format"] = JsonString(it) }

        // Numeric constraints
        schema.minimum?.let { map["minimum"] = JsonNumber(it) }
        schema.maximum?.let { map["maximum"] = JsonNumber(it) }
        schema.exclusiveMinimum?.let { map["exclusiveMinimum"] = JsonNumber(it) }
        schema.exclusiveMaximum?.let { map["exclusiveMaximum"] = JsonNumber(it) }
        schema.multipleOf?.let { map["multipleOf"] = JsonNumber(it) }

        // Object constraints
        schema.properties?.let { props ->
            val propsObj = JsonObject(props.mapValues { (_, v) -> write(v) })
            map["properties"] = propsObj
        }
        schema.required?.let { map["required"] = JsonArray(it.map { r -> JsonString(r) }) }
        schema.additionalProperties?.let { ap ->
            map["additionalProperties"] = when (ap) {
                is AdditionalProperties.BooleanValue -> JsonBool(ap.value)
                is AdditionalProperties.SchemaValue -> write(ap.schema)
            }
        }
        schema.minProperties?.let { map["minProperties"] = JsonNumber(it) }
        schema.maxProperties?.let { map["maxProperties"] = JsonNumber(it) }

        // Array constraints
        schema.items?.let { map["items"] = write(it) }
        schema.minItems?.let { map["minItems"] = JsonNumber(it) }
        schema.maxItems?.let { map["maxItems"] = JsonNumber(it) }
        schema.uniqueItems?.let { map["uniqueItems"] = JsonBool(it) }

        // Composition
        schema.allOf?.let { map["allOf"] = JsonArray(it.map { s -> write(s) }) }
        schema.anyOf?.let { map["anyOf"] = JsonArray(it.map { s -> write(s) }) }
        schema.oneOf?.let { map["oneOf"] = JsonArray(it.map { s -> write(s) }) }
        schema.not?.let { map["not"] = write(it) }

        return JsonObject(map)
    }
}
