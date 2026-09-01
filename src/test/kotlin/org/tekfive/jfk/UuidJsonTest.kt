@file:OptIn(kotlin.uuid.ExperimentalUuidApi::class)

package org.tekfive.jfk

import java.util.UUID as JavaUuid
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
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
