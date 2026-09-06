package org.tekfive.jfk

import java.util.Collections

/** One object key or zero-based array index in a [JsonPath]. */
sealed interface JsonPathSegment {
    data class Key(val name: String) : JsonPathSegment

    data class Index(val index: Int) : JsonPathSegment {
        init {
            require(index >= 0) { "A JSON path index must be nonnegative" }
        }
    }
}

/**
 * Invalid path syntax, with a zero-based UTF-16 [offset] into the input string.
 * An offset equal to the input length indicates unexpected end of input.
 */
class JsonPathParseException(message: String, val offset: Int) :
    IllegalArgumentException("$message at offset $offset")

/**
 * An immutable location relative to the value supplied to [JsonValue.at].
 *
 * Paths start with `$`, followed by `.identifier`, `["JSON-escaped key"]`, or
 * `[index]` segments. Identifiers use ASCII letters, digits, and underscores,
 * and cannot start with a digit. Indices are nonnegative [Int] values without
 * leading zeroes. Whitespace outside quoted keys and query operators are unsupported.
 *
 * [toString] uses dot notation for identifier keys and quoted brackets otherwise.
 * Parsing this canonical representation always produces an equal path.
 */
class JsonPath(segments: List<JsonPathSegment>) {
    /** A defensive, unmodifiable copy of the supplied segments. */
    val segments: List<JsonPathSegment> = Collections.unmodifiableList(ArrayList(segments))

    /** Append an object key without modifying this path. */
    fun key(name: String): JsonPath = JsonPath(segments + JsonPathSegment.Key(name))

    /** Append a zero-based array index without modifying this path. */
    fun index(index: Int): JsonPath = JsonPath(segments + JsonPathSegment.Index(index))

    override fun equals(other: Any?): Boolean = other is JsonPath && segments == other.segments

    override fun hashCode(): Int = segments.hashCode()

    override fun toString(): String = buildString {
        append('$')
        for (segment in segments) {
            when (segment) {
                is JsonPathSegment.Key -> {
                    val name = segment.name
                    if (name.isNotEmpty() && name[0].isIdentifierStart() && name.all { it.isIdentifierPart() }) {
                        append('.').append(name)
                    } else {
                        append('[').append(JsonString(name).toJsonString()).append(']')
                    }
                }
                is JsonPathSegment.Index -> append('[').append(segment.index).append(']')
            }
        }
    }

    companion object {
        /** The supplied value itself. */
        val Root: JsonPath = JsonPath(emptyList())

        /** Parse a rooted path, throwing [JsonPathParseException] for invalid syntax. */
        fun parse(path: String): JsonPath = JsonPathParser(path).parse()
    }
}

private fun Char.isIdentifierStart(): Boolean = this in 'a'..'z' || this in 'A'..'Z' || this == '_'
private fun Char.isIdentifierPart(): Boolean = isIdentifierStart() || this in '0'..'9'

private class JsonPathParser(private val input: String) {
    private var offset = 0

    fun parse(): JsonPath {
        expect('$')
        val segments = mutableListOf<JsonPathSegment>()
        while (offset < input.length) {
            when (input[offset]) {
                '.' -> {
                    offset++
                    val start = offset
                    if (peek()?.isIdentifierStart() != true) fail("Expected an identifier")
                    while (peek()?.isIdentifierPart() == true) offset++
                    segments += JsonPathSegment.Key(input.substring(start, offset))
                }
                '[' -> {
                    offset++
                    segments += if (peek() == '"') JsonPathSegment.Key(readKey()) else readIndex()
                    expect(']')
                }
                else -> fail("Expected '.' or '['")
            }
        }
        return if (segments.isEmpty()) JsonPath.Root else JsonPath(segments)
    }

    private fun readIndex(): JsonPathSegment.Index {
        if (peek() !in '0'..'9') fail("Expected an array index or quoted key")
        val start = offset
        var index = 0
        while (peek() in '0'..'9') {
            if (offset > start && input[start] == '0') fail("Leading zeroes are not allowed")
            val digit = input[offset] - '0'
            if (index > (Int.MAX_VALUE - digit) / 10) fail("Array index exceeds Int.MAX_VALUE")
            index = index * 10 + digit
            offset++
        }
        return JsonPathSegment.Index(index)
    }

    private fun readKey(): String {
        expect('"')
        return buildString {
            while (true) {
                val ch = peek() ?: fail("Unterminated quoted key")
                when {
                    ch == '"' -> {
                        offset++
                        return@buildString
                    }
                    ch == '\\' -> {
                        offset++
                        val escape = peek() ?: fail("Unterminated escape")
                        when (escape) {
                            '"', '\\', '/' -> { append(escape); offset++ }
                            'b' -> { append('\b'); offset++ }
                            'f' -> { append('\u000C'); offset++ }
                            'n' -> { append('\n'); offset++ }
                            'r' -> { append('\r'); offset++ }
                            't' -> { append('\t'); offset++ }
                            'u' -> {
                                offset++
                                var code = 0
                                repeat(4) {
                                    val hex = peek() ?: fail("Incomplete Unicode escape")
                                    val digit = when (hex) {
                                        in '0'..'9' -> hex - '0'
                                        in 'a'..'f' -> hex - 'a' + 10
                                        in 'A'..'F' -> hex - 'A' + 10
                                        else -> fail("Expected a hexadecimal digit")
                                    }
                                    code = code * 16 + digit
                                    offset++
                                }
                                append(code.toChar())
                            }
                            else -> fail("Invalid JSON escape")
                        }
                    }
                    ch < ' ' -> fail("Unescaped control character in key")
                    else -> { append(ch); offset++ }
                }
            }
        }
    }

    private fun peek(): Char? = input.getOrNull(offset)

    private fun expect(expected: Char) {
        if (peek() != expected) fail("Expected '$expected'")
        offset++
    }

    private fun fail(message: String): Nothing = throw JsonPathParseException(message, offset)
}
