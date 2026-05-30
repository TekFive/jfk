package org.tekfive.jfk

import kotlin.test.*

class JsonBuilderTest {

    @Test
    fun `build simple object`() {
        val obj = json {
            "name" set "Alice"
            "age" set 30
            "active" set true
            "score" set null
        }
        assertEquals("Alice", obj["name"].string)
        assertEquals(30, obj["age"].int)
        assertEquals(true, obj["active"].boolean)
        assertTrue(obj["score"].isNull)
    }

    @Test
    fun `build nested object`() {
        val obj = json {
            "person" set json {
                "name" set "Bob"
                "address" set json {
                    "city" set "Austin"
                }
            }
        }
        assertEquals("Austin", obj["person"]["address"]["city"].string)
    }

    @Test
    fun `build with jsonArray varargs`() {
        val obj = json {
            "tags" set jsonArray("kotlin", "json")
            "nums" set jsonArray(1, 2, 3)
        }
        assertEquals("kotlin", obj["tags"][0].string)
        assertEquals(3, obj["nums"][2].int)
    }

    @Test
    fun `build array with builder`() {
        val arr = jsonArray {
            add("hello")
            add(42)
            add(true)
            addNull()
            addObject {
                "key" set "value"
            }
            addArray {
                add(1)
                add(2)
            }
        }
        assertEquals(6, arr.size)
        assertEquals("hello", arr[0].string)
        assertEquals(42, arr[1].int)
        assertEquals(true, arr[2].boolean)
        assertTrue(arr[3].isNull)
        assertEquals("value", arr[4]["key"].string)
        assertEquals(2, arr[5][1].int)
    }

    @Test
    fun `build array with unary plus`() {
        val arr = jsonArray {
            +"hello"
            +JsonNumber(42)
            +JsonBool(false)
        }
        assertEquals("hello", arr[0].string)
        assertEquals(42, arr[1].int)
        assertEquals(false, arr[2].boolean)
    }

    @Test
    fun `preserves insertion order`() {
        val obj = json {
            "z" set 1
            "a" set 2
            "m" set 3
        }
        assertEquals(listOf("z", "a", "m"), obj.keys.toList())
    }
}
