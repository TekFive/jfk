package org.tekfive.jfk

import java.time.Instant as JavaInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
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
    fun `instant member parses ISO strings preserving nanoseconds and offsets`() {
        val json = JsonObject(mapOf(
            "utc" to instantText,
            "offset" to "2026-08-20T16:15:16.123456789+02:00",
        ))

        assertEquals(JavaInstant.parse(instantText), json.instant("utc"))
        assertEquals(JavaInstant.parse(instantText), json.instant("offset"))
    }

    @Test
    fun `instant member accepts epoch milliseconds as numbers and strings`() {
        val json = Json.parse(
            """{"number":1787235316123,"string":"1787235316123","zero":0,"negative":-1,"wholeDecimal":1000.0}""",
        ) as JsonObject

        val expected = JavaInstant.parse("2026-08-20T14:15:16.123Z")
        assertEquals(expected, json.instant("number"))
        assertEquals(expected, json.instant("string"))
        assertEquals(JavaInstant.EPOCH, json.instant("zero"))
        assertEquals(JavaInstant.parse("1969-12-31T23:59:59.999Z"), json.instant("negative"))
        assertEquals(JavaInstant.parse("1970-01-01T00:00:01Z"), json.instant("wholeDecimal"))
    }

    @Test
    fun `instant member handles Long boundaries without overflow`() {
        for (millis in listOf(Long.MIN_VALUE, Long.MAX_VALUE)) {
            val json = JsonObject(mapOf("number" to millis, "string" to millis.toString()))
            assertEquals(JavaInstant.ofEpochMilli(millis), json.instant("number"))
            assertEquals(JavaInstant.ofEpochMilli(millis), json.instant("string"))
        }
    }

    @Test
    fun `instant member returns null for missing invalid or unsupported values`() {
        val json = Json.parse(
            """{"null":null,"invalid":"not-an-instant","empty":"","fraction":1.5,"fractionString":"1.5","overflowString":"9223372036854775808","boolean":true,"array":[],"object":{}}""",
        ) as JsonObject
        json["overflow"] = java.math.BigInteger("9223372036854775808")

        for (name in json.keys + "missing") {
            assertNull(json.instant(name), name)
        }
    }

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
