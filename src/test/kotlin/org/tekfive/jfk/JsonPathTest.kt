package org.tekfive.jfk

import kotlin.random.Random
import kotlin.test.*

class JsonPathTest {
    @Test
    fun `root is an empty path`() {
        assertSame(JsonPath.Root, JsonPath.parse("$"))
        assertEquals(emptyList(), JsonPath.Root.segments)
        assertEquals("$", JsonPath.Root.toString())
        assertEquals(JsonPath(emptyList()), JsonPath.Root)
    }

    @Test
    fun `mixed segments parse to typed keys and indices`() {
        val path = JsonPath.parse("$.users[0].matrix[1][3]._name2")
        assertEquals(listOf(
            JsonPathSegment.Key("users"), JsonPathSegment.Index(0),
            JsonPathSegment.Key("matrix"), JsonPathSegment.Index(1),
            JsonPathSegment.Index(3), JsonPathSegment.Key("_name2"),
        ), path.segments)
        assertEquals("$.users[0].matrix[1][3]._name2", path.toString())
    }

    @Test
    fun `quoted identifiers normalize and compare by value`() {
        val path = JsonPath.parse("$[\"users\"][0][\"name\"]")
        val built = JsonPath.Root.key("users").index(0).key("name")
        assertEquals(built, path)
        assertEquals(built.hashCode(), path.hashCode())
        assertEquals("$.users[0].name", path.toString())
        assertEquals(1, setOf(path, built).size)
        assertNotEquals(JsonPath.Root.key("0"), JsonPath.Root.index(0))
        assertNotEquals(JsonPath.Root.key("a.b"), JsonPath.Root.key("a").key("b"))
    }

    @Test
    fun `quoted keys support punctuation empty names whitespace and unicode`() {
        val keys = listOf("a.b", "x[y]", "0", "", "hello world", "é", "名字", "😀", "$", "*")
        for (key in keys) {
            val path = JsonPath.Root.key(key)
            assertEquals("$[${JsonString(key).toJsonString()}]", path.toString())
            assertEquals(path, JsonPath.parse(path.toString()))
        }
    }

    @Test
    fun `all JSON escapes decode including unicode surrogate pairs`() {
        val input = """$["\"\\\/\b\f\n\r\t\u0041\u00e9\uD83D\uDE00"]"""
        val key = "\"\\/\b\u000C\n\r\tAé😀"
        assertEquals(JsonPath.Root.key(key), JsonPath.parse(input))
        assertEquals(JsonPath.Root.key("name"), JsonPath.parse("""$["\u006eame"]"""))
    }

    @Test
    fun `canonical paths round trip arbitrary UTF-16 keys`() {
        val random = Random(731)
        val keys = listOf((0..31).map(Int::toChar).joinToString(""), "\uD800", "\uDC00") +
            List(200) { CharArray(random.nextInt(0, 60)) { random.nextInt(65536).toChar() }.concatToString() }
        for (key in keys) {
            val path = JsonPath.Root.key(key).index(17).key("tail")
            val canonical = path.toString()
            assertEquals(path, JsonPath.parse(canonical))
            assertEquals(canonical, JsonPath.parse(canonical).toString())
        }
    }

    @Test
    fun `index bounds are enforced in parsing and construction`() {
        assertEquals(JsonPath.Root.index(Int.MAX_VALUE), JsonPath.parse("$[2147483647]"))
        assertFailsWith<IllegalArgumentException> { JsonPathSegment.Index(-1) }
        assertFailsWith<IllegalArgumentException> { JsonPath.Root.index(Int.MIN_VALUE) }
        assertFailsWith<JsonPathParseException> { JsonPath.parse("$[2147483648]") }
        assertFailsWith<JsonPathParseException> { JsonPath.parse("$[99999999999999999999]") }
    }

    @Test
    fun `paths copy input and expose unmodifiable segments`() {
        val input = mutableListOf<JsonPathSegment>(JsonPathSegment.Key("users"))
        val path = JsonPath(input)
        val hash = path.hashCode()
        input.clear()
        assertEquals("$.users", path.toString())
        assertEquals(hash, path.hashCode())
        assertFailsWith<UnsupportedOperationException> {
            (path.segments as MutableList<JsonPathSegment>).clear()
        }
        val child = path.index(0).key("name")
        assertEquals("$.users", path.toString())
        assertEquals("$.users[0].name", child.toString())
        assertEquals("$", JsonPath.Root.toString())
    }

    @Test
    fun `invalid syntax reports the exact offending offset`() {
        val invalid = listOf(
            "" to 0,
            "users" to 0,
            " $.users" to 0,
            "$." to 2,
            "$.0" to 2,
            "$.é" to 2,
            "$.a..b" to 4,
            "$.a b" to 3,
            "$[" to 2,
            "$[]" to 2,
            "$[-1]" to 2,
            "$[+1]" to 2,
            "$[01]" to 3,
            "$[00]" to 3,
            "$[1.0]" to 3,
            "$[1e2]" to 3,
            "$[１]" to 2,
            "$[1" to 3,
            "$[1 ]" to 3,
            "$[ 1]" to 2,
            "$['key']" to 2,
            "$[key]" to 2,
            "$[*]" to 2,
            "$[0:2]" to 3,
            "$[0,1]" to 3,
            "$..name" to 2,
            "$.a*" to 3,
            "$[0]name" to 4,
            "$[0]]" to 4,
            "$ " to 1,
            "$[2147483648]" to 11,
            "$[\"x\"" to 5,
            "$[\"x\" ]" to 5,
            "$[\"x" to 4,
            "$[\"\\" to 4,
            "$[\"\\q\"]" to 4,
            "$[\"\\u12" to 7,
            "$[\"\\u12xz\"]" to 7,
            "$[\"\\u１２３４\"]" to 5,
            "$[\"a\nb\"]" to 4,
        )
        for ((input, offset) in invalid) {
            val error = assertFailsWith<JsonPathParseException>(input) { JsonPath.parse(input) }
            assertEquals(offset, error.offset, input)
            assertTrue(error.message.orEmpty().contains("offset $offset"), input)
        }
    }

    @Test
    fun `unescaped control characters are rejected`() {
        for (code in 0..31) {
            val error = assertFailsWith<JsonPathParseException> {
                JsonPath.parse("$[\"${code.toChar()}\"]")
            }
            assertEquals(3, error.offset)
        }
    }

    @Test
    fun `deep paths parse and format without recursive traversal`() {
        val input = "$" + ".a[0]".repeat(2000)
        val path = JsonPath.parse(input)
        assertEquals(4000, path.segments.size)
        assertEquals(input, path.toString())
    }

    @Test
    fun `traversal selects exact nested array elements`() {
        val root: JsonContainer = JsonObject(mapOf(
            "users" to listOf(mapOf("name" to "Alice"), mapOf("name" to "Bob")),
            "matrix" to listOf(listOf(1, 2), listOf(3, 4)),
        ))
        assertEquals("Bob", root.at(JsonPath.parse("$.users[1].name")).reqString)
        assertEquals(3, root.at(JsonPath.parse("$.matrix[1][0]")).reqInt)
        assertSame(root["users"], root.at(JsonPath.parse("$.users")))
    }

    @Test
    fun `root and subtree paths are relative to the supplied value`() {
        val array: JsonContainer = JsonArray(listOf(mapOf("id" to 42)))
        assertSame(array, array.at(JsonPath.Root))
        assertEquals(42, array.at(JsonPath.parse("$[0].id")).reqInt)
        val subtree = array[0]
        assertSame(subtree, subtree.at(JsonPath.Root))
        assertEquals(42, subtree.at(JsonPath.parse("$.id")).reqInt)
        val scalar = JsonNumber(7)
        assertSame(scalar, scalar.at(JsonPath.Root))
        assertSame(JsonNull, JsonNull.at(JsonPath.Root))
    }

    @Test
    fun `numeric keys and indices never coerce to each other`() {
        val obj = JsonObject(mapOf("0" to "key"))
        val array = JsonArray(listOf("element"))
        assertEquals("key", obj.at(JsonPath.parse("$[\"0\"]")).reqString)
        assertEquals("element", array.at(JsonPath.parse("$[0]")).reqString)
        assertSame(JsonNull, obj.at(JsonPath.parse("$[0]")))
        assertSame(JsonNull, array.at(JsonPath.parse("$[\"0\"]")))
    }

    @Test
    fun `quoted keys resolve literally`() {
        val obj = JsonObject(mapOf("a.b" to mapOf("x[y]" to mapOf("" to 42))))
        assertEquals(42, obj.at(JsonPath.parse("$[\"a.b\"][\"x[y]\"][\"\"]")).reqInt)
        val key = "quote\"slash\\newline\n名字"
        assertEquals(true, JsonObject(mapOf(key to true)).at(JsonPath.Root.key(key)).reqBoolean)
    }

    @Test
    fun `missing values nulls and type mismatches return JsonNull`() {
        val root = JsonObject(mapOf(
            "users" to listOf(mapOf("name" to "Alice")),
            "nil" to null, "scalar" to 42, "empty" to emptyList<Any>(),
        ))
        val paths = listOf(
            "$.missing", "$.missing[0].deep", "$.nil", "$.nil[0].deep",
            "$.users[1]", "$.users[2147483647].name", "$.users[0].missing",
            "$.users.name", "$.users[0][0]", "$.scalar.name", "$.scalar[0]", "$.empty[0]",
        )
        for (path in paths) assertSame(JsonNull, root.at(JsonPath.parse(path)), path)
    }

    @Test
    fun `required accessors keep chained lookup error context`() {
        val root = JsonObject(mapOf("users" to listOf(mapOf("name" to "Alice"))))
        val chained = assertFailsWith<IllegalStateException> { root["users"][1]["name"].reqString }
        val path = assertFailsWith<IllegalStateException> {
            root.at(JsonPath.parse("$.users[1].name")).reqString
        }
        assertEquals(chained.message, path.message)
    }

    @Test
    fun `legacy string paths and literal key access are unchanged`() {
        val root = JsonObject(mapOf(
            "a" to mapOf("b" to 1), "users[0]" to mapOf("name" to "literal"),
            "$" to 2, "" to 3, "a.b" to 4,
        ))
        assertEquals(1, root.at("a.b").reqInt)
        assertEquals("literal", root.at("users[0].name").reqString)
        assertEquals(2, root.at("$").reqInt)
        assertEquals(3, root.at("").reqInt)
        assertEquals(4, root["a.b"].reqInt)
        assertSame(JsonNull, root.at("a.missing"))
    }
}
