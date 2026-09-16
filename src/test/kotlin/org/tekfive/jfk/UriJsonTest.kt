package org.tekfive.jfk

import java.net.URI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UriJsonTest {
    @Test
    fun `uri member accepts absolute relative opaque and empty URIs`() {
        for (text in listOf("https://example.com/a%20b?q=value#fragment", "../path", "urn:example:item", "")) {
            val json = JsonObject("uri" to text)
            assertEquals(URI(text), json.uri("uri"))
        }
    }

    @Test
    fun `uri member returns null for missing invalid and non-string values`() {
        val json = Json.parse(
            """{"invalid":"https://example.com/a b","escape":"%zz","null":null,"number":42,"boolean":true,"array":[],"object":{}}""",
        ) as JsonObject

        for (name in json.keys + "missing") {
            assertNull(json.uri(name), name)
        }
    }
}
