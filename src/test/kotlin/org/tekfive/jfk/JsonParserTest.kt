package org.tekfive.jfk

import kotlin.test.*

class JsonParserTest {

    @Test
    fun `parse null`() {
        assertEquals(JsonNull, Json.parse("null"))
    }

    @Test
    fun `parse booleans`() {
        assertEquals(JsonBool(true), Json.parse("true"))
        assertEquals(JsonBool(false), Json.parse("false"))
    }

    @Test
    fun `parse integers`() {
        assertEquals(JsonNumber(0), Json.parse("0"))
        assertEquals(JsonNumber(42), Json.parse("42"))
        assertEquals(JsonNumber(-7), Json.parse("-7"))
    }

    @Test
    fun `parse large numbers as long`() {
        val big = Json.parse("3000000000") as JsonNumber
        assertEquals(3000000000L, big.long)
    }

    @Test
    fun `parse decimals`() {
        val num = Json.parse("3.14") as JsonNumber
        assertEquals(3.14, num.double)
    }

    @Test
    fun `parse exponent notation`() {
        val num = Json.parse("1.5e2") as JsonNumber
        assertEquals(150.0, num.double)
    }

    @Test
    fun `parse strings`() {
        assertEquals(JsonString("hello"), Json.parse("\"hello\""))
        assertEquals(JsonString(""), Json.parse("\"\""))
    }

    @Test
    fun `parse string escapes`() {
        assertEquals(JsonString("a\"b"), Json.parse(""""a\"b""""))
        assertEquals(JsonString("a\\b"), Json.parse(""""a\\b""""))
        assertEquals(JsonString("a/b"), Json.parse(""""a\/b""""))
        assertEquals(JsonString("a\nb"), Json.parse(""""a\nb""""))
        assertEquals(JsonString("a\tb"), Json.parse(""""a\tb""""))
        assertEquals(JsonString("a\rb"), Json.parse(""""a\rb""""))
        assertEquals(JsonString("a\u000Cb"), Json.parse(""""a\fb""""))
        assertEquals(JsonString("a\bb"), Json.parse(""""a\bb""""))
    }

    @Test
    fun `parse unicode escapes`() {
        assertEquals(JsonString("\u00E9"), Json.parse(""""\\u00e9"""".replace("\\\\", "\\")))
        // Simpler form
        val input = "\"\\u0041\"" // \u0041 = A
        assertEquals(JsonString("A"), Json.parse(input))
    }

    @Test
    fun `parse empty object`() {
        assertEquals(JsonObject(emptyMap()), Json.parse("{}"))
    }

    @Test
    fun `parse simple object`() {
        val result = Json.parse("""{"name":"Alice","age":30}""")
        assertEquals("Alice", result["name"].string)
        assertEquals(30, result["age"].int)
    }

    @Test
    fun `parse empty array`() {
        assertEquals(JsonArray(emptyList()), Json.parse("[]"))
    }

    @Test
    fun `parse simple array`() {
        val result = Json.parse("[1,2,3]") as JsonArray
        assertEquals(3, result.size)
        assertEquals(1, result[0].int)
    }

    @Test
    fun `parse nested structures`() {
        val input = """{"people":[{"name":"Alice"},{"name":"Bob"}]}"""
        val result = Json.parse(input)
        assertEquals("Bob", result["people"][1]["name"].string)
    }

    @Test
    fun `parse with whitespace`() {
        val input = """
            {
                "key" : "value" ,
                "arr" : [ 1 , 2 ]
            }
        """
        val result = Json.parse(input)
        assertEquals("value", result["key"].string)
        assertEquals(2, result["arr"][1].int)
    }

    @Test
    fun `parse error on invalid input`() {
        assertFailsWith<JsonParseException> { Json.parse("{invalid}") }
        assertFailsWith<JsonParseException> { Json.parse("[,]") }
        assertFailsWith<JsonParseException> { Json.parse("") }
        assertFailsWith<JsonParseException> { Json.parse("{\"a\":1} extra") }
    }

    @Test
    fun `parse preserves object key order`() {
        val result = Json.parse("""{"z":1,"a":2,"m":3}""") as JsonObject
        assertEquals(listOf("z", "a", "m"), result.keys.toList())
    }
}
