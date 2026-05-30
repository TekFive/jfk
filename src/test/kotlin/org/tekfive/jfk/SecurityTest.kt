package org.tekfive.jfk

import kotlin.test.*

class SecurityTest {

    // --- Finding 1: Max depth ---

    @Test
    fun `rejects deeply nested arrays`() {
        val input = "[".repeat(600) + "]".repeat(600)
        assertFailsWith<JsonParseException> {
            Json.parse(input)
        }.also {
            assertTrue(it.message!!.contains("depth"))
        }
    }

    @Test
    fun `rejects deeply nested objects`() {
        val sb = StringBuilder()
        repeat(600) { i -> sb.append("""{"k$i":""") }
        sb.append("1")
        repeat(600) { sb.append("}") }
        assertFailsWith<JsonParseException> {
            Json.parse(sb.toString())
        }.also {
            assertTrue(it.message!!.contains("depth"))
        }
    }

    @Test
    fun `accepts nesting within limit`() {
        val input = "[".repeat(100) + "1" + "]".repeat(100)
        val result = Json.parse(input)
        assertNotNull(result)
    }

    @Test
    fun `custom depth limit`() {
        val config = JsonParserConfig(maxDepth = 5)
        val input = "[[[[[[1]]]]]]" // depth 6
        assertFailsWith<JsonParseException> {
            Json.parse(input, config = config)
        }
    }

    @Test
    fun `depth limit can be disabled`() {
        val config = JsonParserConfig(maxDepth = 0)
        val input = "[".repeat(600) + "1" + "]".repeat(600)
        val result = Json.parse(input, config = config)
        assertNotNull(result)
    }

    // --- Finding 2: Max string length ---

    @Test
    fun `rejects oversized string`() {
        val config = JsonParserConfig(maxStringLength = 100)
        val input = "\"${"a".repeat(101)}\""
        assertFailsWith<JsonParseException> {
            Json.parse(input, config = config)
        }.also {
            assertTrue(it.message!!.contains("String exceeds"))
        }
    }

    @Test
    fun `accepts string within limit`() {
        val config = JsonParserConfig(maxStringLength = 100)
        val input = "\"${"a".repeat(100)}\""
        val result = Json.parse(input, config = config)
        assertEquals("a".repeat(100), result.string)
    }

    // --- Finding 2: Max number length ---

    @Test
    fun `rejects oversized number literal`() {
        val config = JsonParserConfig(maxNumberLength = 20)
        val input = "1".repeat(21)
        assertFailsWith<JsonParseException> {
            Json.parse(input, config = config)
        }.also {
            assertTrue(it.message!!.contains("Number literal exceeds"))
        }
    }

    @Test
    fun `accepts number within limit`() {
        val config = JsonParserConfig(maxNumberLength = 20)
        val input = "1234567890123456789" // 19 chars, fits in Long
        val result = Json.parse(input, config = config)
        assertNotNull(result.long)
    }

    // --- Finding 3: Number overflow ---

    @Test
    fun `rejects integer overflow`() {
        val input = "99999999999999999999" // exceeds Long.MAX_VALUE
        assertFailsWith<JsonParseException> {
            Json.parse(input)
        }.also {
            assertTrue(it.message!!.contains("out of range"))
        }
    }

    @Test
    fun `rejects double overflow`() {
        val input = "1e99999"
        assertFailsWith<JsonParseException> {
            Json.parse(input)
        }.also {
            assertTrue(it.message!!.contains("out of range"))
        }
    }

    @Test
    fun `int accessor returns null for out-of-range long`() {
        val big = JsonNumber(3_000_000_000L)
        assertNull(big.int)
        assertEquals(3_000_000_000L, big.long)
    }

    @Test
    fun `int accessor works for in-range values`() {
        assertEquals(42, JsonNumber(42).int)
        assertEquals(42, JsonNumber(42L).int)
    }

    // --- Finding 5: Max object entries ---

    @Test
    fun `rejects oversized object`() {
        val config = JsonParserConfig(maxObjectEntries = 5)
        val entries = (0..5).joinToString(",") { "\"k$it\":$it" }
        val input = "{$entries}"
        assertFailsWith<JsonParseException> {
            Json.parse(input, config = config)
        }.also {
            assertTrue(it.message!!.contains("Object exceeds"))
        }
    }

    @Test
    fun `accepts object within limit`() {
        val config = JsonParserConfig(maxObjectEntries = 5)
        val entries = (0..4).joinToString(",") { "\"k$it\":$it" }
        val input = "{$entries}"
        val result = Json.parse(input, config = config) as JsonObject
        assertEquals(5, result.size)
    }

    // --- Finding 5: Max array elements ---

    @Test
    fun `rejects oversized array`() {
        val config = JsonParserConfig(maxArrayElements = 5)
        val input = "[1,2,3,4,5,6]"
        assertFailsWith<JsonParseException> {
            Json.parse(input, config = config)
        }.also {
            assertTrue(it.message!!.contains("Array exceeds"))
        }
    }

    @Test
    fun `accepts array within limit`() {
        val config = JsonParserConfig(maxArrayElements = 5)
        val input = "[1,2,3,4,5]"
        val result = Json.parse(input, config = config) as JsonArray
        assertEquals(5, result.size)
    }

    // --- Finding 7: Duplicate keys ---

    @Test
    fun `rejects duplicate keys by default`() {
        val input = """{"a":1,"b":2,"a":3}"""
        assertFailsWith<JsonParseException> {
            Json.parse(input)
        }.also {
            assertTrue(it.message!!.contains("Duplicate key"))
        }
    }

    @Test
    fun `allows duplicate keys when configured`() {
        val config = JsonParserConfig(rejectDuplicateKeys = false)
        val result = Json.parse("""{"a":1,"b":2,"a":3}""", config = config)
        assertEquals(3, result["a"].int) // last value wins
    }

    // --- Finding 4: Thread-safe JsonNull path ---

    @Test
    fun `JsonNull path is thread-local`() {
        val obj = Json.parse("""{"name":"Alice"}""") as JsonObject

        val paths = mutableListOf<String>()
        val threads = (0..9).map { i ->
            Thread {
                val key = "key$i"
                obj[key] // sets JsonNull._accessPath
                Thread.sleep(10) // give other threads a chance to interfere
                val path = JsonNull._accessPath
                synchronized(paths) {
                    paths.add(path.joinToString("."))
                }
            }
        }
        threads.forEach { it.start() }
        threads.forEach { it.join() }

        // Each thread should see its own key, not another thread's
        for (i in 0..9) {
            assertTrue("key$i" in paths, "Expected key$i in paths: $paths")
        }
    }
}
