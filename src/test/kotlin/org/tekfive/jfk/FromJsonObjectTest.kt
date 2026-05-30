package org.tekfive.jfk

import kotlin.test.*

data class Color(val r: Int, val g: Int, val b: Int) {
    companion object : FromJsonObject<Color>
}

data class Palette(val name: String, val colors: List<Color>) {
    companion object : FromJsonObject<Palette>
}

data class LabelListHolder(val name: String, val labels: List<String>) {
    companion object : FromJsonObject<LabelListHolder>
}

data class NullableLabelListHolder(val name: String, val labels: List<String>?) {
    companion object : FromJsonObject<NullableLabelListHolder>
}

data class NullableIntListHolder(val name: String, val values: List<Int?>) {
    companion object : FromJsonObject<NullableIntListHolder>
}

abstract class TestNodeMessage

interface TestNodeMessageType<T : TestNodeMessage> : FromJsonObject<T> {
    val id: String
}

data class TestTextNodeMessage(val body: String) : TestNodeMessage() {
    companion object : TestNodeMessageType<TestTextNodeMessage> {
        override val id: String = "text"
    }
}

enum class Size { SMALL, MEDIUM, LARGE }

data class Shirt(val label: String, val size: Size) {
    companion object : FromJsonObject<Shirt>
}

data class NicknameHolder(val name: String, val nickname: String?) {
    companion object : FromJsonObject<NicknameHolder>
}

data class TagHolder(val name: String, val tag: String = "default") {
    companion object : FromJsonObject<TagHolder>
}

class MutableWidget(
    val id: Long,
    var name: String,
    var description: String? = null,
    var count: Int = 0,
) {
    companion object : FromJsonObject<MutableWidget>
}

data class DynamicHolder(val name: String, val value: Any?) {
    companion object : FromJsonObject<DynamicHolder>
}

data class ContainerHolder(val name: String, val payload: JsonContainer?) {
    companion object : FromJsonObject<ContainerHolder>
}

data class TagSet(val name: String, val tags: Set<String>) {
    companion object : FromJsonObject<TagSet>
}

data class StringMapHolder(val name: String, val labels: Map<String, String>) {
    companion object : FromJsonObject<StringMapHolder>
}

data class IntMapHolder(val name: String, val counts: Map<String, Int>) {
    companion object : FromJsonObject<IntMapHolder>
}

data class NullableIntMapHolder(val name: String, val counts: Map<String, Int?>) {
    companion object : FromJsonObject<NullableIntMapHolder>
}

data class IntKeyMapHolder(val name: String, val counts: Map<Int, String>) {
    companion object : FromJsonObject<IntKeyMapHolder>
}

data class NestedMapHolder(val name: String, val colors: Map<String, Color>) {
    companion object : FromJsonObject<NestedMapHolder>
}

data class DoubleHolder(val value: Double) {
    companion object : FromJsonObject<DoubleHolder>
}

data class InvalidBase64Holder(val content: ByteArray) {
    companion object : FromJsonObject<InvalidBase64Holder>
}

data class ThrowingHolder(val name: String) {
    companion object : FromJsonObject<ThrowingHolder> {
        override fun fromJson(json: JsonObject): ThrowingHolder {
            throw IllegalStateException("boom")
        }
    }
}

class FromJsonObjectTest {

    @Test
    fun `fromJson parses valid object`() {
        val obj = Json.parse("""{"r":255,"g":128,"b":0}""") as JsonObject
        val color = obj.fromJson(Color)
        assertEquals(Color(255, 128, 0), color)
    }

    @Test
    fun `fromJson throws for missing required field`() {
        val obj = Json.parse("""{"r":255,"g":128}""") as JsonObject
        assertFailsWith<JsonMappingException> { obj.fromJson(Color) }
    }

    @Test
    fun `fromJsonOrNull returns null for missing field`() {
        val obj = Json.parse("""{"r":255,"g":128}""") as JsonObject
        assertNull(obj.fromJsonOrNull(Color))
    }

    @Test
    fun `fromJson throws for wrong type`() {
        val obj = Json.parse("""{"r":"red","g":128,"b":0}""") as JsonObject
        assertFailsWith<JsonMappingException> { obj.fromJson(Color) }
    }

    @Test
    fun `fromJson rejects fractional number for integer field`() {
        val obj = Json.parse("""{"r":1.9,"g":128,"b":0}""") as JsonObject
        assertFailsWith<JsonMappingException> { obj.fromJson(Color) }
    }

    @Test
    fun `fromJsonOptional only catches mapping failures`() {
        val obj = Json.parse("""{"name":"test"}""") as JsonObject
        assertFailsWith<IllegalStateException> {
            ThrowingHolder.fromJsonOptional(obj)
        }
    }

    @Test
    fun `invalid ByteArray base64 reports mapping exception`() {
        val obj = Json.parse("""{"content":"not base64!"}""") as JsonObject
        val error = assertFailsWith<JsonMappingException> {
            obj.fromJson(InvalidBase64Holder)
        }

        assertTrue(error.message!!.contains("content"))
    }

    @Test
    fun `nested fromJson`() {
        val input = """{"name":"sunset","colors":[{"r":255,"g":100,"b":0},{"r":200,"g":50,"b":50}]}"""
        val obj = Json.parse(input) as JsonObject
        val palette = obj.fromJson(Palette)
        assertEquals("sunset", palette.name)
        assertEquals(2, palette.colors.size)
        assertEquals(Color(255, 100, 0), palette.colors[0])
    }

    @Test
    fun `nested fromJson throws for invalid nested`() {
        val input = """{"name":"broken","colors":[{"r":255}]}"""
        val obj = Json.parse(input) as JsonObject
        assertFailsWith<JsonMappingException> { obj.fromJson(Palette) }
    }

    @Test
    fun `fromJson maps null to empty list for non-nullable primitive list`() {
        val result = """{"name":"test","labels":null}""".fromJsonOrThrow(LabelListHolder)
        assertEquals(emptyList(), result.labels)
    }

    @Test
    fun `fromJson maps null to empty list for non-nullable nested object list`() {
        val result = """{"name":"test","colors":null}""".fromJsonOrThrow(Palette)
        assertEquals(emptyList(), result.colors)
    }

    @Test
    fun `fromJson keeps null for nullable list`() {
        val result = """{"name":"test","labels":null}""".fromJsonOrThrow(NullableLabelListHolder)
        assertNull(result.labels)
    }

    @Test
    fun `fromJson rejects null element for non-nullable list element type`() {
        assertFailsWith<JsonMappingException> {
            """{"name":"test","labels":["a",null]}""".fromJsonOrThrow(LabelListHolder)
        }
    }

    @Test
    fun `fromJson allows null element for nullable list element type`() {
        val result = """{"name":"test","values":[1,null,3]}""".fromJsonOrThrow(NullableIntListHolder)
        assertEquals(listOf(1, null, 3), result.values)
    }

    @Test
    fun `fromJson still rejects missing non-nullable list`() {
        assertFailsWith<JsonMappingException> {
            """{"name":"test"}""".fromJsonOrThrow(LabelListHolder)
        }
    }

    @Test
    fun `fromJson resolves target type through parameterized FromJsonObject subinterface`() {
        val obj = Json.parse("""{"body":"hello"}""") as JsonObject
        val message = obj.fromJson(TestTextNodeMessage)
        assertEquals(TestTextNodeMessage("hello"), message)
    }

    // String extensions
    @Test
    fun `string fromJson`() {
        val color = """{"r":1,"g":2,"b":3}""".fromJson(Color)
        assertEquals(Color(1, 2, 3), color)
    }

    @Test
    fun `string fromJson returns null for invalid`() {
        assertNull("not json".fromJson(Color))
    }

    @Test
    fun `string fromJson returns null for missing field`() {
        assertNull("""{"r":1}""".fromJson(Color))
    }

    @Test
    fun `string fromJsonOrThrow`() {
        val color = """{"r":1,"g":2,"b":3}""".fromJsonOrThrow(Color)
        assertEquals(Color(1, 2, 3), color)
    }

    @Test
    fun `string fromJsonOrThrow throws for invalid`() {
        assertFailsWith<IllegalArgumentException> {
            "not json".fromJsonOrThrow(Color)
        }
    }

    @Test
    fun `string fromJsonOrThrow throws for missing field`() {
        assertFailsWith<JsonMappingException> {
            """{"r":1}""".fromJsonOrThrow(Color)
        }
    }

    @Test
    fun `enum property deserialized by name`() {
        val shirt = """{"label":"Classic","size":"MEDIUM"}""".fromJsonOrThrow(Shirt)
        assertEquals(Size.MEDIUM, shirt.size)
    }

    @Test
    fun `enum property throws for unknown value`() {
        assertFailsWith<JsonMappingException> {
            """{"label":"Classic","size":"XL"}""".fromJsonOrThrow(Shirt)
        }
    }

    @Test
    fun `enum property throws for non-string value`() {
        assertFailsWith<JsonMappingException> {
            """{"label":"Classic","size":42}""".fromJsonOrThrow(Shirt)
        }
    }

    @Test
    fun `lax Double rejects non-finite string values`() {
        assertFailsWith<JsonMappingException> {
            """{"value":"NaN"}""".fromJsonOrThrow(DoubleHolder)
        }
        assertFailsWith<JsonMappingException> {
            """{"value":"Infinity"}""".fromJsonOrThrow(DoubleHolder)
        }
    }

    // fromJson with onError block

    @Test
    fun `fromJson with onError calls block on missing property`() {
        val obj = Json.parse("""{"r":255,"g":128}""") as JsonObject
        var captured: JsonMappingException? = null
        assertFailsWith<IllegalArgumentException> {
            Color.fromJson(obj, false) { e ->
                captured = e
                throw IllegalArgumentException("missing: ${e.path}")
            }
        }
        assertEquals("b", captured?.path)
        assertTrue(captured?.actual is JsonNull)
    }

    @Test
    fun `fromJson with onError calls block on wrong type`() {
        val obj = Json.parse("""{"r":"red","g":128,"b":0}""") as JsonObject
        var captured: JsonMappingException? = null
        assertFailsWith<IllegalArgumentException> {
            Color.fromJson(obj, false) { e ->
                captured = e
                throw IllegalArgumentException("bad type: ${e.path}")
            }
        }
        assertEquals("r", captured?.path)
        assertTrue(captured?.actual is JsonString)
    }

    @Test
    fun `fromJson with onError returns result on valid input`() {
        val obj = Json.parse("""{"r":1,"g":2,"b":3}""") as JsonObject
        val color = Color.fromJson(obj, false) { e ->
            throw IllegalArgumentException("unexpected: ${e.path}")
        }
        assertEquals(Color(1, 2, 3), color)
    }

    // treatEmptyStringAsNull

    @Test
    fun `treatEmptyStringAsNull throws for required non-nullable String`() {
        val obj = Json.parse("""{"label":"","size":"MEDIUM"}""") as JsonObject
        assertFailsWith<JsonMappingException> {
            Shirt.fromJson(obj, treatEmptyStringAsNull = true)
        }
    }

    @Test
    fun `treatEmptyStringAsNull sets nullable field to null`() {
        val obj = Json.parse("""{"name":"test","nickname":""}""") as JsonObject
        val result = NicknameHolder.fromJson(obj, treatEmptyStringAsNull = true)
        assertEquals("test", result.name)
        assertNull(result.nickname)
    }

    @Test
    fun `treatEmptyStringAsNull uses default for optional field`() {
        val obj = Json.parse("""{"name":"test","tag":""}""") as JsonObject
        val result = TagHolder.fromJson(obj, treatEmptyStringAsNull = true)
        assertEquals("test", result.name)
        assertEquals("default", result.tag)
    }

    @Test
    fun `treatEmptyStringAsNull false allows empty strings through`() {
        val obj = Json.parse("""{"label":"","size":"MEDIUM"}""") as JsonObject
        val shirt = Shirt.fromJson(obj)
        assertEquals("", shirt.label)
    }

    // overrides

    @Test
    fun `override replaces JSON value`() {
        val obj = Json.parse("""{"r":1,"g":2,"b":3}""") as JsonObject
        val color = Color.fromJson(obj, false, Color::r to 99)
        assertEquals(99, color.r)
        assertEquals(2, color.g)
        assertEquals(3, color.b)
    }

    @Test
    fun `override supplies missing required value`() {
        val obj = Json.parse("""{"r":1,"g":2}""") as JsonObject
        val color = Color.fromJson(obj, false, Color::b to 42)
        assertEquals(42, color.b)
    }

    @Test
    fun `override with null on nullable field`() {
        val obj = Json.parse("""{"name":"test","nickname":"Alice"}""") as JsonObject
        val result = NicknameHolder.fromJson(obj, false, NicknameHolder::nickname to null)
        assertNull(result.nickname)
    }

    @Test
    fun `override combined with onError`() {
        val obj = Json.parse("""{"r":1,"g":2}""") as JsonObject
        val color = Color.fromJson(obj, false, Color::b to 10) { e ->
            throw IllegalArgumentException(e.path)
        }
        assertEquals(1, color.r)
        assertEquals(2, color.g)
        assertEquals(10, color.b)
    }

    @Test
    fun `multiple overrides`() {
        val obj = Json.parse("""{"r":1,"g":2,"b":3}""") as JsonObject
        val color = Color.fromJson(obj, false, Color::r to 10, Color::g to 20)
        assertEquals(10, color.r)
        assertEquals(20, color.g)
        assertEquals(3, color.b)
    }

    // applyJson

    @Test
    fun `applyJson updates var properties from JSON`() {
        val widget = MutableWidget(id = 1, name = "old", description = "old desc", count = 5)
        val json = Json.parse("""{"name":"new","count":10}""") as JsonObject
        MutableWidget.applyJson(widget, json)
        assertEquals("new", widget.name)
        assertEquals(10, widget.count)
        assertEquals("old desc", widget.description)
        assertEquals(1L, widget.id)
    }

    @Test
    fun `applyJson ignores val properties in JSON`() {
        val widget = MutableWidget(id = 1, name = "old")
        val json = Json.parse("""{"id":99,"name":"new"}""") as JsonObject
        MutableWidget.applyJson(widget, json)
        assertEquals(1L, widget.id)
        assertEquals("new", widget.name)
    }

    @Test
    fun `applyJson skips excluded properties`() {
        val widget = MutableWidget(id = 1, name = "old", count = 5)
        val json = Json.parse("""{"name":"new","count":10}""") as JsonObject
        MutableWidget.applyJson(widget, json, MutableWidget::name)
        assertEquals("old", widget.name)
        assertEquals(10, widget.count)
    }

    @Test
    fun `applyJson ignores properties missing from JSON`() {
        val widget = MutableWidget(id = 1, name = "old", description = "keep", count = 5)
        val json = Json.parse("""{"name":"new"}""") as JsonObject
        MutableWidget.applyJson(widget, json)
        assertEquals("new", widget.name)
        assertEquals("keep", widget.description)
        assertEquals(5, widget.count)
    }

    @Test
    fun `applyJson calls onError for wrong type`() {
        val widget = MutableWidget(id = 1, name = "old", count = 5)
        val json = Json.parse("""{"count":"not-a-number"}""") as JsonObject
        var captured: JsonMappingException? = null
        assertFailsWith<IllegalArgumentException> {
            MutableWidget.applyJson(widget, json) { e ->
                captured = e
                throw IllegalArgumentException(e.path)
            }
        }
        assertEquals("count", captured?.path)
        assertEquals(5, widget.count)
    }

    @Test
    fun `applyJson throws for wrong type without onError`() {
        val widget = MutableWidget(id = 1, name = "old", count = 5)
        val json = Json.parse("""{"count":"not-a-number"}""") as JsonObject
        assertFailsWith<JsonMappingException> {
            MutableWidget.applyJson(widget, json)
        }
    }

    @Test
    fun `applyJson ignores null JSON values`() {
        val widget = MutableWidget(id = 1, name = "old", description = "keep")
        val json = Json.parse("""{"description":null}""") as JsonObject
        MutableWidget.applyJson(widget, json)
        assertEquals("keep", widget.description)
    }

    // applyJsonOnly

    @Test
    fun `applyJsonOnly updates only included properties`() {
        val widget = MutableWidget(id = 1, name = "old", description = "old desc", count = 5)
        val json = Json.parse("""{"name":"new","count":10,"description":"new desc"}""") as JsonObject
        MutableWidget.applyJsonOnly(widget, json, MutableWidget::name)
        assertEquals("new", widget.name)
        assertEquals("old desc", widget.description)
        assertEquals(5, widget.count)
    }

    @Test
    fun `applyJsonOnly with multiple included properties`() {
        val widget = MutableWidget(id = 1, name = "old", description = "old desc", count = 5)
        val json = Json.parse("""{"name":"new","count":10,"description":"new desc"}""") as JsonObject
        MutableWidget.applyJsonOnly(widget, json, MutableWidget::name, MutableWidget::count)
        assertEquals("new", widget.name)
        assertEquals(10, widget.count)
        assertEquals("old desc", widget.description)
    }

    @Test
    fun `applyJsonOnly throws for val property`() {
        val widget = MutableWidget(id = 1, name = "old")
        val json = Json.parse("""{"id":99,"name":"new"}""") as JsonObject
        assertFailsWith<IllegalArgumentException> {
            MutableWidget.applyJsonOnly(widget, json, MutableWidget::id)
        }
    }

    @Test
    fun `applyJsonOnly ignores properties missing from JSON`() {
        val widget = MutableWidget(id = 1, name = "old", count = 5)
        val json = Json.parse("""{"name":"new"}""") as JsonObject
        MutableWidget.applyJsonOnly(widget, json, MutableWidget::name, MutableWidget::count)
        assertEquals("new", widget.name)
        assertEquals(5, widget.count)
    }

    @Test
    fun `applyJsonOnly calls onError for wrong type`() {
        val widget = MutableWidget(id = 1, name = "old", count = 5)
        val json = Json.parse("""{"count":"not-a-number"}""") as JsonObject
        var captured: JsonMappingException? = null
        assertFailsWith<IllegalArgumentException> {
            MutableWidget.applyJsonOnly(widget, json, MutableWidget::count) { e ->
                captured = e
                throw IllegalArgumentException(e.path)
            }
        }
        assertEquals("count", captured?.path)
        assertEquals(5, widget.count)
    }

    @Test
    fun `applyJsonOnly ignores null JSON values`() {
        val widget = MutableWidget(id = 1, name = "old", description = "keep")
        val json = Json.parse("""{"description":null}""") as JsonObject
        MutableWidget.applyJsonOnly(widget, json, MutableWidget::description)
        assertEquals("keep", widget.description)
    }

    // Any? property

    @Test
    fun `fromJson maps string value to Any property`() {
        val result = """{"name":"test","value":"hello"}""".fromJsonOrThrow(DynamicHolder)
        assertEquals("hello", result.value)
    }

    @Test
    fun `fromJson maps number value to Any property`() {
        val result = """{"name":"test","value":42}""".fromJsonOrThrow(DynamicHolder)
        assertEquals(42, result.value)
    }

    @Test
    fun `fromJson maps boolean value to Any property`() {
        val result = """{"name":"test","value":true}""".fromJsonOrThrow(DynamicHolder)
        assertEquals(true, result.value)
    }

    @Test
    fun `fromJson maps null to Any property`() {
        val result = """{"name":"test","value":null}""".fromJsonOrThrow(DynamicHolder)
        assertNull(result.value)
    }

    @Test
    fun `fromJson maps object to Any property`() {
        val result = """{"name":"test","value":{"a":1}}""".fromJsonOrThrow(DynamicHolder)
        assertEquals(mapOf("a" to 1), result.value)
    }

    @Test
    fun `fromJson maps array to Any property`() {
        val result = """{"name":"test","value":[1,2,3]}""".fromJsonOrThrow(DynamicHolder)
        assertEquals(listOf(1, 2, 3), result.value)
    }

    @Test
    fun `fromJson maps missing value to null for Any property`() {
        val result = """{"name":"test"}""".fromJsonOrThrow(DynamicHolder)
        assertNull(result.value)
    }

    // JsonContainer? property

    @Test
    fun `fromJson maps object value to JsonContainer property`() {
        val result = """{"name":"test","payload":{"a":1}}""".fromJsonOrThrow(ContainerHolder)
        val payload = result.payload as JsonObject
        assertEquals(1, payload["a"].reqInt)
    }

    @Test
    fun `fromJson maps array value to JsonContainer property`() {
        val result = """{"name":"test","payload":[1,2,3]}""".fromJsonOrThrow(ContainerHolder)
        val payload = result.payload as JsonArray
        assertEquals(listOf(1, 2, 3), payload.toList())
    }

    @Test
    fun `fromJson rejects primitive value for JsonContainer property`() {
        assertFailsWith<JsonMappingException> {
            """{"name":"test","payload":"not-a-container"}""".fromJsonOrThrow(ContainerHolder)
        }
    }

    // Set property

    @Test
    fun `fromJson deserializes Set from JSON array`() {
        val result = """{"name":"test","tags":["a","b","c"]}""".fromJsonOrThrow(TagSet)
        assertEquals(setOf("a", "b", "c"), result.tags)
    }

    @Test
    fun `fromJson Set deduplicates values`() {
        val result = """{"name":"test","tags":["a","b","a"]}""".fromJsonOrThrow(TagSet)
        assertEquals(setOf("a", "b"), result.tags)
    }

    @Test
    fun `fromJson Set from empty array`() {
        val result = """{"name":"test","tags":[]}""".fromJsonOrThrow(TagSet)
        assertEquals(emptySet(), result.tags)
    }

    // Map property

    @Test
    fun `fromJson deserializes Map with String values`() {
        val result = """{"name":"test","labels":{"env":"prod","region":"us-east"}}""".fromJsonOrThrow(StringMapHolder)
        assertEquals(mapOf("env" to "prod", "region" to "us-east"), result.labels)
    }

    @Test
    fun `fromJson deserializes Map with Int values`() {
        val result = """{"name":"test","counts":{"a":1,"b":2}}""".fromJsonOrThrow(IntMapHolder)
        assertEquals(mapOf("a" to 1, "b" to 2), result.counts)
    }

    @Test
    fun `fromJson rejects null map value for non-nullable value type`() {
        assertFailsWith<JsonMappingException> {
            """{"name":"test","counts":{"a":1,"b":null}}""".fromJsonOrThrow(IntMapHolder)
        }
    }

    @Test
    fun `fromJson allows null map value for nullable value type`() {
        val result = """{"name":"test","counts":{"a":1,"b":null}}""".fromJsonOrThrow(NullableIntMapHolder)
        assertEquals(mapOf("a" to 1, "b" to null), result.counts)
    }

    @Test
    fun `fromJson rejects maps with non-string key type`() {
        assertFailsWith<JsonMappingException> {
            """{"name":"test","counts":{"1":"one"}}""".fromJsonOrThrow(IntKeyMapHolder)
        }
    }

    @Test
    fun `fromJson deserializes Map with nested object values`() {
        val result = """{"name":"test","colors":{"red":{"r":255,"g":0,"b":0},"blue":{"r":0,"g":0,"b":255}}}""".fromJsonOrThrow(NestedMapHolder)
        assertEquals(Color(255, 0, 0), result.colors["red"])
        assertEquals(Color(0, 0, 255), result.colors["blue"])
    }

    @Test
    fun `fromJson deserializes empty Map`() {
        val result = """{"name":"test","labels":{}}""".fromJsonOrThrow(StringMapHolder)
        assertEquals(emptyMap(), result.labels)
    }
}
