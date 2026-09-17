package org.tekfive.jfk

import java.math.BigDecimal
import java.math.BigInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertSame

data class DecimalRecord(val amount: BigDecimal) {
    companion object : FromJsonObject<DecimalRecord>
}

data class StrictDecimalRecord(val amount: BigDecimal) {
    companion object : FromJsonObject<StrictDecimalRecord> {
        override val lax = false
    }
}

data class OptionalDecimalRecord(val amount: BigDecimal?, val defaulted: BigDecimal = BigDecimal.TEN) {
    companion object : FromJsonObject<OptionalDecimalRecord>
}

data class DecimalContainer(val record: DecimalRecord, val amounts: List<BigDecimal?>, val rates: Map<String, BigDecimal>) {
    companion object : FromJsonObject<DecimalContainer>
}

class BigDecimalJsonTest {
    @Test
    fun `member and reflection accept decimal strings and numbers without losing stored precision`() {
        val values = listOf(
            "12345678901234567890.12345678901234567890",
            "-1.2300E+100",
            BigDecimal("12345678901234567890.12345678901234567890"),
            BigInteger("123456789012345678901234567890"),
            Long.MAX_VALUE,
            0,
            1.25,
        )
        for (value in values) {
            val json = JsonObject("amount" to value)
            val expected = BigDecimal(value.toString())
            assertEquals(expected, json.bigDecimal("amount"))
            assertEquals(expected, DecimalRecord.fromJson(json).amount)
            assertEquals(expected, StrictDecimalRecord.fromJson(json).amount)
        }
    }

    @Test
    fun `member returns null for missing and null values and optionally empty strings`() {
        val json = JsonObject("null" to JsonNull, "empty" to "")
        for (flag in listOf(false, true)) {
            assertNull(json.bigDecimal("missing", flag))
            assertNull(json.bigDecimal("null", flag))
        }
        assertNull(json.bigDecimal("empty", treatEmptyStringAsNull = true))
        assertFailsWith<JsonMappingException> { json.bigDecimal("empty") }
    }

    @Test
    fun `invalid decimals report the property and original value`() {
        for (value in listOf("", " ", " 1.2 ", "invalid", "NaN", "Infinity", true, JsonArray(), JsonObject())) {
            val json = JsonObject("amount" to value)
            val memberError = assertFailsWith<JsonMappingException> { json.bigDecimal("amount") }
            val mappingError = assertFailsWith<JsonMappingException> { DecimalRecord.fromJson(json) }
            for (error in listOf(memberError, mappingError)) {
                assertEquals("amount", error.path)
                assertEquals("decimal number or string", error.expected)
                assertSame(json["amount"], error.actual)
            }
        }
    }

    @Test
    fun `reflection respects nullability defaults and empty string handling`() {
        for (json in listOf(JsonObject(), JsonObject("amount" to JsonNull))) {
            assertEquals(OptionalDecimalRecord(null), OptionalDecimalRecord.fromJson(json))
            assertFailsWith<JsonMappingException> { DecimalRecord.fromJson(json) }
        }
        val empty = JsonObject("amount" to "", "defaulted" to "")
        assertEquals(OptionalDecimalRecord(null), OptionalDecimalRecord.fromJson(empty, treatEmptyStringAsNull = true))
        assertFailsWith<JsonMappingException> { OptionalDecimalRecord.fromJson(empty) }
        assertFailsWith<JsonMappingException> { DecimalRecord.fromJson(empty, treatEmptyStringAsNull = true) }
    }

    @Test
    fun `reflection maps parsed numbers strings nested objects lists and maps`() {
        val json = Json.parse(
            """{"record":{"amount":"12345678901234567890.123456789"},"amounts":[42,1.25,"-2.50",null],"rates":{"tax":"0.0825"}}""",
        ) as JsonObject
        assertEquals(
            DecimalContainer(
                DecimalRecord(BigDecimal("12345678901234567890.123456789")),
                listOf(BigDecimal("42"), BigDecimal("1.25"), BigDecimal("-2.50"), null),
                mapOf("tax" to BigDecimal("0.0825")),
            ),
            DecimalContainer.fromJson(json),
        )
    }

    @Test
    fun `reflection reports paths for invalid nested and collection decimals`() {
        val cases = mapOf(
            """{"record":{"amount":"bad"},"amounts":[],"rates":{}}""" to "record.amount",
            """{"record":{"amount":1},"amounts":["bad"],"rates":{}}""" to "amounts[0]",
            """{"record":{"amount":1},"amounts":[],"rates":{"tax":"bad"}}""" to "rates.tax",
        )
        for ((text, path) in cases) {
            val error = assertFailsWith<JsonMappingException> {
                DecimalContainer.fromJson(Json.parse(text) as JsonObject)
            }
            assertEquals(path, error.path)
            assertEquals("decimal number or string", error.expected)
        }
    }
}
