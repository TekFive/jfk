package org.tekfive.jfk

import kotlin.test.*

class ReqAccessorTest {

    // reqString
    @Test
    fun `reqString returns string value`() {
        val obj = Json.parse("""{"name":"Alice"}""")
        assertEquals("Alice", obj["name"].reqString)
    }

    @Test
    fun `reqString coerces number to string`() {
        assertEquals("42", JsonNumber(42).reqString)
    }

    @Test
    fun `reqString coerces boolean to string`() {
        assertEquals("true", JsonBool(true).reqString)
    }

    @Test
    fun `reqString throws for null with path`() {
        val obj = Json.parse("""{"name":"Alice"}""")
        val ex = assertFailsWith<IllegalStateException> { obj["missing"].reqString }
        assertTrue(ex.message!!.contains("missing"))
        assertTrue(ex.message!!.contains("String"))
        assertTrue(ex.message!!.contains("JsonNull"))
    }

    @Test
    fun `reqString throws for object`() {
        val obj = Json.parse("""{"data":{"nested":true}}""")
        val ex = assertFailsWith<IllegalStateException> { obj["data"].reqString }
        assertTrue(ex.message!!.contains("data"))
        assertTrue(ex.message!!.contains("JsonObject"))
    }

    @Test
    fun `reqString throws for array`() {
        val obj = Json.parse("""{"items":[1,2]}""")
        val ex = assertFailsWith<IllegalStateException> { obj["items"].reqString }
        assertTrue(ex.message!!.contains("items"))
        assertTrue(ex.message!!.contains("JsonArray"))
    }

    // reqInt
    @Test
    fun `reqInt returns int value`() {
        val obj = Json.parse("""{"age":30}""")
        assertEquals(30, obj["age"].reqInt)
    }

    @Test
    fun `reqInt coerces string to int`() {
        val obj = Json.parse("""{"age":"30"}""")
        assertEquals(30, obj["age"].reqInt)
    }

    @Test
    fun `reqInt throws for non-numeric string with path`() {
        val obj = Json.parse("""{"age":"abc"}""")
        val ex = assertFailsWith<IllegalStateException> { obj["age"].reqInt }
        assertTrue(ex.message!!.contains("age"))
        assertTrue(ex.message!!.contains("Int"))
        assertTrue(ex.message!!.contains("abc"))
    }

    // reqLong
    @Test
    fun `reqLong returns long value`() {
        val obj = Json.parse("""{"id":3000000000}""")
        assertEquals(3000000000L, obj["id"].reqLong)
    }

    @Test
    fun `reqLong coerces string to long`() {
        val obj = Json.parse("""{"id":"3000000000"}""")
        assertEquals(3000000000L, obj["id"].reqLong)
    }

    @Test
    fun `reqLong throws for null with path`() {
        val obj = Json.parse("""{}""")
        val ex = assertFailsWith<IllegalStateException> { obj["id"].reqLong }
        assertTrue(ex.message!!.contains("id"))
        assertTrue(ex.message!!.contains("Long"))
        assertTrue(ex.message!!.contains("JsonNull"))
    }

    // reqDouble
    @Test
    fun `reqDouble returns double value`() {
        val obj = Json.parse("""{"rate":3.14}""")
        assertEquals(3.14, obj["rate"].reqDouble)
    }

    @Test
    fun `reqDouble coerces string to double`() {
        val obj = Json.parse("""{"rate":"3.14"}""")
        assertEquals(3.14, obj["rate"].reqDouble)
    }

    @Test
    fun `reqDouble throws for boolean with path`() {
        val obj = Json.parse("""{"rate":true}""")
        val ex = assertFailsWith<IllegalStateException> { obj["rate"].reqDouble }
        assertTrue(ex.message!!.contains("rate"))
        assertTrue(ex.message!!.contains("Double"))
        assertTrue(ex.message!!.contains("JsonBool"))
    }

    // reqBoolean
    @Test
    fun `reqBoolean returns boolean value`() {
        val obj = Json.parse("""{"active":true}""")
        assertEquals(true, obj["active"].reqBoolean)
    }

    @Test
    fun `reqBoolean coerces string to boolean`() {
        val obj = Json.parse("""{"active":"false"}""")
        assertEquals(false, obj["active"].reqBoolean)
    }

    @Test
    fun `reqBoolean throws for number with path`() {
        val obj = Json.parse("""{"active":1}""")
        val ex = assertFailsWith<IllegalStateException> { obj["active"].reqBoolean }
        assertTrue(ex.message!!.contains("active"))
        assertTrue(ex.message!!.contains("Boolean"))
        assertTrue(ex.message!!.contains("JsonNumber"))
    }

    // Nested path tracking
    @Test
    fun `nested path in error message`() {
        val obj = Json.parse("""{"person":{"address":{"city":null}}}""")
        val ex = assertFailsWith<IllegalStateException> { obj["person"]["address"]["city"].reqString }
        assertTrue(ex.message!!.contains("person.address.city"))
        assertTrue(ex.message!!.contains("String"))
        assertTrue(ex.message!!.contains("JsonNull"))
    }

    @Test
    fun `array index in path`() {
        val obj = Json.parse("""{"items":[{"name":"a"},{"name":null}]}""")
        val ex = assertFailsWith<IllegalStateException> { obj["items"][1]["name"].reqString }
        assertTrue(ex.message!!.contains("items.1.name"))
        assertTrue(ex.message!!.contains("JsonNull"))
    }

    @Test
    fun `deeply nested missing path`() {
        val obj = Json.parse("""{"a":{"b":{}}}""")
        val ex = assertFailsWith<IllegalStateException> { obj["a"]["b"]["c"]["d"].reqString }
        assertTrue(ex.message!!.contains("a.b.c.d"))
    }

    @Test
    fun `root level req throws with root path`() {
        val obj = Json.parse("null")
        val ex = assertFailsWith<IllegalStateException> { obj.reqString }
        assertTrue(ex.message!!.contains("<root>"))
    }

    // at() path tracking
    @Test
    fun `at path in error message`() {
        val obj = Json.parse("""{"a":{"b":{"c":null}}}""")
        val ex = assertFailsWith<IllegalStateException> { obj.at("a.b.c").reqInt }
        assertTrue(ex.message!!.contains("a.b.c"))
    }
}
