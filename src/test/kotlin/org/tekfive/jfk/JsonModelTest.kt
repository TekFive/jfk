package org.tekfive.jfk

import kotlin.test.*

// Simple model
data class Rgb(val r: Int, val g: Int, val b: Int) {
    companion object : FromJsonObject<Rgb>
}

// Nullable fields
data class Bio(val name: String, val tagline: String?) {
    companion object : FromJsonObject<Bio>
}

// Nested model
data class Coord(val x: Double, val y: Double) {
    companion object : FromJsonObject<Coord>
}

data class Segment(val start: Coord, val end: Coord) {
    companion object : FromJsonObject<Segment>
}

// Optional nested
data class Marker(val label: String, val position: Coord?) {
    companion object : FromJsonObject<Marker>
}

// List of primitives
data class Numbers(val values: List<Int>) {
    companion object : FromJsonObject<Numbers>
}

// List of nested objects
data class Path(val points: List<Coord>) {
    companion object : FromJsonObject<Path>
}

// Default value
data class Settings(val theme: String = "light", val fontSize: Int = 14) {
    companion object : FromJsonObject<Settings>
}

// Lax coercion (default)
data class LaxConfig(val port: Int, val debug: Boolean) {
    companion object : FromJsonObject<LaxConfig>
}

// Strict (no coercion)
data class StrictConfig(val port: Int, val debug: Boolean) {
    companion object : FromJsonObject<StrictConfig> {
        override val lax: Boolean = false
    }
}

class JsonModelTest {

    // --- Reading ---

    @Test
    fun `read simple model`() {
        val rgb = """{"r":255,"g":128,"b":0}""".fromJson(Rgb)
        assertEquals(Rgb(255, 128, 0), rgb)
    }

    @Test
    fun `read returns null for missing field`() {
        assertNull("""{"r":255,"g":128}""".fromJson(Rgb))
    }

    @Test
    fun `read returns null for wrong type`() {
        assertNull("""{"r":"red","g":128,"b":0}""".fromJson(StrictConfig.takeIf { false } ?: Rgb))
        // Rgb uses lax=true, so "red" as laxInt returns null -> throws -> fromJson catches -> null
        assertNull("""{"r":"red","g":128,"b":0}""".fromJson(Rgb))
    }

    @Test
    fun `read nullable field present`() {
        val bio = """{"name":"Alice","tagline":"Hello world"}""".fromJson(Bio)
        assertEquals(Bio("Alice", "Hello world"), bio)
    }

    @Test
    fun `read nullable field missing`() {
        val bio = """{"name":"Alice"}""".fromJson(Bio)
        assertEquals(Bio("Alice", null), bio)
    }

    @Test
    fun `read nullable field explicit null`() {
        val bio = """{"name":"Alice","tagline":null}""".fromJson(Bio)
        assertEquals(Bio("Alice", null), bio)
    }

    @Test
    fun `read nested model`() {
        val seg = """{"start":{"x":0,"y":0},"end":{"x":10,"y":5}}""".fromJson(Segment)
        assertEquals(Segment(Coord(0.0, 0.0), Coord(10.0, 5.0)), seg)
    }

    @Test
    fun `read nested model missing returns null`() {
        assertNull("""{"start":{"x":0,"y":0}}""".fromJson(Segment))
    }

    @Test
    fun `read optional nested present`() {
        val m = """{"label":"A","position":{"x":1,"y":2}}""".fromJson(Marker)
        assertEquals(Marker("A", Coord(1.0, 2.0)), m)
    }

    @Test
    fun `read optional nested missing`() {
        val m = """{"label":"A"}""".fromJson(Marker)
        assertEquals(Marker("A", null), m)
    }

    @Test
    fun `read list of primitives`() {
        val n = """{"values":[1,2,3]}""".fromJson(Numbers)
        assertEquals(Numbers(listOf(1, 2, 3)), n)
    }

    @Test
    fun `read list of nested objects`() {
        val p = """{"points":[{"x":0,"y":0},{"x":1,"y":1}]}""".fromJson(Path)
        assertEquals(Path(listOf(Coord(0.0, 0.0), Coord(1.0, 1.0))), p)
    }

    // --- Default values ---

    @Test
    fun `default values used when properties missing`() {
        val s = """{}""".fromJson(Settings)
        assertEquals(Settings("light", 14), s)
    }

    @Test
    fun `default values overridden by json`() {
        val s = """{"theme":"dark","fontSize":18}""".fromJson(Settings)
        assertEquals(Settings("dark", 18), s)
    }

    @Test
    fun `partial defaults`() {
        val s = """{"theme":"dark"}""".fromJson(Settings)
        assertEquals(Settings("dark", 14), s)
    }

    // --- Lax coercion ---

    @Test
    fun `lax coercion from strings`() {
        val config = """{"port":"8080","debug":"true"}""".fromJson(LaxConfig)
        assertEquals(LaxConfig(8080, true), config)
    }

    @Test
    fun `lax coercion from native types`() {
        val config = """{"port":8080,"debug":true}""".fromJson(LaxConfig)
        assertEquals(LaxConfig(8080, true), config)
    }

    @Test
    fun `lax coercion fails for unconvertible`() {
        assertNull("""{"port":"abc","debug":"true"}""".fromJson(LaxConfig))
    }

    // --- Strict mode ---

    @Test
    fun `strict rejects string for int`() {
        assertNull("""{"port":"8080","debug":true}""".fromJson(StrictConfig))
    }

    @Test
    fun `strict accepts native types`() {
        val config = """{"port":8080,"debug":true}""".fromJson(StrictConfig)
        assertEquals(StrictConfig(8080, true), config)
    }

    // --- Exception details ---

    @Test
    fun `exception has path for missing required`() {
        val obj = Json.parse("""{"r":1,"g":2}""") as JsonObject
        val ex = assertFailsWith<JsonMappingException> { obj.fromJson(Rgb) }
        assertEquals("b", ex.path)
    }

    @Test
    fun `exception has path for wrong type`() {
        val obj = Json.parse("""{"r":1,"g":"bad","b":3}""") as JsonObject
        val ex = assertFailsWith<JsonMappingException> { obj.fromJson(StrictConfig.takeIf { false } ?: Rgb) }
        // Rgb is lax, "bad" -> laxInt returns null -> throws
        assertTrue(ex.path.isNotEmpty())
    }

    @Test
    fun `exception has path for nested array element`() {
        val obj = Json.parse("""{"values":[1,"two",3]}""") as JsonObject
        val ex = assertFailsWith<JsonMappingException> { obj.fromJson(Numbers) }
        assertEquals("values[1]", ex.path)
    }
}
