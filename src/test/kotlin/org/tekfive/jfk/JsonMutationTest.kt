package org.tekfive.jfk

import kotlin.test.*

class JsonMutationTest {

    @Test
    fun `set string on object`() {
        val obj = json { "name" set "Alice" }
        obj["name"] = "Bob"
        assertEquals("Bob", obj["name"].string)
    }

    @Test
    fun `set number on object`() {
        val obj = json { "age" set 25 }
        obj["age"] = 30
        assertEquals(30, obj["age"].int)
    }

    @Test
    fun `set boolean on object`() {
        val obj = json { "active" set false }
        obj["active"] = true
        assertEquals(true, obj["active"].boolean)
    }

    @Test
    fun `set null on object`() {
        val obj = json { "name" set "Alice" }
        obj["name"] = null
        assertTrue(obj["name"].isNull)
    }

    @Test
    fun `set JsonValue on object`() {
        val obj = json { "data" set "old" }
        obj["data"] = jsonArray(1, 2, 3)
        assertEquals(2, obj["data"][1].int)
    }

    @Test
    fun `add new property to object`() {
        val obj = json { "name" set "Alice" }
        obj["age"] = 30
        obj["active"] = true
        assertEquals("Alice", obj["name"].string)
        assertEquals(30, obj["age"].int)
        assertEquals(true, obj["active"].boolean)
        assertEquals(3, obj.size)
    }

    @Test
    fun `remove property from object`() {
        val obj = json {
            "name" set "Alice"
            "age" set 30
        }
        val removed = obj.remove("age")
        assertEquals(30, removed?.int)
        assertTrue(obj["age"].isNull)
        assertEquals(1, obj.size)
    }

    @Test
    fun `remove nonexistent key returns null`() {
        val obj = json { "name" set "Alice" }
        assertNull(obj.remove("missing"))
    }

    @Test
    fun `set string on array by index`() {
        val arr = jsonArray("a", "b", "c")
        arr[1] = "B"
        assertEquals("B", arr[1].string)
    }

    @Test
    fun `set number on array by index`() {
        val arr = jsonArray(1, 2, 3)
        arr[0] = 10
        assertEquals(10, arr[0].int)
    }

    @Test
    fun `set boolean on array by index`() {
        val arr = jsonArray(true, false)
        arr[1] = true
        assertEquals(true, arr[1].boolean)
    }

    @Test
    fun `set null on array by index`() {
        val arr = jsonArray("a", "b")
        arr[0] = null
        assertTrue(arr[0].isNull)
    }

    @Test
    fun `set JsonValue on array by index`() {
        val arr = jsonArray(1, 2, 3)
        arr[1] = json { "nested" set true }
        assertEquals(true, arr[1]["nested"].boolean)
    }

    @Test
    fun `add elements to array`() {
        val arr = jsonArray(1)
        arr.add("two")
        arr.add(3)
        arr.add(true)
        arr.addNull()
        arr.add(JsonString("five"))
        assertEquals(6, arr.size)
        assertEquals("two", arr[1].string)
        assertEquals(3, arr[2].int)
        assertEquals(true, arr[3].boolean)
        assertTrue(arr[4].isNull)
        assertEquals("five", arr[5].string)
    }

    @Test
    fun `removeAt from array`() {
        val arr = jsonArray("a", "b", "c")
        val removed = arr.removeAt(1)
        assertEquals("b", removed.string)
        assertEquals(2, arr.size)
        assertEquals("c", arr[1].string)
    }

    @Test
    fun `mutate parsed json`() {
        val result = Json.parse("""{"name":"Alice","scores":[90,85]}""")
        val obj = result as JsonObject
        obj["name"] = "Bob"
        obj["active"] = true
        val scores = obj["scores"] as JsonArray
        scores[0] = 95
        scores.add(100)

        assertEquals("Bob", obj["name"].string)
        assertEquals(true, obj["active"].boolean)
        assertEquals(95, obj["scores"][0].int)
        assertEquals(100, obj["scores"][2].int)
    }

    @Test
    fun `builder results are mutable`() {
        val obj = json {
            "items" set jsonArray {
                addObject { "id" set 1 }
            }
        }
        (obj["items"] as JsonArray).add(json { "id" set 2 })
        assertEquals(2, obj["items"][1]["id"].int)
    }
}
