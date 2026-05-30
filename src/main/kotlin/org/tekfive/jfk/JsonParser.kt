package org.tekfive.jfk

import java.io.Reader
import java.io.StringReader

/**
 * Exception thrown when JSON parsing fails.
 *
 * @property position one-based character position where parsing detected the error.
 */
class JsonParseException(message: String, val position: Int) : RuntimeException("$message at position $position")

internal class JsonParser(
    private val reader: Reader,
    private val transform: JsonTransform? = null,
    private val config: JsonParserConfig = JsonParserConfig.DEFAULT,
) {

    constructor(input: String, transform: JsonTransform? = null, config: JsonParserConfig = JsonParserConfig.DEFAULT) :
        this(StringReader(input), transform, config)

    private var pos = 0
    private var current: Int = -1
    private var eof = false
    private var depth = 0

    init {
        advance()
    }

    private fun advance(): Int {
        current = reader.read()
        if (current == -1) {
            eof = true
        } else {
            pos++
        }
        return current
    }

    private fun peek(): Char {
        if (eof) throw error("Unexpected end of input")
        return current.toChar()
    }

    private fun hasMore(): Boolean = !eof

    private fun readChar(): Char {
        if (eof) throw error("Unexpected end of input")
        val ch = current.toChar()
        advance()
        return ch
    }

    fun parse(): JsonValue {
        skipWhitespace()
        val value = readValue(emptyList())
        skipWhitespace()
        if (hasMore()) {
            throw error("Unexpected character '${peek()}' after value")
        }
        value._accessPath = emptyList()
        return value
    }

    private fun readValue(path: List<String>): JsonValue {
        if (!hasMore()) throw error("Unexpected end of input")
        val raw = when (peek()) {
            '{' -> readObject(path)
            '[' -> readArray(path)
            '"' -> readString()
            't', 'f' -> readBool()
            'n' -> readNull()
            else -> {
                if (peek() == '-' || peek().isDigit()) {
                    readNumber()
                } else {
                    throw error("Unexpected character '${peek()}'")
                }
            }
        }
        return transform?.transform(path, raw) ?: raw
    }

    private fun pushDepth() {
        depth++
        if (config.maxDepth > 0 && depth > config.maxDepth) {
            throw error("Nesting depth exceeds maximum of ${config.maxDepth}")
        }
    }

    private fun popDepth() {
        depth--
    }

    private fun readObject(path: List<String>): JsonObject {
        pushDepth()
        try {
            expect('{')
            skipWhitespace()
            val entries = LinkedHashMap<String, JsonValue>()
            if (hasMore() && peek() == '}') {
                advance()
                return applyDefaults(path, entries)
            }
            while (true) {
                if (config.maxObjectEntries > 0 && entries.size >= config.maxObjectEntries) {
                    throw error("Object exceeds maximum of ${config.maxObjectEntries} entries")
                }
                skipWhitespace()
                if (!hasMore() || peek() != '"') {
                    throw error("Expected string key")
                }
                val key = readStringValue()
                if (config.rejectDuplicateKeys && key in entries) {
                    throw error("Duplicate key '$key'")
                }
                skipWhitespace()
                expect(':')
                skipWhitespace()
                val value = readValue(path + key)
                entries[key] = value
                skipWhitespace()
                if (!hasMore()) throw error("Unexpected end of input in object")
                when (peek()) {
                    ',' -> advance()
                    '}' -> { advance(); return applyDefaults(path, entries) }
                    else -> throw error("Expected ',' or '}'")
                }
            }
        } finally {
            popDepth()
        }
    }

    private fun applyDefaults(path: List<String>, entries: LinkedHashMap<String, JsonValue>): JsonObject {
        val t = transform ?: return JsonObject(entries)
        for (key in t.defaultKeys(path)) {
            if (key !in entries) {
                val value = t.default(path, key)
                if (value != null) {
                    entries[key] = value
                }
            }
        }
        return JsonObject(entries)
    }

    private fun readArray(path: List<String>): JsonArray {
        pushDepth()
        try {
            expect('[')
            skipWhitespace()
            val elements = mutableListOf<JsonValue>()
            if (hasMore() && peek() == ']') {
                advance()
                return JsonArray(elements)
            }
            while (true) {
                if (config.maxArrayElements > 0 && elements.size >= config.maxArrayElements) {
                    throw error("Array exceeds maximum of ${config.maxArrayElements} elements")
                }
                skipWhitespace()
                elements += readValue(path + elements.size.toString())
                skipWhitespace()
                if (!hasMore()) throw error("Unexpected end of input in array")
                when (peek()) {
                    ',' -> advance()
                    ']' -> { advance(); return JsonArray(elements) }
                    else -> throw error("Expected ',' or ']'")
                }
            }
        } finally {
            popDepth()
        }
    }

    private fun readString(): JsonString = JsonString(readStringValue())

    private fun readStringValue(): String {
        expect('"')
        val sb = StringBuilder()
        val maxLen = config.maxStringLength
        while (hasMore()) {
            val ch = readChar()
            when {
                ch == '"' -> return sb.toString()
                ch == '\\' -> {
                    if (!hasMore()) throw error("Unexpected end of input in string escape")
                    when (val esc = readChar()) {
                        '"' -> sb.append('"')
                        '\\' -> sb.append('\\')
                        '/' -> sb.append('/')
                        'b' -> sb.append('\b')
                        'f' -> sb.append('\u000C')
                        'n' -> sb.append('\n')
                        'r' -> sb.append('\r')
                        't' -> sb.append('\t')
                        'u' -> {
                            val hex = StringBuilder(4)
                            repeat(4) {
                                if (!hasMore()) throw error("Unexpected end of input in unicode escape")
                                hex.append(readChar())
                            }
                            val code = hex.toString().toIntOrNull(16)
                                ?: throw error("Invalid unicode escape: \\u$hex")
                            sb.append(code.toChar())
                        }
                        else -> throw error("Invalid escape character: \\$esc")
                    }
                }
                ch < '\u0020' -> throw error("Unescaped control character in string")
                else -> sb.append(ch)
            }
            if (maxLen > 0 && sb.length > maxLen) {
                throw error("String exceeds maximum length of $maxLen characters")
            }
        }
        throw error("Unterminated string")
    }

    private fun readNumber(): JsonNumber {
        val sb = StringBuilder()
        val maxLen = config.maxNumberLength

        fun appendChecked(ch: Char) {
            sb.append(ch)
            if (maxLen > 0 && sb.length > maxLen) {
                throw error("Number literal exceeds maximum length of $maxLen characters")
            }
        }

        // optional minus
        if (hasMore() && peek() == '-') appendChecked(readChar())

        // integer part
        if (!hasMore()) throw error("Unexpected end of input in number")
        if (peek() == '0') {
            appendChecked(readChar())
        } else if (peek().isDigit()) {
            while (hasMore() && peek().isDigit()) appendChecked(readChar())
        } else {
            throw error("Invalid number")
        }

        var isDecimal = false

        // fraction
        if (hasMore() && peek() == '.') {
            isDecimal = true
            appendChecked(readChar())
            if (!hasMore() || !peek().isDigit()) throw error("Expected digit after decimal point")
            while (hasMore() && peek().isDigit()) appendChecked(readChar())
        }

        // exponent
        if (hasMore() && (peek() == 'e' || peek() == 'E')) {
            isDecimal = true
            appendChecked(readChar())
            if (hasMore() && (peek() == '+' || peek() == '-')) appendChecked(readChar())
            if (!hasMore() || !peek().isDigit()) throw error("Expected digit in exponent")
            while (hasMore() && peek().isDigit()) appendChecked(readChar())
        }

        val text = sb.toString()
        val number: Number = try {
            if (isDecimal) {
                val d = text.toDouble()
                if (d.isInfinite() || d.isNaN()) throw error("Number out of range: $text")
                d
            } else {
                val long = text.toLong()
                if (long in Int.MIN_VALUE..Int.MAX_VALUE) long.toInt() else long
            }
        } catch (e: NumberFormatException) {
            throw error("Number out of range: $text")
        }
        return JsonNumber(number)
    }

    private fun readBool(): JsonBool {
        return if (tryConsume("true")) {
            JsonBool(true)
        } else if (tryConsume("false")) {
            JsonBool(false)
        } else {
            throw error("Expected 'true' or 'false'")
        }
    }

    private fun readNull(): JsonNull {
        if (tryConsume("null")) {
            return JsonNull
        }
        throw error("Expected 'null'")
    }

    private fun tryConsume(expected: String): Boolean {
        for (i in expected.indices) {
            if (!hasMore() || peek() != expected[i]) return false
            advance()
        }
        return true
    }

    private fun skipWhitespace() {
        while (hasMore() && peek().isWhitespace()) advance()
    }

    private fun expect(ch: Char) {
        if (!hasMore()) throw error("Expected '$ch' but reached end of input")
        if (peek() != ch) throw error("Expected '$ch' but got '${peek()}'")
        advance()
    }

    private fun error(message: String): JsonParseException = JsonParseException(message, pos)
}
