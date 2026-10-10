package org.tekfive.jfk

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

data class LocalDateRecord(val date: LocalDate) : ToJsonObject {
    companion object : FromJsonObject<LocalDateRecord>
}

data class StrictLocalDateRecord(val date: LocalDate) {
    companion object : FromJsonObject<StrictLocalDateRecord> {
        override val lax = false
    }
}

data class OptionalLocalDateRecord(val date: LocalDate?, val defaulted: LocalDate = LocalDate.of(2000, 1, 1)) : ToJsonObject {
    companion object : FromJsonObject<OptionalLocalDateRecord>
}

data class LocalDateContainer(
    val record: LocalDateRecord,
    val dates: List<LocalDate?>,
    val datesByName: Map<String, LocalDate>,
) : ToJsonObject {
    companion object : FromJsonObject<LocalDateContainer>
}

class LocalDateJsonTest {
    @Test
    fun `dates serialize as ISO strings and round trip in strict and lax mapping`() {
        for (date in listOf(LocalDate.of(2026, 10, 10), LocalDate.of(2024, 2, 29), LocalDate.MIN, LocalDate.MAX)) {
            val original = LocalDateRecord(date)
            val text = """{"date":"$date"}"""

            assertEquals(text, original.toJsonString())
            assertEquals(original, text.fromJsonOrThrow(LocalDateRecord))
            assertEquals(date, text.fromJsonOrThrow(StrictLocalDateRecord).date)
            assertEquals(text, JsonObject("date" to date).toJsonString())
            assertEquals(JsonString(date.toString()), JsonValue.toJsonValue(date))
        }
    }

    @Test
    fun `nested dates lists and maps round trip`() {
        val date = LocalDate.of(2024, 2, 29)
        val original = LocalDateContainer(LocalDateRecord(date), listOf(date, null), mapOf("start" to date))
        val text = """{"record":{"date":"2024-02-29"},"dates":["2024-02-29",null],"datesByName":{"start":"2024-02-29"}}"""

        assertEquals(text, original.toJsonString())
        assertEquals(original, text.fromJsonOrThrow(LocalDateContainer))
    }

    @Test
    fun `additional JSON values support dates`() {
        val value = object : ToJsonObject {
            override fun additionalJsonValues(): Map<String, Any?> = mapOf("date" to LocalDate.of(2026, 10, 10))
        }

        assertEquals("""{"date":"2026-10-10"}""", value.toJsonString())
    }

    @Test
    fun `date mapping respects nullability defaults and empty string handling`() {
        for (json in listOf(JsonObject(), JsonObject("date" to JsonNull))) {
            assertEquals(OptionalLocalDateRecord(null), OptionalLocalDateRecord.fromJson(json))
            assertFailsWith<JsonMappingException> { LocalDateRecord.fromJson(json) }
        }
        val original = OptionalLocalDateRecord(null)
        assertEquals("""{"defaulted":"2000-01-01"}""", original.toJsonString())
        assertEquals(original, original.toJsonString().fromJsonOrThrow(OptionalLocalDateRecord))

        val empty = JsonObject("date" to "", "defaulted" to "")
        assertEquals(original, OptionalLocalDateRecord.fromJson(empty, treatEmptyStringAsNull = true))
        assertFailsWith<JsonMappingException> { OptionalLocalDateRecord.fromJson(empty) }
        assertFailsWith<JsonMappingException> { LocalDateRecord.fromJson(empty, treatEmptyStringAsNull = true) }
    }

    @Test
    fun `invalid dates and unsupported types report the property and original value`() {
        val values = listOf(
            "not-a-date", "", "2023-02-29", "2024-04-31", "2024-13-01", "2024-2-01",
            "2024-02-29T00:00:00Z", " 2024-02-29 ", 0, true, JsonArray(), JsonObject(),
        )
        for (value in values) {
            val json = JsonObject("date" to value)
            val error = assertFailsWith<JsonMappingException>(value.toString()) { LocalDateRecord.fromJson(json) }
            assertEquals("date", error.path)
            assertTrue(error.expected.contains("LocalDate"), error.expected)
            assertSame(json["date"], error.actual)
            assertNull(LocalDateRecord.fromJsonOptional(json))
            assertFailsWith<JsonMappingException> { StrictLocalDateRecord.fromJson(json) }
        }
    }

    @Test
    fun `invalid nested and collection dates report their full paths`() {
        val cases = mapOf(
            """{"record":{"date":"bad"},"dates":[],"datesByName":{}}""" to "record.date",
            """{"record":{"date":"2024-02-29"},"dates":["bad"],"datesByName":{}}""" to "dates[0]",
            """{"record":{"date":"2024-02-29"},"dates":[],"datesByName":{"start":"bad"}}""" to "datesByName.start",
        )
        for ((text, path) in cases) {
            val error = assertFailsWith<JsonMappingException> { text.fromJsonOrThrow(LocalDateContainer) }
            assertEquals(path, error.path)
            assertTrue(error.expected.contains("java.time.LocalDate"), error.expected)
        }
    }
}
