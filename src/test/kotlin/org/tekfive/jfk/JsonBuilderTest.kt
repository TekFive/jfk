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
    fun `build object with to syntax`() {
        data class User(val name: String) : ToJsonObject

        val obj = json {
            "name" to "Alice"
            "age" to 30
            "active" to true
            "score" to null
            "tags" to listOf("kotlin", "json")
            "user" to User("Bob")
            "nested" to json {
                "city" to "Austin"
            }
        }
        assertEquals("Alice", obj["name"].string)
        assertEquals(30, obj["age"].int)
        assertEquals(true, obj["active"].boolean)
        assertTrue(obj["score"].isNull)
        assertEquals("json", obj["tags"][1].string)
        assertEquals("Bob", obj["user"]["name"].string)
        assertEquals("Austin", obj["nested"]["city"].string)
    }

    @Test
    fun `to and set syntax interoperate`() {
        val obj = json {
            "a" set "one"
            "b" to "two"
        }
        assertEquals("one", obj["a"].string)
        assertEquals("two", obj["b"].string)
        assertEquals(listOf("a", "b"), obj.keys.toList())
    }

    @Test
    fun `merge object with unary plus`() {
        val base = json {
            "name" set "Alice"
            "age" set 30
        }
        val obj = json {
            +base
            "additional" set "property"
        }
        assertEquals("Alice", obj["name"].string)
        assertEquals(30, obj["age"].int)
        assertEquals("property", obj["additional"].string)
    }

    @Test
    fun `merge ToJsonObject with unary plus`() {
        data class User(val name: String, val age: Int) : ToJsonObject

        val obj = json {
            +User("Bob", 25)
            "additional" set "property"
        }
        assertEquals("Bob", obj["name"].string)
        assertEquals(25, obj["age"].int)
        assertEquals("property", obj["additional"].string)
    }

    @Test
    fun `merge overwrites earlier properties and is overwritten by later ones`() {
        val merged = json {
            "a" set "merged"
            "b" set "merged"
        }
        val obj = json {
            "a" set "original"
            +merged
            "b" set "after"
        }
        assertEquals("merged", obj["a"].string)
        assertEquals("after", obj["b"].string)
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
