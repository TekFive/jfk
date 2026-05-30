package org.tekfive.jfk

import kotlin.test.*

// Custom fromJson override for non-standard mapping
data class CustomPoint(val x: Double, val y: Double) {
    companion object : FromJsonObject<CustomPoint> {
        override fun fromJson(json: JsonObject, treatEmptyStringAsNull: Boolean, vararg overrides: Pair<kotlin.reflect.KProperty<*>, Any?>): CustomPoint {
            return CustomPoint(
                x = json["px"].double ?: throw JsonMappingException("px", "Double", json["px"]),
                y = json["py"].double ?: throw JsonMappingException("py", "Double", json["py"]),
            )
        }
    }
}

// Test models for default reflection-based FromJson
data class Vec2(val x: Double, val y: Double) {
    companion object : FromJsonObject<Vec2>
}

data class Edge(val start: Vec2, val end: Vec2) {
    companion object : FromJsonObject<Edge>
}

data class Figure(val name: String, val origin: Vec2?) {
    companion object : FromJsonObject<Figure>
}

data class Mesh(val vertices: List<Vec2>) {
    companion object : FromJsonObject<Mesh>
}

abstract class NamedEnumType<E : Enum<E>> : FromJsonObject<E> {
    abstract val values: Array<E>

    override fun fromJson(json: JsonObject): E {
        val name = json["name"].reqString
        return values.firstOrNull { it.name == name }
            ?: throw JsonMappingException("name", "one of ${values.map { it.name }}", json["name"])
    }
}

enum class NamedMode {
    STATIC,
    EXPRESSION,
    ;

    companion object : NamedEnumType<NamedMode>() {
        override val values: Array<NamedMode> = entries.toTypedArray()
    }
}

data class NamedModeHolder(val mode: NamedMode) {
    companion object : FromJsonObject<NamedModeHolder>
}

class JsonMappingTest {

    @Test
    fun `custom fromJson override`() {
        val p = """{"px":1.5,"py":2.5}""".fromJson(CustomPoint)
        assertEquals(CustomPoint(1.5, 2.5), p)
    }

    @Test
    fun `custom fromJson override fails for missing`() {
        assertNull("""{"px":1.5}""".fromJson(CustomPoint))
    }

    @Test
    fun `default fromJson with reflection`() {
        val obj = """{"x":1.5,"y":2.5}""".asRequiredJsonObject()
        val p = obj.fromJson(Vec2)
        assertEquals(Vec2(1.5, 2.5), p)
    }

    @Test
    fun `default fromJson nested`() {
        val obj = """{"start":{"x":0,"y":0},"end":{"x":10,"y":5}}""".asRequiredJsonObject()
        val edge = obj.fromJson(Edge)
        assertEquals(Edge(Vec2(0.0, 0.0), Vec2(10.0, 5.0)), edge)
    }

    @Test
    fun `default fromJson optional nested`() {
        val obj = """{"name":"circle"}""".asRequiredJsonObject()
        val figure = obj.fromJson(Figure)
        assertEquals(Figure("circle", null), figure)
    }

    @Test
    fun `default fromJson list of objects`() {
        val input = """{"vertices":[{"x":0,"y":0},{"x":1,"y":0},{"x":0,"y":1}]}"""
        val obj = input.asRequiredJsonObject()
        val mesh = obj.fromJson(Mesh)
        assertEquals(3, mesh.vertices.size)
        assertEquals(Vec2(1.0, 0.0), mesh.vertices[1])
    }

    @Test
    fun `inherited custom fromJson override on companion is used`() {
        val obj = """{"mode":{"name":"EXPRESSION"}}""".asRequiredJsonObject()
        val holder = obj.fromJson(NamedModeHolder)
        assertEquals(NamedMode.EXPRESSION, holder.mode)
    }
}
