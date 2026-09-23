package org.tekfive.jfk

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

data class PathRecord(val path: JsonPath, val alternatives: List<JsonPath>) : ToJsonObject {
    companion object : FromJsonObject<PathRecord>
}

class JsonPathMappingTest {
    @Test
    fun `paths serialize as typed segments and deserialize through their companion`() {
        val path = JsonPath.Root.key("users").index(0).key("name")
        val text = """{"segments":[{"name":"users"},{"index":0},{"name":"name"}]}"""
        val serializable: ToJsonObject = path
        val deserializer: FromJsonObject<JsonPath> = JsonPath

        assertEquals(text, serializable.toJsonString())
        assertEquals(path, text.fromJsonOrThrow(deserializer))
        assertEquals(path, deserializer.fromJson(path.toJsonObject()))
        assertEquals("""{"segments":[]}""", JsonPath.Root.toJsonString())
        assertEquals(JsonPath.Root, JsonPath.fromJson(JsonPath.Root.toJsonObject()))
    }

    @Test
    fun `round trip preserves empty special numeric and unicode keys and index bounds`() {
        val path = JsonPath.Root.key("").key("0").index(0).key("a.b[0]")
            .key("quote\"slash\\newline\n名字").index(Int.MAX_VALUE)

        assertEquals(path, path.toJsonString().fromJsonOrThrow(JsonPath))
        assertEquals(path, JsonPath.fromJson(path.toJsonObject(), treatEmptyStringAsNull = true))
    }

    @Test
    fun `paths map as nested properties and collection elements`() {
        val record = PathRecord(JsonPath.Root.key("items").index(2), listOf(JsonPath.Root, JsonPath.Root.key("0")))

        assertEquals(record, record.toJsonString().fromJsonOrThrow(PathRecord))
        assertEquals(record.path.toJsonObject(), JsonObject("path" to record.path)["path"])
    }

    @Test
    fun `invalid segments fail mapping instead of constructing an invalid path`() {
        val invalid = listOf(
            "{}",
            """{"name":"key","index":0}""",
            """{"name":null}""",
            """{"name":42}""",
            """{"index":null}""",
            """{"index":-1}""",
            """{"index":1.5}""",
            """{"index":2147483648}""",
            """{"index":"0"}""",
            "null",
            "true",
            "[]",
        )
        for (segment in invalid) {
            val json = """{"segments":[$segment]}""".asRequiredJsonObject()
            assertFailsWith<JsonMappingException>(segment) { JsonPath.fromJson(json) }
            assertNull(JsonPath.fromJsonOptional(json), segment)
        }
    }

    @Test
    fun `mapped paths retain immutable segments and support constructor overrides`() {
        val segments = mutableListOf<JsonPathSegment>(JsonPathSegment.Key("original"))
        val path = JsonPath.fromJson(JsonObject(), false, JsonPath::segments to segments)
        segments.clear()

        assertEquals(JsonPath.Root.key("original"), path)
        assertFailsWith<UnsupportedOperationException> {
            (path.segments as MutableList<JsonPathSegment>).clear()
        }
    }
}
