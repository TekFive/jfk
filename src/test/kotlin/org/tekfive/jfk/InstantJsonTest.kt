package org.tekfive.jfk

import java.time.Instant as JavaInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Instant as KotlinInstant

data class InstantRecord(
    val javaInstant: JavaInstant,
    val kotlinInstant: KotlinInstant,
) : ToJsonObject {
    companion object : FromJsonObject<InstantRecord>
}

class InstantJsonTest {

    private val instantText = "2026-08-20T14:15:16.123456789Z"

    @Test
    fun `reflective serialization encodes both Instant types as ISO strings`() {
        val value = InstantRecord(
            JavaInstant.parse(instantText),
            KotlinInstant.parse(instantText),
        )

        assertEquals(
            """{"javaInstant":"$instantText","kotlinInstant":"$instantText"}""",
            value.toJsonString(),
        )
    }

    @Test
    fun `reflective deserialization decodes both Instant types from ISO strings`() {
        val value =
            """{"javaInstant":"$instantText","kotlinInstant":"$instantText"}"""
                .fromJsonOrThrow(InstantRecord)

        assertEquals(JavaInstant.parse(instantText), value.javaInstant)
        assertEquals(KotlinInstant.parse(instantText), value.kotlinInstant)
    }

    @Test
    fun `both Instant types round trip through JSON`() {
        val original = InstantRecord(
            JavaInstant.parse(instantText),
            KotlinInstant.parse(instantText),
        )

        assertEquals(original, original.toJsonString().fromJsonOrThrow(InstantRecord))
    }

    @Test
    fun `generic tree conversion encodes both Instant types as ISO strings`() {
        val value = JsonValue.toJsonValue(
            listOf(JavaInstant.parse(instantText), KotlinInstant.parse(instantText)),
        )

        assertEquals("""["$instantText","$instantText"]""", value.toJsonString())
    }

    @Test
    fun `invalid Java Instant reports its property path`() {
        val error = assertFailsWith<JsonMappingException> {
            """{"javaInstant":"not-an-instant","kotlinInstant":"$instantText"}"""
                .fromJsonOrThrow(InstantRecord)
        }

        assertEquals("javaInstant", error.path)
        assertTrue(error.expected.contains("java.time.Instant"))
    }

    @Test
    fun `invalid Kotlin Instant reports its property path`() {
        val error = assertFailsWith<JsonMappingException> {
            """{"javaInstant":"$instantText","kotlinInstant":"not-an-instant"}"""
                .fromJsonOrThrow(InstantRecord)
        }

        assertEquals("kotlinInstant", error.path)
        assertTrue(error.expected.contains("kotlin.time.Instant"))
    }
}
