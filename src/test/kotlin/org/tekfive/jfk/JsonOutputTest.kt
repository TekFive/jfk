package org.tekfive.jfk

import java.io.StringWriter
import kotlin.test.*

class JsonOutputTest {

    @Test
    fun `compact output primitives`() {
        assertEquals("null", JsonNull.toJsonString())
        assertEquals("true", JsonBool(true).toJsonString())
        assertEquals("false", JsonBool(false).toJsonString())
        assertEquals("42", JsonNumber(42).toJsonString())
        assertEquals("3.14", JsonNumber(3.14).toJsonString())
        assertEquals("\"hello\"", JsonString("hello").toJsonString())
    }

    @Test
    fun `compact output escapes strings`() {
        assertEquals("\"a\\\"b\"", JsonString("a\"b").toJsonString())
        assertEquals("\"a\\\\b\"", JsonString("a\\b").toJsonString())
        assertEquals("\"a\\nb\"", JsonString("a\nb").toJsonString())
        assertEquals("\"a\\tb\"", JsonString("a\tb").toJsonString())
    }

    @Test
    fun `compact output object`() {
        val obj = json {
            "name" set "Alice"
            "age" set 30
        }
        assertEquals("""{"name":"Alice","age":30}""", obj.toJsonString())
    }

    @Test
    fun `compact output array`() {
        val arr = jsonArray(1, 2, 3)
        assertEquals("[1,2,3]", arr.toJsonString())
    }

    @Test
    fun `compact output empty containers`() {
        assertEquals("{}", JsonObject(emptyMap()).toJsonString())
        assertEquals("[]", JsonArray(emptyList()).toJsonString())
    }

    @Test
    fun `pretty print object`() {
        val obj = json {
            "name" set "Alice"
            "age" set 30
        }
        val expected = """
            |{
            |  "name": "Alice",
            |  "age": 30
            |}
        """.trimMargin()
        assertEquals(expected, obj.toJsonString(indent = 2))
    }

    @Test
    fun `pretty print nested`() {
        val obj = json {
            "person" set json {
                "name" set "Bob"
            }
        }
        val expected = """
            |{
            |  "person": {
            |    "name": "Bob"
            |  }
            |}
        """.trimMargin()
        assertEquals(expected, obj.toJsonString(indent = 2))
    }

    @Test
    fun `writeTo writer`() {
        val obj = json { "key" set "value" }
        val writer = StringWriter()
        obj.writeTo(writer)
        assertEquals("""{"key":"value"}""", writer.toString())
    }

    @Test
    fun `roundtrip parse and output`() {
        val input = """{"name":"Alice","scores":[100,95.5,null],"active":true,"address":{"city":"Austin"}}"""
        val parsed = Json.parse(input)
        assertEquals(input, parsed.toJsonString())
    }

    @Test
    fun `integer output for whole numbers`() {
        assertEquals("100", JsonNumber(100).toJsonString())
        assertEquals("100", JsonNumber(100L).toJsonString())
        assertEquals("100", JsonNumber(100.0).toJsonString())
        assertEquals("3.14", JsonNumber(3.14).toJsonString())
    }

    @Test
    fun `non-finite numbers are rejected`() {
        assertFailsWith<IllegalArgumentException> { JsonNumber(Double.NaN) }
        assertFailsWith<IllegalArgumentException> { JsonNumber(Double.POSITIVE_INFINITY) }
        assertFailsWith<IllegalArgumentException> { JsonNumber(Double.NEGATIVE_INFINITY) }
    }
}
