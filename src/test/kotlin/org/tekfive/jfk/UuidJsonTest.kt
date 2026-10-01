@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package org.tekfive.jfk

import java.util.UUID as JavaUuid
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.uuid.Uuid as KotlinUuid

data class UuidRecord(
    val javaUuid: JavaUuid,
    val kotlinUuid: KotlinUuid,
) : ToJsonObject {
    companion object : FromJsonObject<UuidRecord>
}

class UuidJsonTest {

    private val uuidText = "123e4567-e89b-12d3-a456-426614174000"

    @Test
    fun `UUID accessors parse strings regardless of letter case`() {
        for (text in listOf(uuidText, uuidText.uppercase(), "00000000-0000-0000-0000-000000000000", "ffffffff-ffff-ffff-ffff-ffffffffffff")) {
            val json = JsonObject("id" to text)
            val expected = JavaUuid.fromString(text)

            assertEquals(expected, json.uuid("id"))
            assertEquals(expected, json.reqUuid("id"))
        }
    }

    @Test
    fun `UUID accessors read values encoded from either UUID type`() {
        val expected = JavaUuid.fromString(uuidText)
        val json = JsonObject("java" to expected, "kotlin" to KotlinUuid.parse(uuidText))

        for (name in json.keys) {
            assertEquals(expected, json.uuid(name))
            assertEquals(expected, json.reqUuid(name))
        }
    }

    @Test
    fun `UUID accessors handle missing null invalid and non-string properties`() {
        val json = """{"invalid":"not-a-uuid","empty":"","hex":"123e4567-e89b-12d3-a456-42661417400z","null":null,"number":42,"boolean":true,"array":[],"object":{}}""".asRequiredJsonObject()

        for (name in json.keys + "missing") {
            assertNull(json.uuid(name), name)
            val error = assertFailsWith<IllegalStateException>(name) { json.reqUuid(name) }
            assertTrue(error.message!!.contains("Required UUID at '$name'"), error.message)
        }
    }

    @Test
    fun `required UUID errors include nested property paths and invalid values`() {
        val json = """{"users":[{"id":"not-a-uuid"}]}""".asRequiredJsonObject()
        val user = json["users"][0].reqObj

        val invalid = assertFailsWith<IllegalStateException> { user.reqUuid("id") }
        assertTrue(invalid.message!!.contains("users.0.id"), invalid.message)
        assertTrue(invalid.message!!.contains("not-a-uuid"), invalid.message)

        val missing = assertFailsWith<IllegalStateException> { user.reqUuid("missing") }
        assertTrue(missing.message!!.contains("users.0.missing"), missing.message)
        assertTrue(missing.message!!.contains("JsonNull"), missing.message)
    }

    @Test
    fun `reflective serialization encodes both UUID types as canonical strings`() {
        val value = UuidRecord(
            JavaUuid.fromString(uuidText),
            KotlinUuid.parse(uuidText),
        )

        assertEquals(
            """{"javaUuid":"$uuidText","kotlinUuid":"$uuidText"}""",
            value.toJsonString(),
        )
    }

    @Test
    fun `reflective deserialization decodes both UUID types from strings`() {
        val value =
            """{"javaUuid":"$uuidText","kotlinUuid":"$uuidText"}"""
                .fromJsonOrThrow(UuidRecord)

        assertEquals(JavaUuid.fromString(uuidText), value.javaUuid)
        assertEquals(KotlinUuid.parse(uuidText), value.kotlinUuid)
    }

    @Test
    fun `both UUID types round trip through JSON`() {
        val original = UuidRecord(
            JavaUuid.fromString(uuidText),
            KotlinUuid.parse(uuidText),
        )

        assertEquals(original, original.toJsonString().fromJsonOrThrow(UuidRecord))
    }

    @Test
    fun `generic tree conversion encodes both UUID types as strings`() {
        val value = JsonValue.toJsonValue(
            listOf(JavaUuid.fromString(uuidText), KotlinUuid.parse(uuidText)),
        )

        assertEquals("""["$uuidText","$uuidText"]""", value.toJsonString())
    }

    @Test
    fun `invalid Java UUID reports its property path`() {
        val error = assertFailsWith<JsonMappingException> {
            """{"javaUuid":"not-a-uuid","kotlinUuid":"$uuidText"}"""
                .fromJsonOrThrow(UuidRecord)
        }

        assertEquals("javaUuid", error.path)
        assertTrue(error.expected.contains("java.util.UUID"))
    }

    @Test
    fun `invalid Kotlin UUID reports its property path`() {
        val error = assertFailsWith<JsonMappingException> {
            """{"javaUuid":"$uuidText","kotlinUuid":"not-a-uuid"}"""
                .fromJsonOrThrow(UuidRecord)
        }

        assertEquals("kotlinUuid", error.path)
        assertTrue(error.expected.contains("kotlin.uuid.Uuid"))
    }
}
