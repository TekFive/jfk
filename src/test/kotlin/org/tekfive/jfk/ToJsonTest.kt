package org.tekfive.jfk

import kotlin.test.*

data class Address(val city: String, val zip: String) : ToJsonObject

data class Person(val name: String, val age: Int, val address: Address) : ToJsonObject

data class AccountSummary(val id: Long, val name: String, val email: String, val active: Boolean) : ToJsonObject

data class OtherSummary(val id: Long, val name: String) : ToJsonObject

// Additional properties
data class Greeting(val name: String) : ToJsonObject {
    override fun additionalJsonValues(): Map<String, JsonValue> = mapOf(
        "greeting" to JsonString("Hello, $name!"),
        "nameLength" to JsonNumber(name.length),
    )
}

// Non-ToJson property is skipped
data class WithOpaque(val name: String, val opaque: Regex) : ToJsonObject

// Nullable property
data class Optional(val name: String, val nickname: String?) : ToJsonObject

// Nullable property with nulls included
data class OptionalIncludeNulls(val name: String, val nickname: String?) : ToJsonObject {
    override val includeNullProperties: Boolean get() = true
}

// List of primitives
data class IntBag(val values: List<Int>) : ToJsonObject

// List of ToJson objects
data class People(val members: List<Person>) : ToJsonObject

enum class Priority { LOW, MEDIUM, HIGH }

data class Task(val title: String, val priority: Priority) : ToJsonObject

data class BinaryData(val name: String, val content: ByteArray) : ToJsonObject {
    companion object : FromJsonObject<BinaryData>
}

data class StringSet(val name: String, val tags: Set<String>) : ToJsonObject

data class StringMapOutput(val name: String, val labels: Map<String, String>) : ToJsonObject

class ToJsonTest {

    @Test
    fun `simple toJson`() {
        val addr = Address("Austin", "78701")
        val json = addr.toJsonObject()
        assertEquals("Austin", json["city"].string)
        assertEquals("78701", json["zip"].string)
    }

    @Test
    fun `toJson with included properties serializes only those properties`() {
        val account = AccountSummary(7, "Alice", "alice@example.test", active = true)
        val json = account.toJsonObject(AccountSummary::id, AccountSummary::email)

        assertEquals(2, json.size)
        assertEquals(7, json["id"].long)
        assertEquals("alice@example.test", json["email"].string)
        assertFalse(json.containsKey("name"))
        assertFalse(json.containsKey("active"))
    }

    @Test
    fun `toJson excluding properties serializes default properties except those properties`() {
        val account = AccountSummary(7, "Alice", "alice@example.test", active = true)
        val json = account.toJsonObjectExcluding(AccountSummary::email, AccountSummary::active)

        assertEquals(2, json.size)
        assertEquals(7, json["id"].long)
        assertEquals("Alice", json["name"].string)
        assertFalse(json.containsKey("email"))
        assertFalse(json.containsKey("active"))
    }

    @Test
    fun `toJson property filters reject properties from another class`() {
        val account = AccountSummary(7, "Alice", "alice@example.test", active = true)

        assertFailsWith<IllegalArgumentException> {
            account.toJsonObject(OtherSummary::name)
        }
        assertFailsWith<IllegalArgumentException> {
            account.toJsonObjectExcluding(OtherSummary::name)
        }
    }

    @Test
    fun `nested toJson`() {
        val person = Person("Alice", 30, Address("Austin", "78701"))
        val json = person.toJsonObject()
        assertEquals("Alice", json["name"].string)
        assertEquals(30, json["age"].int)
        assertEquals("Austin", json["address"]["city"].string)
    }

    @Test
    fun `toJson compact string`() {
        val addr = Address("Austin", "78701")
        assertEquals("""{"city":"Austin","zip":"78701"}""", addr.toJsonObject().toJsonString())
    }

    @Test
    fun `additional properties`() {
        val g = Greeting("Alice")
        val json = g.toJsonObject()
        assertEquals("Alice", json["name"].string)
        assertEquals("Hello, Alice!", json["greeting"].string)
        assertEquals(5, json["nameLength"].int)
    }

    @Test
    fun `non-ToJson property skipped`() {
        val w = WithOpaque("test", Regex(".*"))
        val json = w.toJsonObject()
        assertEquals("test", json["name"].string)
        assertTrue(json["opaque"].isNull) // skipped
        assertEquals(1, json.size)
    }

    @Test
    fun `nullable property present`() {
        val o = Optional("Alice", "Ali")
        val json = o.toJsonObject()
        assertEquals("Ali", json["nickname"].string)
    }

    @Test
    fun `nullable property null`() {
        val o = Optional("Alice", null)
        val json = o.toJsonObject()
        assertEquals(1, json.size) // null property omitted by default
        assertFalse(json.containsKey("nickname"))
    }

    @Test
    fun `nullable property null with includeNullProperties`() {
        val o = OptionalIncludeNulls("Alice", null)
        val json = o.toJsonObject()
        assertTrue(json["nickname"].isNull)
        assertEquals(2, json.size) // both present, one is null
    }

    @Test
    fun `list of primitives`() {
        val bag = IntBag(listOf(1, 2, 3))
        val json = bag.toJsonObject()
        assertEquals("[1,2,3]", json["values"].toJsonString())
    }

    @Test
    fun `list of ToJson objects`() {
        val people = People(listOf(
            Person("Alice", 30, Address("Austin", "78701")),
            Person("Bob", 25, Address("Dallas", "75201")),
        ))
        val json = people.toJsonObject()
        assertEquals("Alice", json["members"][0]["name"].string)
        assertEquals("Dallas", json["members"][1]["address"]["city"].string)
    }

    @Test
    fun `set ToJson on object`() {
        val addr = Address("Austin", "78701")
        val obj = json { "name" set "Alice" }
        obj["address"] = addr
        assertEquals("Austin", obj["address"]["city"].string)
    }

    @Test
    fun `set ToJson on array by index`() {
        val arr = jsonArray("placeholder")
        arr[0] = Address("Austin", "78701")
        assertEquals("Austin", arr[0]["city"].string)
    }

    @Test
    fun `add ToJson to array`() {
        val arr = jsonArray()
        arr.add(Address("Austin", "78701"))
        arr.add(Address("Dallas", "75201"))
        assertEquals("Austin", arr[0]["city"].string)
        assertEquals("Dallas", arr[1]["city"].string)
    }

    @Test
    fun `ToJson in DSL builder set`() {
        val obj = json {
            "person" set Person("Alice", 30, Address("Austin", "78701"))
        }
        assertEquals("Alice", obj["person"]["name"].string)
        assertEquals(30, obj["person"]["age"].int)
        assertEquals("Austin", obj["person"]["address"]["city"].string)
    }

    @Test
    fun `ToJson in DSL array builder add`() {
        val arr = jsonArray {
            add(Address("Austin", "78701"))
            add(Address("Dallas", "75201"))
        }
        assertEquals("78701", arr[0]["zip"].string)
        assertEquals("75201", arr[1]["zip"].string)
    }

    @Test
    fun `ToJson in jsonArray varargs`() {
        val arr = jsonArray(Address("Austin", "78701"), Address("Dallas", "75201"))
        assertEquals("Austin", arr[0]["city"].string)
        assertEquals("Dallas", arr[1]["city"].string)
    }

    @Test
    fun `enum property serialized by name`() {
        val task = Task("Fix bug", Priority.HIGH)
        val json = task.toJsonObject()
        assertEquals("HIGH", json["priority"].string)
    }

    @Test
    fun `roundtrip toJson to string to parse`() {
        val person = Person("Bob", 25, Address("Dallas", "75201"))
        val jsonStr = person.toJsonObject().toJsonString()
        val parsed = Json.parse(jsonStr)
        assertEquals("Bob", parsed["name"].string)
        assertEquals(25, parsed["age"].int)
        assertEquals("Dallas", parsed["address"]["city"].string)
    }

    @Test
    fun `ByteArray serialized as Base64 string`() {
        val data = BinaryData("test", byteArrayOf(0, 1, 2, 3, 255.toByte()))
        val json = data.toJsonObject()
        assertEquals("test", json["name"].string)
        val encoded = json["content"].string
        assertNotNull(encoded)
        val decoded = java.util.Base64.getDecoder().decode(encoded)
        assertContentEquals(byteArrayOf(0, 1, 2, 3, 255.toByte()), decoded)
    }

    @Test
    fun `ByteArray roundtrip through JSON`() {
        val original = BinaryData("doc", "Hello World".toByteArray())
        val json = original.toJsonObject()
        val restored = BinaryData.fromJson(json)
        assertEquals("doc", restored.name)
        assertContentEquals("Hello World".toByteArray(), restored.content)
    }

    @Test
    fun `empty ByteArray serialized as empty Base64 string`() {
        val data = BinaryData("empty", byteArrayOf())
        val json = data.toJsonObject()
        assertEquals("", json["content"].string)
        val restored = BinaryData.fromJson(json)
        assertContentEquals(byteArrayOf(), restored.content)
    }

    @Test
    fun `set of primitives serialized as JSON array`() {
        val obj = StringSet("test", setOf("a", "b", "c"))
        val json = obj.toJsonObject()
        val arr = json["tags"] as JsonArray
        assertEquals(3, arr.size)
        assertEquals(setOf("a", "b", "c"), arr.items.map { it.string }.toSet())
    }

    @Test
    fun `empty set serialized as empty JSON array`() {
        val obj = StringSet("test", emptySet())
        val json = obj.toJsonObject()
        val arr = json["tags"] as JsonArray
        assertEquals(0, arr.size)
    }

    @Test
    fun `map property serialized as JSON object`() {
        val obj = StringMapOutput("test", mapOf("env" to "prod", "region" to "us-east"))
        val json = obj.toJsonObject()

        assertEquals("test", json["name"].string)
        assertEquals("prod", json["labels"]["env"].string)
        assertEquals("us-east", json["labels"]["region"].string)
    }
}
