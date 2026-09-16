package org.tekfive.jfk

import java.net.URI
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JsonObjectConstructionTest {
    @Test
    fun `pair constructor converts values including instants and URIs`() {
        val instant = Instant.parse("2026-08-20T14:15:16.123456789Z")
        val uri = URI("https://example.com/path?q=value#fragment")
        val json = JsonObject(
            "time" to instant,
            "link" to uri,
            "count" to 42,
            "active" to true,
            "nested" to mapOf("items" to listOf(1, 2)),
        )

        assertEquals(instant, json.instant("time"))
        assertEquals(uri, json.uri("link"))
        assertEquals(42L, json.long("count"))
        assertEquals(true, json.boolean("active"))
        assertEquals(2, json["nested"]["items"][1].int)
    }

    @Test
    fun `pair constructor skips nulls and preserves explicit JSON null`() {
        val json = JsonObject("omitted" to null, "explicit" to JsonNull)

        assertFalse(json.containsKey("omitted"))
        assertTrue(json.containsKey("explicit"))
        assertEquals("{\"explicit\":null}", json.toJsonString())
    }

    @Test
    fun `duplicate pairs keep last non-null value and insertion order`() {
        val json = JsonObject("a" to 1, "b" to 2, "a" to 3, "a" to null)

        assertEquals("{\"a\":3,\"b\":2}", json.toJsonString())
    }

    @Test
    fun `empty and map constructors remain available`() {
        assertEquals(0, JsonObject().size)
        assertEquals(0, JsonObject(values = emptyArray()).size)
        assertEquals("{\"retained\":null}", JsonObject(mapOf("retained" to null)).toJsonString())
    }
}
