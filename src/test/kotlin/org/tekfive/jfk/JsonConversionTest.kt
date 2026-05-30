package org.tekfive.jfk

import kotlin.test.*

class JsonConversionTest {

    @Test
    fun `map to JsonObject`() {
        val map = mapOf("name" to "Alice", "age" to 30, "active" to true, "nothing" to null)
        val obj = map.toJsonObject()
        assertEquals("Alice", obj["name"].string)
        assertEquals(30, obj["age"].int)
        assertEquals(true, obj["active"].boolean)
        assertTrue(obj["nothing"].isNull)
    }

    @Test
    fun `list to JsonArray`() {
        val list = listOf("a", 1, true, null)
        val arr = list.toJsonArray()
        assertEquals("a", arr[0].string)
        assertEquals(1, arr[1].int)
        assertEquals(true, arr[2].boolean)
        assertTrue(arr[3].isNull)
    }

    @Test
    fun `nested map to JsonObject`() {
        val map = mapOf("person" to mapOf("name" to "Bob"))
        val obj = map.toJsonObject()
        assertEquals("Bob", obj["person"]["name"].string)
    }

    @Test
    fun `JsonObject to map`() {
        val obj = json {
            "name" set "Alice"
            "age" set 30
            "nothing" set null
        }
        val map = obj.toMap()
        assertEquals("Alice", map["name"])
        assertEquals(30, map["age"])
        assertNull(map["nothing"])
    }

    @Test
    fun `JsonArray to list`() {
        val arr = jsonArray("a", 1, true, null)
        val list = arr.toList()
        assertEquals(listOf("a", 1, true, null), list)
    }
}
