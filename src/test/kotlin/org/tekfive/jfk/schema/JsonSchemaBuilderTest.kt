package org.tekfive.jfk.schema

import org.tekfive.jfk.JsonNull
import org.tekfive.jfk.JsonNumber
import org.tekfive.jfk.JsonString
import kotlin.test.*

class JsonSchemaBuilderTest {

    @Test
    fun `jsonSchema builds empty schema`() {
        val schema = jsonSchema {}
        assertEquals(JsonSchema(), schema)
    }

    @Test
    fun `jsonSchema sets meta fields`() {
        val schema = jsonSchema {
            schema = "https://json-schema.org/draft/2020-12/schema"
            id = "https://example.com/person.json"
            title = "Person"
            description = "A person object"
        }
        assertEquals("https://json-schema.org/draft/2020-12/schema", schema.schema)
        assertEquals("https://example.com/person.json", schema.id)
        assertEquals("Person", schema.title)
        assertEquals("A person object", schema.description)
    }

    @Test
    fun `stringSchema sets type`() {
        val schema = stringSchema {
            minLength = 1
            maxLength = 100
            pattern = "^[a-z]+$"
            format = "email"
        }
        assertEquals(SchemaType.STRING, schema.type)
        assertEquals(1, schema.minLength)
        assertEquals(100, schema.maxLength)
        assertEquals("^[a-z]+$", schema.pattern)
        assertEquals("email", schema.format)
    }

    @Test
    fun `integerSchema sets type and numeric constraints`() {
        val schema = integerSchema {
            minimum = 0
            maximum = 100
            multipleOf = 5
        }
        assertEquals(SchemaType.INTEGER, schema.type)
        assertEquals(0, schema.minimum)
        assertEquals(100, schema.maximum)
        assertEquals(5, schema.multipleOf)
    }

    @Test
    fun `numberSchema sets type`() {
        val schema = numberSchema {
            exclusiveMinimum = 0.0
            exclusiveMaximum = 1.0
        }
        assertEquals(SchemaType.NUMBER, schema.type)
        assertEquals(0.0, schema.exclusiveMinimum)
        assertEquals(1.0, schema.exclusiveMaximum)
    }

    @Test
    fun `booleanSchema sets type`() {
        val schema = booleanSchema()
        assertEquals(SchemaType.BOOLEAN, schema.type)
    }

    @Test
    fun `nullSchema sets type`() {
        val schema = nullSchema()
        assertEquals(SchemaType.NULL, schema.type)
    }

    @Test
    fun `refSchema sets ref`() {
        val schema = refSchema("#/\$defs/Name")
        assertEquals("#/\$defs/Name", schema.ref)
    }

    @Test
    fun `arraySchema with items`() {
        val schema = arraySchema {
            items(stringSchema())
            minItems = 1
            maxItems = 10
            uniqueItems = true
        }
        assertEquals(SchemaType.ARRAY, schema.type)
        assertEquals(SchemaType.STRING, schema.items?.type)
        assertEquals(1, schema.minItems)
        assertEquals(10, schema.maxItems)
        assertEquals(true, schema.uniqueItems)
    }

    @Test
    fun `arraySchema with items builder`() {
        val schema = arraySchema {
            items {
                type = SchemaType.INTEGER
                minimum = 0
            }
        }
        assertEquals(SchemaType.INTEGER, schema.items?.type)
        assertEquals(0, schema.items?.minimum)
    }

    @Test
    fun `objectSchema with properties`() {
        val schema = objectSchema {
            properties {
                "name" to stringSchema { minLength = 1 }
                "age" to integerSchema { minimum = 0 }
                "active" to booleanSchema()
            }
            required("name", "age")
            additionalProperties(false)
        }
        assertEquals(SchemaType.OBJECT, schema.type)
        assertEquals(3, schema.properties?.size)
        assertEquals(SchemaType.STRING, schema.properties?.get("name")?.type)
        assertEquals(SchemaType.INTEGER, schema.properties?.get("age")?.type)
        assertEquals(SchemaType.BOOLEAN, schema.properties?.get("active")?.type)
        assertEquals(listOf("name", "age"), schema.required)
        assertEquals(AdditionalProperties.BooleanValue(false), schema.additionalProperties)
    }

    @Test
    fun `objectSchema with additionalProperties schema`() {
        val schema = objectSchema {
            additionalProperties(stringSchema())
        }
        val ap = schema.additionalProperties as AdditionalProperties.SchemaValue
        assertEquals(SchemaType.STRING, ap.schema.type)
    }

    @Test
    fun `objectSchema with min and max properties`() {
        val schema = objectSchema {
            minProperties = 1
            maxProperties = 5
        }
        assertEquals(1, schema.minProperties)
        assertEquals(5, schema.maxProperties)
    }

    @Test
    fun `types sets union types`() {
        val schema = jsonSchema {
            types(SchemaType.STRING, SchemaType.NULL)
        }
        assertNull(schema.type)
        assertEquals(listOf(SchemaType.STRING, SchemaType.NULL), schema.types)
    }

    @Test
    fun `enum sets enum values`() {
        val schema = jsonSchema {
            type = SchemaType.STRING
            enum(JsonString("red"), JsonString("green"), JsonString("blue"))
        }
        assertEquals(3, schema.enum?.size)
        assertEquals(JsonString("red"), schema.enum?.get(0))
    }

    @Test
    fun `const sets value with hasConst flag`() {
        val schema = jsonSchema {
            const(JsonString("fixed"))
        }
        assertTrue(schema.hasConst)
        assertEquals(JsonString("fixed"), schema.const)
    }

    @Test
    fun `const null sets value with hasConst flag`() {
        val schema = jsonSchema {
            const(JsonNull)
        }
        assertTrue(schema.hasConst)
        assertEquals(JsonNull, schema.const)
    }

    @Test
    fun `default sets value with hasDefault flag`() {
        val schema = jsonSchema {
            default(JsonNumber(42))
        }
        assertTrue(schema.hasDefault)
        assertEquals(JsonNumber(42), schema.default)
    }

    @Test
    fun `allOf composition`() {
        val schema = jsonSchema {
            allOf {
                schema { type = SchemaType.OBJECT }
                schema {
                    properties {
                        "name" to stringSchema()
                    }
                }
            }
        }
        assertEquals(2, schema.allOf?.size)
        assertEquals(SchemaType.OBJECT, schema.allOf?.get(0)?.type)
        assertNotNull(schema.allOf?.get(1)?.properties)
    }

    @Test
    fun `anyOf composition`() {
        val schema = jsonSchema {
            anyOf {
                schema { type = SchemaType.STRING }
                schema { type = SchemaType.INTEGER }
            }
        }
        assertEquals(2, schema.anyOf?.size)
    }

    @Test
    fun `oneOf composition`() {
        val schema = jsonSchema {
            oneOf {
                schema { type = SchemaType.STRING }
                schema { type = SchemaType.NUMBER }
            }
        }
        assertEquals(2, schema.oneOf?.size)
    }

    @Test
    fun `oneOf with direct schema`() {
        val str = stringSchema()
        val num = numberSchema()
        val schema = jsonSchema {
            oneOf {
                schema(str)
                schema(num)
            }
        }
        assertEquals(listOf(str, num), schema.oneOf)
    }

    @Test
    fun `not schema`() {
        val schema = jsonSchema {
            not { type = SchemaType.STRING }
        }
        assertEquals(SchemaType.STRING, schema.not?.type)
    }

    @Test
    fun `not with direct schema`() {
        val notTarget = stringSchema()
        val schema = jsonSchema {
            not(notTarget)
        }
        assertEquals(notTarget, schema.not)
    }

    @Test
    fun `defs builder`() {
        val schema = jsonSchema {
            defs {
                "Name" to stringSchema { minLength = 1 }
                "Age" to integerSchema { minimum = 0 }
            }
            type = SchemaType.OBJECT
            properties {
                "name" to refSchema("#/\$defs/Name")
                "age" to refSchema("#/\$defs/Age")
            }
        }
        assertEquals(2, schema.defs?.size)
        assertEquals(SchemaType.STRING, schema.defs?.get("Name")?.type)
        assertEquals(SchemaType.INTEGER, schema.defs?.get("Age")?.type)
        assertEquals("#/\$defs/Name", schema.properties?.get("name")?.ref)
    }

    @Test
    fun `full schema example`() {
        val schema = jsonSchema {
            schema = "https://json-schema.org/draft/2020-12/schema"
            id = "https://example.com/person.json"
            title = "Person"
            description = "A person record"
            type = SchemaType.OBJECT
            defs {
                "Address" to objectSchema {
                    properties {
                        "street" to stringSchema()
                        "city" to stringSchema()
                        "zip" to stringSchema { pattern = "^\\d{5}$" }
                    }
                    required("street", "city")
                }
            }
            properties {
                "name" to stringSchema { minLength = 1 }
                "age" to integerSchema { minimum = 0; maximum = 150 }
                "email" to stringSchema { format = "email" }
                "address" to refSchema("#/\$defs/Address")
                "tags" to arraySchema {
                    items(stringSchema())
                    uniqueItems = true
                }
            }
            required("name", "email")
            additionalProperties(false)
        }

        assertEquals("https://json-schema.org/draft/2020-12/schema", schema.schema)
        assertEquals(SchemaType.OBJECT, schema.type)
        assertEquals(5, schema.properties?.size)
        assertEquals(1, schema.defs?.size)
        assertEquals(listOf("name", "email"), schema.required)
        assertEquals(AdditionalProperties.BooleanValue(false), schema.additionalProperties)
    }
}
