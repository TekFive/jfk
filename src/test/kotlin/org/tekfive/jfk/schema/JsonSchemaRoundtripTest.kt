package org.tekfive.jfk.schema

import org.tekfive.jfk.*
import kotlin.test.*

class JsonSchemaRoundtripTest {

    private fun roundtrip(schema: JsonSchema): JsonSchema {
        val json = schema.toJsonObject()
        return JsonSchema.parse(json)
    }

    @Test
    fun `roundtrip empty schema`() {
        val original = JsonSchema()
        assertEquals(original, roundtrip(original))
    }

    @Test
    fun `roundtrip string schema`() {
        val original = JsonSchema(
            type = SchemaType.STRING,
            minLength = 1,
            maxLength = 255,
            pattern = "^[a-z]+$",
            format = "email",
        )
        assertEquals(original, roundtrip(original))
    }

    @Test
    fun `roundtrip integer schema`() {
        val original = JsonSchema(
            type = SchemaType.INTEGER,
            minimum = 0,
            maximum = 100,
            multipleOf = 5,
        )
        assertEquals(original, roundtrip(original))
    }

    @Test
    fun `roundtrip number schema with exclusive bounds`() {
        val original = JsonSchema(
            type = SchemaType.NUMBER,
            exclusiveMinimum = 0,
            exclusiveMaximum = 1,
        )
        assertEquals(original, roundtrip(original))
    }

    @Test
    fun `roundtrip boolean schema`() {
        val original = JsonSchema(type = SchemaType.BOOLEAN)
        assertEquals(original, roundtrip(original))
    }

    @Test
    fun `roundtrip null schema`() {
        val original = JsonSchema(type = SchemaType.NULL)
        assertEquals(original, roundtrip(original))
    }

    @Test
    fun `roundtrip union types`() {
        val original = JsonSchema(types = listOf(SchemaType.STRING, SchemaType.NULL))
        assertEquals(original, roundtrip(original))
    }

    @Test
    fun `roundtrip enum values`() {
        val original = JsonSchema(
            type = SchemaType.STRING,
            enum = listOf(JsonString("a"), JsonString("b"), JsonString("c")),
        )
        assertEquals(original, roundtrip(original))
    }

    @Test
    fun `roundtrip const string`() {
        val original = JsonSchema(const = JsonString("fixed"), hasConst = true)
        assertEquals(original, roundtrip(original))
    }

    @Test
    fun `roundtrip const null`() {
        val original = JsonSchema(const = JsonNull, hasConst = true)
        val result = roundtrip(original)
        assertTrue(result.hasConst)
        assertTrue(result.const?.isNull ?: false)
    }

    @Test
    fun `roundtrip default value`() {
        val original = JsonSchema(default = JsonNumber(42), hasDefault = true)
        assertEquals(original, roundtrip(original))
    }

    @Test
    fun `roundtrip object schema`() {
        val original = JsonSchema(
            type = SchemaType.OBJECT,
            properties = mapOf(
                "name" to JsonSchema(type = SchemaType.STRING, minLength = 1),
                "age" to JsonSchema(type = SchemaType.INTEGER, minimum = 0),
            ),
            required = listOf("name"),
            additionalProperties = AdditionalProperties.BooleanValue(false),
            minProperties = 1,
            maxProperties = 10,
        )
        assertEquals(original, roundtrip(original))
    }

    @Test
    fun `roundtrip additionalProperties as schema`() {
        val original = JsonSchema(
            type = SchemaType.OBJECT,
            additionalProperties = AdditionalProperties.SchemaValue(
                JsonSchema(type = SchemaType.STRING)
            ),
        )
        assertEquals(original, roundtrip(original))
    }

    @Test
    fun `roundtrip array schema`() {
        val original = JsonSchema(
            type = SchemaType.ARRAY,
            items = JsonSchema(type = SchemaType.STRING),
            minItems = 1,
            maxItems = 50,
            uniqueItems = true,
        )
        assertEquals(original, roundtrip(original))
    }

    @Test
    fun `roundtrip composition`() {
        val original = JsonSchema(
            allOf = listOf(
                JsonSchema(type = SchemaType.OBJECT),
                JsonSchema(properties = mapOf("a" to JsonSchema(type = SchemaType.STRING))),
            ),
            anyOf = listOf(
                JsonSchema(type = SchemaType.STRING),
                JsonSchema(type = SchemaType.INTEGER),
            ),
            oneOf = listOf(
                JsonSchema(minimum = 0),
                JsonSchema(maximum = 100),
            ),
            not = JsonSchema(type = SchemaType.NULL),
        )
        assertEquals(original, roundtrip(original))
    }

    @Test
    fun `roundtrip ref schema`() {
        val original = JsonSchema(ref = "#/\$defs/Name")
        assertEquals(original, roundtrip(original))
    }

    @Test
    fun `roundtrip schema with defs`() {
        val original = JsonSchema(
            defs = mapOf(
                "Name" to JsonSchema(type = SchemaType.STRING, minLength = 1),
                "Age" to JsonSchema(type = SchemaType.INTEGER, minimum = 0),
            ),
            type = SchemaType.OBJECT,
            properties = mapOf(
                "name" to JsonSchema(ref = "#/\$defs/Name"),
                "age" to JsonSchema(ref = "#/\$defs/Age"),
            ),
        )
        assertEquals(original, roundtrip(original))
    }

    @Test
    fun `roundtrip meta fields`() {
        val original = JsonSchema(
            schema = "https://json-schema.org/draft/2020-12/schema",
            id = "https://example.com/test.json",
            title = "Test Schema",
            description = "A schema for testing roundtrips",
        )
        assertEquals(original, roundtrip(original))
    }

    @Test
    fun `roundtrip through JSON string`() {
        val original = jsonSchema {
            schema = "https://json-schema.org/draft/2020-12/schema"
            type = SchemaType.OBJECT
            properties {
                "name" to stringSchema { minLength = 1 }
                "age" to integerSchema { minimum = 0 }
                "tags" to arraySchema {
                    items(stringSchema())
                    uniqueItems = true
                }
            }
            required("name")
        }
        val jsonStr = original.toJsonString()
        val parsed = JsonSchema.parse(jsonStr)
        assertEquals(original, parsed)
    }

    @Test
    fun `roundtrip complex nested schema`() {
        val original = jsonSchema {
            schema = "https://json-schema.org/draft/2020-12/schema"
            title = "Workflow"
            type = SchemaType.OBJECT
            defs {
                "Step" to objectSchema {
                    properties {
                        "name" to stringSchema()
                        "type" to jsonSchema {
                            type = SchemaType.STRING
                            enum(JsonString("ai"), JsonString("manual"), JsonString("auto"))
                        }
                        "config" to objectSchema {
                            additionalProperties(stringSchema())
                        }
                    }
                    required("name", "type")
                }
            }
            properties {
                "name" to stringSchema { minLength = 1; maxLength = 200 }
                "steps" to arraySchema {
                    items(refSchema("#/\$defs/Step"))
                    minItems = 1
                }
                "enabled" to jsonSchema {
                    type = SchemaType.BOOLEAN
                    default(JsonBool(true))
                }
            }
            required("name", "steps")
            additionalProperties(false)
        }
        val jsonStr = original.toJsonString(indent = 2)
        val parsed = JsonSchema.parse(jsonStr)
        assertEquals(original, parsed)
    }
}
