package org.tekfive.jfk

import kotlin.test.*

data class Server(val host: String, val port: Int) {
    companion object : FromJsonObject<Server>
}

data class Cluster(val name: String, val servers: List<Server>) {
    companion object : FromJsonObject<Cluster>
}

class JsonMappingExceptionTest {

    @Test
    fun `missing field reports path`() {
        val obj = Json.parse("""{"host":"localhost"}""") as JsonObject
        val ex = assertFailsWith<JsonMappingException> { obj.fromJson(Server) }
        assertEquals("port", ex.path)
        assertEquals("Int", ex.expected)
    }

    @Test
    fun `wrong type reports path and actual`() {
        val obj = Json.parse("""{"host":"localhost","port":true}""") as JsonObject
        // lax=true, so Boolean -> laxInt returns null -> throws
        val ex = assertFailsWith<JsonMappingException> { obj.fromJson(Server) }
        assertEquals("port", ex.path)
        assertTrue(ex.message!!.contains("JsonBool"))
    }

    @Test
    fun `nested list element failure reports indexed path`() {
        val json = """{"name":"prod","servers":[{"host":"a","port":1},{"host":"b"}]}"""
        val obj = Json.parse(json) as JsonObject
        val ex = assertFailsWith<JsonMappingException> { obj.fromJson(Cluster) }
        assertEquals("servers[1].port", ex.path)
    }

    @Test
    fun `list element wrong type reports indexed path`() {
        val obj = Json.parse("""{"name":"prod","servers":[{"host":"a","port":"bad"}]}""") as JsonObject
        val ex = assertFailsWith<JsonMappingException> { obj.fromJson(Cluster) }
        assertTrue(ex.path.startsWith("servers[0]"))
    }

    @Test
    fun `missing nested object field`() {
        val obj = Json.parse("""{"name":"prod","servers":[{"port":1}]}""") as JsonObject
        val ex = assertFailsWith<JsonMappingException> { obj.fromJson(Cluster) }
        assertEquals("servers[0].host", ex.path)
    }

    @Test
    fun `exception message is descriptive`() {
        val obj = Json.parse("""{}""") as JsonObject
        val ex = assertFailsWith<JsonMappingException> { obj.fromJson(Server) }
        assertTrue(ex.message!!.contains("host"))
        assertTrue(ex.message!!.contains("String"))
        assertTrue(ex.message!!.contains("JsonNull"))
    }

    @Test
    fun `fromJsonOrNull still returns null`() {
        val obj = Json.parse("""{"host":"localhost"}""") as JsonObject
        assertNull(obj.fromJsonOrNull(Server))
    }
}
