package org.tekfive.jfk.schema

import org.tekfive.jfk.*
import kotlin.test.*

/**
 * Tests that JsonSchema implements FromJson and can be deserialized using the
 * standard FromJson extension methods (String.fromJson, JsonObject.fromJson, etc.).
 */
class JsonSchemaFromJsonTest {

    @Test
    fun `fromJson parses empty schema`() {
        val json = "{}"
        val schema = json.fromJsonOrThrow(JsonSchema)
        assertEquals(JsonSchema(), schema)
    }

    @Test
    fun `fromJson parses string schema`() {
        val json = """{"type": "string", "minLength": 1, "maxLength": 100}"""
        val schema = json.fromJsonOrThrow(JsonSchema)
        assertEquals(SchemaType.STRING, schema.type)
        assertEquals(1, schema.minLength)
        assertEquals(100, schema.maxLength)
    }

    @Test
    fun `fromJson parses object schema with properties`() {
        val json = """{
            "type": "object",
            "properties": {
                "name": {"type": "string"},
                "age": {"type": "integer", "minimum": 0}
            },
            "required": ["name"]
        }"""
        val schema = json.fromJsonOrThrow(JsonSchema)
        assertEquals(SchemaType.OBJECT, schema.type)
        assertEquals(2, schema.properties?.size)
        assertEquals(SchemaType.STRING, schema.properties?.get("name")?.type)
        assertEquals(SchemaType.INTEGER, schema.properties?.get("age")?.type)
        assertEquals(0, schema.properties?.get("age")?.minimum)
        assertEquals(listOf("name"), schema.required)
    }

    @Test
    fun `fromJson handles dollar-prefixed keys`() {
        val json = """{
            "${'$'}schema": "https://json-schema.org/draft/2020-12/schema",
            "${'$'}id": "https://example.com/test",
            "${'$'}ref": "#/${'$'}defs/Name",
            "${'$'}defs": {
                "Name": {"type": "string"}
            }
        }"""
        val schema = json.fromJsonOrThrow(JsonSchema)
        assertEquals("https://json-schema.org/draft/2020-12/schema", schema.schema)
        assertEquals("https://example.com/test", schema.id)
        assertEquals("#/\$defs/Name", schema.ref)
        val defs = assertNotNull(schema.defs)
        assertEquals(SchemaType.STRING, defs["Name"]?.type)
    }

    @Test
    fun `fromJson handles union types`() {
        val json = """{"type": ["string", "null"]}"""
        val schema = json.fromJsonOrThrow(JsonSchema)
        assertNull(schema.type)
        assertEquals(listOf(SchemaType.STRING, SchemaType.NULL), schema.types)
    }

    @Test
    fun `fromJson handles const null vs absent const`() {
        val withConst = """{"const": null}""".fromJsonOrThrow(JsonSchema)
        assertTrue(withConst.hasConst)

        val withoutConst = """{}""".fromJsonOrThrow(JsonSchema)
        assertFalse(withoutConst.hasConst)
    }

    @Test
    fun `fromJson handles default value`() {
        val json = """{"type": "boolean", "default": true}"""
        val schema = json.fromJsonOrThrow(JsonSchema)
        assertTrue(schema.hasDefault)
        assertEquals(JsonBool(true), schema.default)
    }

    @Test
    fun `fromJson handles additionalProperties as boolean`() {
        val json = """{"type": "object", "additionalProperties": false}"""
        val schema = json.fromJsonOrThrow(JsonSchema)
        assertEquals(AdditionalProperties.BooleanValue(false), schema.additionalProperties)
    }

    @Test
    fun `fromJson handles additionalProperties as schema`() {
        val json = """{"type": "object", "additionalProperties": {"type": "string"}}"""
        val schema = json.fromJsonOrThrow(JsonSchema)
        val ap = schema.additionalProperties as AdditionalProperties.SchemaValue
        assertEquals(SchemaType.STRING, ap.schema.type)
    }

    @Test
    fun `fromJson handles array schema`() {
        val json = """{"type": "array", "items": {"type": "integer"}, "minItems": 1, "uniqueItems": true}"""
        val schema = json.fromJsonOrThrow(JsonSchema)
        assertEquals(SchemaType.ARRAY, schema.type)
        assertEquals(SchemaType.INTEGER, schema.items?.type)
        assertEquals(1, schema.minItems)
        assertEquals(true, schema.uniqueItems)
    }

    @Test
    fun `fromJson handles composition keywords`() {
        val json = """{
            "allOf": [{"type": "object"}, {"required": ["name"]}],
            "not": {"type": "null"}
        }"""
        val schema = json.fromJsonOrThrow(JsonSchema)
        assertEquals(2, schema.allOf?.size)
        assertEquals(SchemaType.NULL, schema.not?.type)
    }

    @Test
    fun `fromJson handles enum values`() {
        val json = """{"type": "string", "enum": ["a", "b", "c"]}"""
        val schema = json.fromJsonOrThrow(JsonSchema)
        assertEquals(3, schema.enum?.size)
        assertEquals(JsonString("a"), schema.enum!![0])
    }

    @Test
    fun `fromJsonOptional returns null for null input`() {
        val result = JsonSchema.fromJsonOptional(null)
        assertNull(result)
    }

    @Test
    fun `fromJson via JsonObject extension`() {
        val jsonObj = JsonObject(linkedMapOf<String, Any?>(
            "type" to "string",
            "minLength" to 5,
        ))
        val schema = jsonObj.fromJson(JsonSchema)
        assertEquals(SchemaType.STRING, schema.type)
        assertEquals(5, schema.minLength)
    }

    @Test
    fun `fromJson nullable extension returns null for invalid json string`() {
        val result = "not valid json".fromJson(JsonSchema)
        assertNull(result)
    }

    @Test
    fun `fromJson roundtrips through serialization`() {
        val original = jsonSchema {
            schema = "https://json-schema.org/draft/2020-12/schema"
            type = SchemaType.OBJECT
            properties {
                "name" to stringSchema { minLength = 1 }
                "scores" to arraySchema {
                    items(numberSchema { minimum = 0 })
                    uniqueItems = true
                }
            }
            required("name")
            additionalProperties(false)
        }

        val jsonString = original.toJsonString()
        val deserialized = jsonString.fromJsonOrThrow(JsonSchema)
        assertEquals(original, deserialized)
    }

    @Test
    fun `JsonSchema works as nested type in another FromJson class`() {
        val json = """{"name": "test", "schema": {"type": "STRING", "minLength": 1}}"""
        val wrapper = json.fromJsonOrThrow(SchemaWrapper)
        assertEquals("test", wrapper.name)
        assertEquals(SchemaType.STRING, wrapper.schema.type)
        assertEquals(1, wrapper.schema.minLength)
    }

    @Test
    fun `JsonSchema works in list within another FromJson class`() {
        val json = """{
            "name": "test",
            "schemas": [
                {"type": "STRING"},
                {"type": "INTEGER", "minimum": 0}
            ]
        }"""
        val wrapper = json.fromJsonOrThrow(SchemaListWrapper)
        assertEquals("test", wrapper.name)
        assertEquals(2, wrapper.schemas.size)
        assertEquals(SchemaType.STRING, wrapper.schemas[0].type)
        assertEquals(SchemaType.INTEGER, wrapper.schemas[1].type)
    }
}

internal data class SchemaWrapper(val name: String, val schema: JsonSchema) {
    companion object : FromJsonObject<SchemaWrapper>
}

internal data class SchemaListWrapper(val name: String, val schemas: List<JsonSchema>) {
    companion object : FromJsonObject<SchemaListWrapper>
}
