package org.tekfive.jfk

import kotlin.test.*

class StringExtensionTest {

    @Test
    fun `asJsonObject parses valid object`() {
        val obj = """{"name":"Alice","age":30}""".asJsonObject()
        assertNotNull(obj)
        assertEquals("Alice", obj["name"].string)
        assertEquals(30, obj["age"].int)
    }

    @Test
    fun `asJsonObject returns null for array`() {
        assertNull("""[1,2,3]""".asJsonObject())
    }

    @Test
    fun `asJsonObject returns null for invalid json`() {
        assertNull("not json".asJsonObject())
    }

    @Test
    fun `asJsonObject returns null for primitive`() {
        assertNull(""""hello"""".asJsonObject())
        assertNull("42".asJsonObject())
        assertNull("null".asJsonObject())
    }

    @Test
    fun `asJsonObject with transform`() {
        val transform = object : JsonTransform {
            override fun defaultKeys(path: List<String>) =
                if (path.isEmpty()) setOf("active") else emptySet()

            override fun default(path: List<String>, key: String): JsonValue? =
                if (key == "active") JsonBool(true) else null
        }
        val obj = """{"name":"Alice"}""".asJsonObject(transform)
        assertNotNull(obj)
        assertEquals(true, obj["active"].boolean)
    }

    @Test
    fun `asRequiredJsonObject parses valid object`() {
        val obj = """{"key":"value"}""".asRequiredJsonObject()
        assertEquals("value", obj["key"].string)
    }

    @Test
    fun `asRequiredJsonObject throws for array`() {
        assertFailsWith<IllegalArgumentException> {
            """[1,2,3]""".asRequiredJsonObject()
        }
    }

    @Test
    fun `asRequiredJsonObject throws for invalid json`() {
        assertFailsWith<IllegalArgumentException> {
            "not json".asRequiredJsonObject()
        }
    }

    @Test
    fun `asJsonArray parses valid array`() {
        val arr = """[1,"two",true]""".asJsonArray()
        assertNotNull(arr)
        assertEquals(1, arr[0].int)
        assertEquals("two", arr[1].string)
        assertEquals(true, arr[2].boolean)
    }

    @Test
    fun `asJsonArray returns null for object`() {
        assertNull("""{"key":"value"}""".asJsonArray())
    }

    @Test
    fun `asJsonArray returns null for invalid json`() {
        assertNull("not json".asJsonArray())
    }

    @Test
    fun `asJsonArray returns null for primitive`() {
        assertNull(""""hello"""".asJsonArray())
        assertNull("42".asJsonArray())
    }

    @Test
    fun `asJsonArray with transform`() {
        val transform = object : JsonTransform {
            override fun transform(path: List<String>, value: JsonValue): JsonValue {
                if (path.size == 1 && value is JsonNumber) {
                    return JsonNumber(value.value.toInt() * 10)
                }
                return value
            }
        }
        val arr = """[1,2,3]""".asJsonArray(transform)
        assertNotNull(arr)
        assertEquals(10, arr[0].int)
        assertEquals(20, arr[1].int)
        assertEquals(30, arr[2].int)
    }

    @Test
    fun `asRequiredJsonArray parses valid array`() {
        val arr = """[1,2]""".asRequiredJsonArray()
        assertEquals(2, arr.size)
    }

    @Test
    fun `asRequiredJsonArray throws for object`() {
        assertFailsWith<IllegalArgumentException> {
            """{"key":"value"}""".asRequiredJsonArray()
        }
    }

    @Test
    fun `asRequiredJsonArray throws for invalid json`() {
        assertFailsWith<IllegalArgumentException> {
            "not json".asRequiredJsonArray()
        }
    }
}
