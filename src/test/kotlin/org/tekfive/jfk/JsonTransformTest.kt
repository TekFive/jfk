package org.tekfive.jfk

import kotlin.test.*

class JsonTransformTest {

    @Test
    fun `transform values by path`() {
        val input = """{"name":"alice","age":25}"""
        val transform = object : JsonTransform {
            override fun transform(path: List<String>, value: JsonValue): JsonValue {
                if (path == listOf("name") && value is JsonString) {
                    return JsonString(value.value.replaceFirstChar { it.uppercase() })
                }
                return value
            }
        }
        val result = Json.parse(input, transform)
        assertEquals("Alice", result["name"].string)
        assertEquals(25, result["age"].int)
    }

    @Test
    fun `transform nested values`() {
        val input = """{"person":{"name":"bob"}}"""
        val transform = object : JsonTransform {
            override fun transform(path: List<String>, value: JsonValue): JsonValue {
                if (path == listOf("person", "name") && value is JsonString) {
                    return JsonString(value.value.uppercase())
                }
                return value
            }
        }
        val result = Json.parse(input, transform)
        assertEquals("BOB", result["person"]["name"].string)
    }

    @Test
    fun `transform array elements by index path`() {
        val input = """{"scores":[10,20,30]}"""
        val transform = object : JsonTransform {
            override fun transform(path: List<String>, value: JsonValue): JsonValue {
                if (path.size == 2 && path[0] == "scores" && value is JsonNumber) {
                    return JsonNumber(value.value.toInt() * 2)
                }
                return value
            }
        }
        val result = Json.parse(input, transform)
        assertEquals(20, result["scores"][0].int)
        assertEquals(40, result["scores"][1].int)
        assertEquals(60, result["scores"][2].int)
    }

    @Test
    fun `defaults for missing keys`() {
        val input = """{"name":"Alice"}"""
        val transform = object : JsonTransform {
            override fun defaultKeys(path: List<String>): Set<String> {
                if (path.isEmpty()) return setOf("age", "active")
                return emptySet()
            }

            override fun default(path: List<String>, key: String): JsonValue? = when (key) {
                "age" -> JsonNumber(0)
                "active" -> JsonBool(true)
                else -> null
            }
        }
        val result = Json.parse(input, transform)
        assertEquals("Alice", result["name"].string)
        assertEquals(0, result["age"].int)
        assertEquals(true, result["active"].boolean)
    }

    @Test
    fun `defaults do not override existing keys`() {
        val input = """{"name":"Alice","age":30}"""
        val transform = object : JsonTransform {
            override fun defaultKeys(path: List<String>): Set<String> {
                if (path.isEmpty()) return setOf("age", "active")
                return emptySet()
            }

            override fun default(path: List<String>, key: String): JsonValue? = when (key) {
                "age" -> JsonNumber(0)
                "active" -> JsonBool(true)
                else -> null
            }
        }
        val result = Json.parse(input, transform)
        assertEquals(30, result["age"].int)
        assertEquals(true, result["active"].boolean)
    }

    @Test
    fun `defaults for nested objects`() {
        val input = """{"address":{"city":"Austin"}}"""
        val transform = object : JsonTransform {
            override fun defaultKeys(path: List<String>): Set<String> {
                if (path == listOf("address")) return setOf("zip", "state")
                return emptySet()
            }

            override fun default(path: List<String>, key: String): JsonValue? = when (key) {
                "zip" -> JsonString("00000")
                "state" -> JsonString("TX")
                else -> null
            }
        }
        val result = Json.parse(input, transform)
        assertEquals("Austin", result["address"]["city"].string)
        assertEquals("00000", result["address"]["zip"].string)
        assertEquals("TX", result["address"]["state"].string)
    }

    @Test
    fun `defaults for empty object`() {
        val input = """{}"""
        val transform = object : JsonTransform {
            override fun defaultKeys(path: List<String>): Set<String> {
                if (path.isEmpty()) return setOf("version")
                return emptySet()
            }

            override fun default(path: List<String>, key: String): JsonValue? = when (key) {
                "version" -> JsonNumber(1)
                else -> null
            }
        }
        val result = Json.parse(input, transform)
        assertEquals(1, result["version"].int)
    }

    @Test
    fun `combined transform and defaults`() {
        val input = """{"price":"19.99"}"""
        val transform = object : JsonTransform {
            override fun transform(path: List<String>, value: JsonValue): JsonValue {
                if (path == listOf("price") && value is JsonString) {
                    return JsonNumber(value.value.toDouble())
                }
                return value
            }

            override fun defaultKeys(path: List<String>): Set<String> {
                if (path.isEmpty()) return setOf("currency")
                return emptySet()
            }

            override fun default(path: List<String>, key: String): JsonValue? = when (key) {
                "currency" -> JsonString("USD")
                else -> null
            }
        }
        val result = Json.parse(input, transform)
        assertEquals(19.99, result["price"].double)
        assertEquals("USD", result["currency"].string)
    }

    @Test
    fun `transform root value`() {
        val input = """[1,2,3]"""
        val transform = object : JsonTransform {
            override fun transform(path: List<String>, value: JsonValue): JsonValue {
                if (path.isEmpty() && value is JsonArray) {
                    return JsonObject(mapOf("data" to value))
                }
                return value
            }
        }
        val result = Json.parse(input, transform)
        assertEquals(1, result["data"][0].int)
    }

    @Test
    fun `null transform parses normally`() {
        val input = """{"name":"Alice"}"""
        val result = Json.parse(input, null)
        assertEquals("Alice", result["name"].string)
    }

    @Test
    fun `no transform parses normally`() {
        val input = """{"name":"Alice"}"""
        val result = Json.parse(input)
        assertEquals("Alice", result["name"].string)
    }

    @Test
    fun `default returning null skips key`() {
        val input = """{"name":"Alice"}"""
        val transform = object : JsonTransform {
            override fun defaultKeys(path: List<String>): Set<String> {
                if (path.isEmpty()) return setOf("age", "skip")
                return emptySet()
            }

            override fun default(path: List<String>, key: String): JsonValue? = when (key) {
                "age" -> JsonNumber(0)
                "skip" -> null
                else -> null
            }
        }
        val result = Json.parse(input, transform)
        assertEquals(0, result["age"].int)
        assertTrue(result["skip"].isNull)
        assertEquals(2, (result as JsonObject).size)
    }
}
