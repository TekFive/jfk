package org.tekfive.jfk

import kotlin.test.*

class LaxAccessorTest {

    // laxInt
    @Test
    fun `laxInt returns int from JsonNumber`() {
        assertEquals(42, JsonNumber(42).laxInt)
    }

    @Test
    fun `laxInt coerces string to int`() {
        assertEquals(42, JsonString("42").laxInt)
    }

    @Test
    fun `laxInt returns null for non-numeric string`() {
        assertNull(JsonString("abc").laxInt)
    }

    @Test
    fun `laxInt returns null for decimal string`() {
        assertNull(JsonString("3.14").laxInt)
    }

    @Test
    fun `laxInt returns null for null`() {
        assertNull(JsonNull.laxInt)
    }

    @Test
    fun `laxInt returns null for bool`() {
        assertNull(JsonBool(true).laxInt)
    }

    // laxLong
    @Test
    fun `laxLong returns long from JsonNumber`() {
        assertEquals(3000000000L, JsonNumber(3000000000L).laxLong)
    }

    @Test
    fun `laxLong coerces string to long`() {
        assertEquals(3000000000L, JsonString("3000000000").laxLong)
    }

    @Test
    fun `laxLong returns null for non-numeric string`() {
        assertNull(JsonString("abc").laxLong)
    }

    @Test
    fun `laxLong returns null for null`() {
        assertNull(JsonNull.laxLong)
    }

    // laxDouble
    @Test
    fun `laxDouble returns double from JsonNumber`() {
        assertEquals(3.14, JsonNumber(3.14).laxDouble)
    }

    @Test
    fun `laxDouble coerces string to double`() {
        assertEquals(3.14, JsonString("3.14").laxDouble)
    }

    @Test
    fun `laxDouble coerces integer string to double`() {
        assertEquals(42.0, JsonString("42").laxDouble)
    }

    @Test
    fun `laxDouble returns null for non-numeric string`() {
        assertNull(JsonString("abc").laxDouble)
    }

    @Test
    fun `laxDouble returns null for non-finite string`() {
        assertNull(JsonString("NaN").laxDouble)
        assertNull(JsonString("Infinity").laxDouble)
        assertNull(JsonString("-Infinity").laxDouble)
    }

    @Test
    fun `laxDouble returns null for null`() {
        assertNull(JsonNull.laxDouble)
    }

    // laxBoolean
    @Test
    fun `laxBoolean returns boolean from JsonBool`() {
        assertEquals(true, JsonBool(true).laxBoolean)
        assertEquals(false, JsonBool(false).laxBoolean)
    }

    @Test
    fun `laxBoolean coerces string true`() {
        assertEquals(true, JsonString("true").laxBoolean)
    }

    @Test
    fun `laxBoolean coerces string false`() {
        assertEquals(false, JsonString("false").laxBoolean)
    }

    @Test
    fun `laxBoolean returns null for non-boolean string`() {
        assertNull(JsonString("yes").laxBoolean)
        assertNull(JsonString("1").laxBoolean)
    }

    @Test
    fun `laxBoolean returns null for null`() {
        assertNull(JsonNull.laxBoolean)
    }

    @Test
    fun `laxBoolean returns null for number`() {
        assertNull(JsonNumber(1).laxBoolean)
    }

    // end-to-end with parsed JSON
    @Test
    fun `lax accessors on parsed json with string numbers`() {
        val result = Json.parse("""{"count":"42","rate":"3.14","big":"3000000000","active":"true"}""")
        assertEquals(42, result["count"].laxInt)
        assertEquals(3.14, result["rate"].laxDouble)
        assertEquals(3000000000L, result["big"].laxLong)
        assertEquals(true, result["active"].laxBoolean)
    }

    @Test
    fun `lax accessors fall through to strict when already correct type`() {
        val result = Json.parse("""{"count":42,"rate":3.14,"active":true}""")
        assertEquals(42, result["count"].laxInt)
        assertEquals(3.14, result["rate"].laxDouble)
        assertEquals(true, result["active"].laxBoolean)
    }
}
