package org.tekfive.jfk

import kotlin.test.*

class JsonValueTest {

    @Test
    fun `data class equality`() {
        assertEquals(JsonString("hello"), JsonString("hello"))
        assertEquals(JsonNumber(42), JsonNumber(42))
        assertEquals(JsonBool(true), JsonBool(true))
        assertEquals(JsonNull, JsonNull)
    }

    @Test
    fun `object key access`() {
        val obj = JsonObject(mapOf("name" to JsonString("Alice"), "age" to JsonNumber(30)))
        assertEquals("Alice", obj["name"].string)
        assertEquals(30, obj["age"].int)
        assertTrue(obj["missing"].isNull)
    }

    @Test
    fun `array index access`() {
        val arr = JsonArray(listOf(JsonString("a"), JsonNumber(1)))
        assertEquals("a", arr[0].string)
        assertEquals(1, arr[1].int)
        assertTrue(arr[99].isNull)
        assertTrue(arr[-1].isNull)
    }

    @Test
    fun `chained navigation`() {
        val value = JsonObject(mapOf(
            "address" to JsonObject(mapOf(
                "city" to JsonString("Austin")
            ))
        ))
        assertEquals("Austin", value["address"]["city"].string)
        assertTrue(value["address"]["zip"].isNull)
        assertTrue(value["nope"]["deep"].isNull)
    }

    @Test
    fun `dot-path navigation`() {
        val value = JsonObject(mapOf(
            "a" to JsonObject(mapOf(
                "b" to JsonObject(mapOf(
                    "c" to JsonNumber(42)
                ))
            ))
        ))
        assertEquals(42, value.at("a.b.c").int)
        assertTrue(value.at("a.b.missing").isNull)
    }

    @Test
    fun `typed accessors return null for wrong types`() {
        val str = JsonString("hello")
        assertEquals("hello", str.string)
        assertNull(str.int)
        assertNull(str.boolean)

        val num = JsonNumber(5)
        assertNull(num.string)
        assertNull(num.boolean)
        assertEquals(5, num.int)
        assertEquals(5L, num.long)
        assertEquals(5.0, num.double)

        val bool = JsonBool(true)
        assertNull(bool.string)
        assertNull(bool.int)
        assertEquals(true, bool.boolean)
    }

    @Test
    fun `integer accessors reject fractional numbers`() {
        val number = JsonNumber(1.9)
        assertNull(number.int)
        assertNull(number.long)
        assertEquals(1.9, number.double)
    }

    @Test
    fun `toJsonValue allows reused sibling collections`() {
        val shared = listOf("a", "b")
        val value = JsonValue.toJsonValue(listOf(shared, shared))

        assertEquals(listOf(listOf("a", "b"), listOf("a", "b")), value.toValue())
    }

    @Test
    fun `toJsonValue rejects recursive maps`() {
        val recursive = linkedMapOf<String, Any?>()
        recursive["self"] = recursive

        assertFailsWith<IllegalArgumentException> {
            JsonValue.toJsonValue(recursive)
        }
    }

    @Test
    fun `size property`() {
        assertEquals(0, JsonNull.size)
        assertEquals(2, JsonObject(mapOf("a" to JsonNull, "b" to JsonNull)).size)
        assertEquals(3, JsonArray(listOf(JsonNull, JsonNull, JsonNull)).size)
    }

    @Test
    fun `contains operators`() {
        val obj = JsonObject(mapOf("key" to JsonString("val")))
        assertTrue("key" in obj)
        assertFalse("other" in obj)

        val arr = JsonArray(listOf(JsonString("a"), JsonNumber(1)))
        assertTrue(JsonString("a") in arr)
        assertFalse(JsonString("b") in arr)
    }

    @Test
    fun `object plus returns merged object without mutating original`() {
        val original = JsonObject(mapOf(
            "id" to "one",
            "name" to "Original",
        ))

        val merged = original + mapOf(
            "name" to "Updated",
            "displayName" to "Updated Name",
            "nested" to mapOf("active" to true),
        )

        assertEquals("Original", original.reqString("name"))
        assertFalse(original.containsKey("displayName"))
        assertEquals("one", merged.reqString("id"))
        assertEquals("Updated", merged.reqString("name"))
        assertEquals("Updated Name", merged.reqString("displayName"))
        assertEquals(true, merged.reqObj("nested").reqBoolean("active"))
    }
}
