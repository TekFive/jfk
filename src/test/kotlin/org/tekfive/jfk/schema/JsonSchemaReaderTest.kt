package org.tekfive.jfk.schema

import org.tekfive.jfk.*
import kotlin.test.*

class JsonSchemaReaderTest {

    @Test
    fun `parses empty object to empty schema`() {
        val schema = JsonSchema.parse(JsonObject())
        assertEquals(JsonSchema(), schema)
    }

    @Test
    fun `parses dollar-prefixed meta keys`() {
        val json = json {
            "\$schema" set "https://json-schema.org/draft/2020-12/schema"
            "\$id" set "https://example.com/test.json"
            "\$ref" set "#/\$defs/Name"
        }
        val schema = JsonSchema.parse(json)
        assertEquals("https://json-schema.org/draft/2020-12/schema", schema.schema)
        assertEquals("https://example.com/test.json", schema.id)
        assertEquals("#/\$defs/Name", schema.ref)
    }

    @Test
    fun `parses defs`() {
        val json = json {
            "\$defs" set json {
                "Name" set json {
                    "type" set "string"
                    "minLength" set 1
                }
            }
        }
        val schema = JsonSchema.parse(json)
        val defs = schema.defs
        assertNotNull(defs)
        assertEquals(SchemaType.STRING, defs["Name"]?.type)
        assertEquals(1, defs["Name"]?.minLength)
    }

    @Test
    fun `parses type as string`() {
        val json = json { "type" set "object" }
        val schema = JsonSchema.parse(json)
        assertEquals(SchemaType.OBJECT, schema.type)
        assertNull(schema.types)
    }

    @Test
    fun `parses type as array`() {
        val json = json {
            "type" set jsonArray("string", "null")
        }
        val schema = JsonSchema.parse(json)
        assertNull(schema.type)
        assertEquals(listOf(SchemaType.STRING, SchemaType.NULL), schema.types)
    }

    @Test
    fun `parses enum values`() {
        val json = json {
            "enum" set jsonArray("red", "green", "blue")
        }
        val schema = JsonSchema.parse(json)
        assertEquals(3, schema.enum?.size)
        assertEquals(JsonString("red"), schema.enum?.get(0))
    }

    @Test
    fun `parses const with value`() {
        val json = json { "const" set "fixed" }
        val schema = JsonSchema.parse(json)
        assertTrue(schema.hasConst)
        assertEquals(JsonString("fixed"), schema.const)
    }

    @Test
    fun `parses const null`() {
        val jsonStr = """{"const": null}"""
        val schema = JsonSchema.parse(jsonStr)
        assertTrue(schema.hasConst)
        assertTrue(schema.const?.isNull ?: false)
    }

    @Test
    fun `absent const sets hasConst false`() {
        val json = json { "type" set "string" }
        val schema = JsonSchema.parse(json)
        assertFalse(schema.hasConst)
        assertNull(schema.const)
    }

    @Test
    fun `parses default with value`() {
        val json = json { "default" set 42 }
        val schema = JsonSchema.parse(json)
        assertTrue(schema.hasDefault)
        assertEquals(42, schema.default?.int)
    }

    @Test
    fun `parses default null`() {
        val jsonStr = """{"default": null}"""
        val schema = JsonSchema.parse(jsonStr)
        assertTrue(schema.hasDefault)
        assertTrue(schema.default?.isNull ?: false)
    }

    @Test
    fun `absent default sets hasDefault false`() {
        val json = json { "type" set "string" }
        val schema = JsonSchema.parse(json)
        assertFalse(schema.hasDefault)
        assertNull(schema.default)
    }

    @Test
    fun `parses string constraints`() {
        val json = json {
            "type" set "string"
            "minLength" set 1
            "maxLength" set 255
            "pattern" set "^[a-z]+$"
            "format" set "email"
        }
        val schema = JsonSchema.parse(json)
        assertEquals(SchemaType.STRING, schema.type)
        assertEquals(1, schema.minLength)
        assertEquals(255, schema.maxLength)
        assertEquals("^[a-z]+$", schema.pattern)
        assertEquals("email", schema.format)
    }

    @Test
    fun `parses numeric constraints`() {
        val json = json {
            "type" set "number"
            "minimum" set 0
            "maximum" set 100
            "exclusiveMinimum" set -1
            "exclusiveMaximum" set 101
            "multipleOf" set 0.5
        }
        val schema = JsonSchema.parse(json)
        assertEquals(SchemaType.NUMBER, schema.type)
        assertEquals(0, schema.minimum?.toInt())
        assertEquals(100, schema.maximum?.toInt())
        assertEquals(-1, schema.exclusiveMinimum?.toInt())
        assertEquals(101, schema.exclusiveMaximum?.toInt())
        assertEquals(0.5, schema.multipleOf?.toDouble())
    }

    @Test
    fun `records fractional integer constraint as validation error`() {
        val json = json {
            "minLength" set 1.5
        }
        val schema = JsonSchema.parse(json)

        assertNull(schema.minLength)
        assertTrue(schema.validationErrors.any { it.contains("#.minLength: expected integer.") })
    }

    @Test
    fun `parses object constraints`() {
        val json = json {
            "type" set "object"
            "properties" set json {
                "name" set json { "type" set "string" }
                "age" set json { "type" set "integer" }
            }
            "required" set jsonArray("name")
            "additionalProperties" set false
            "minProperties" set 1
            "maxProperties" set 10
        }
        val schema = JsonSchema.parse(json)
        assertEquals(SchemaType.OBJECT, schema.type)
        assertEquals(2, schema.properties?.size)
        assertEquals(SchemaType.STRING, schema.properties?.get("name")?.type)
        assertEquals(SchemaType.INTEGER, schema.properties?.get("age")?.type)
        assertEquals(listOf("name"), schema.required)
        assertEquals(AdditionalProperties.BooleanValue(false), schema.additionalProperties)
        assertEquals(1, schema.minProperties)
        assertEquals(10, schema.maxProperties)
    }

    @Test
    fun `parses additionalProperties as schema`() {
        val json = json {
            "additionalProperties" set json { "type" set "string" }
        }
        val schema = JsonSchema.parse(json)
        val ap = schema.additionalProperties as AdditionalProperties.SchemaValue
        assertEquals(SchemaType.STRING, ap.schema.type)
    }

    @Test
    fun `parses array constraints`() {
        val json = json {
            "type" set "array"
            "items" set json { "type" set "string" }
            "minItems" set 1
            "maxItems" set 50
            "uniqueItems" set true
        }
        val schema = JsonSchema.parse(json)
        assertEquals(SchemaType.ARRAY, schema.type)
        assertEquals(SchemaType.STRING, schema.items?.type)
        assertEquals(1, schema.minItems)
        assertEquals(50, schema.maxItems)
        assertEquals(true, schema.uniqueItems)
    }

    @Test
    fun `parses item as array items compatibility alias`() {
        val json = json {
            "type" set "array"
            "item" set json { "type" set "string" }
        }
        val schema = JsonSchema.parse(json)
        assertEquals(SchemaType.ARRAY, schema.type)
        assertEquals(SchemaType.STRING, schema.items?.type)
        assertTrue(schema.validationErrors.isEmpty())
    }

    @Test
    fun `parses composition keywords`() {
        val json = json {
            "allOf" set jsonArray {
                addObject { "type" set "object" }
                addObject { "type" set "string" }
            }
            "anyOf" set jsonArray {
                addObject { "type" set "string" }
                addObject { "type" set "integer" }
            }
            "oneOf" set jsonArray {
                addObject { "minimum" set 0 }
                addObject { "maximum" set 100 }
            }
            "not" set json { "type" set "null" }
        }
        val schema = JsonSchema.parse(json)
        assertEquals(2, schema.allOf?.size)
        assertEquals(2, schema.anyOf?.size)
        assertEquals(2, schema.oneOf?.size)
        assertEquals(SchemaType.NULL, schema.not?.type)
    }

    @Test
    fun `parses from string`() {
        val jsonStr = """{"type": "string", "minLength": 1}"""
        val schema = JsonSchema.parse(jsonStr)
        assertEquals(SchemaType.STRING, schema.type)
        assertEquals(1, schema.minLength)
    }

    @Test
    fun `records unknown keywords as validation errors`() {
        val json = json {
            "type" set "string"
            "x-custom" set "value"
            "customKeyword" set 42
        }
        val schema = JsonSchema.parse(json)
        assertEquals(SchemaType.STRING, schema.type)
        assertEquals(2, schema.validationErrors.size)
        assertTrue(schema.validationErrors.any { it.contains("unknown schema keyword 'x-custom'") })
        assertTrue(schema.validationErrors.any { it.contains("unknown schema keyword 'customKeyword'") })
    }

    @Test
    fun `parses title and description`() {
        val json = json {
            "title" set "My Schema"
            "description" set "Describes something"
        }
        val schema = JsonSchema.parse(json)
        assertEquals("My Schema", schema.title)
        assertEquals("Describes something", schema.description)
    }

    @Test
    fun `records invalid type value as validation error`() {
        val json = json { "type" set "unknown" }
        val schema = JsonSchema.parse(json)
        assertNull(schema.type)
        assertTrue(schema.validationErrors.any { it.contains("unknown schema type 'unknown'") })
    }

    @Test
    fun `records required properties missing from properties map`() {
        val json = json {
            "type" set "object"
            "properties" set json {
                "name" set json { "type" set "string" }
            }
            "required" set jsonArray("name", "age")
        }
        val schema = JsonSchema.parse(json)
        assertEquals(listOf("name", "age"), schema.required)
        assertTrue(schema.validationErrors.any { it.contains("'age' is required but not defined in properties") })
    }

    @Test
    fun `records invalid nested schema structures without throwing`() {
        val json = json {
            "properties" set json {
                "name" set "not-a-schema"
            }
            "allOf" set jsonArray(
                json { "type" set "string" },
                "bad-entry"
            )
        }
        val schema = JsonSchema.parse(json)
        assertNotNull(schema.allOf)
        assertEquals(1, schema.allOf.size)
        assertTrue(schema.validationErrors.any { it.contains("#.properties.name: expected object schema.") })
        assertTrue(schema.validationErrors.any { it.contains("#.allOf[1]: expected object schema.") })
    }

    @Test
    fun `records wrong schema keyword container types`() {
        val json = json {
            "enum" set "red"
            "properties" set "not-an-object"
            "required" set "name"
            "allOf" set "not-an-array"
        }
        val schema = JsonSchema.parse(json)

        assertTrue(schema.validationErrors.any { it.contains("#.enum: expected array.") })
        assertTrue(schema.validationErrors.any { it.contains("#.properties: expected object schema.") })
        assertTrue(schema.validationErrors.any { it.contains("#.required: expected array.") })
        assertTrue(schema.validationErrors.any { it.contains("#.allOf: expected array.") })
    }

    @Test
    fun `parses nested schema structure`() {
        val jsonStr = """{
            "${'$'}schema": "https://json-schema.org/draft/2020-12/schema",
            "type": "object",
            "properties": {
                "name": { "type": "string" },
                "tags": {
                    "type": "array",
                    "items": { "type": "string" }
                }
            },
            "required": ["name"]
        }"""
        val schema = JsonSchema.parse(jsonStr)
        assertEquals(SchemaType.OBJECT, schema.type)
        assertEquals(SchemaType.STRING, schema.properties?.get("name")?.type)
        assertEquals(SchemaType.ARRAY, schema.properties?.get("tags")?.type)
        assertEquals(SchemaType.STRING, schema.properties?.get("tags")?.items?.type)
        assertEquals(listOf("name"), schema.required)
    }
}
