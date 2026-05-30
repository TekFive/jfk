package org.tekfive.jfk

import java.io.Writer

internal object JsonWriter {

    fun write(value: JsonValue, indent: Int): String {
        val sb = StringBuilder()
        writeValue(sb, value, indent, 0)
        return sb.toString()
    }

    fun writeTo(value: JsonValue, writer: Writer, indent: Int = 0) {
        val sb = StringBuilder()
        writeValue(sb, value, indent, 0)
        writer.write(sb.toString())
    }

    private fun writeValue(sb: StringBuilder, value: JsonValue, indent: Int, depth: Int) {
        when (value) {
            is JsonNull -> sb.append("null")
            is JsonBool -> sb.append(value.value)
            is JsonNumber -> sb.append(formatNumber(value.value))
            is JsonString -> writeString(sb, value.value)
            is JsonObject -> writeObject(sb, value, indent, depth)
            is JsonArray -> writeArray(sb, value, indent, depth)
        }
    }

    private fun formatNumber(number: Number): String {
        val d = number.toDouble()
        return if (d == d.toLong().toDouble() && !d.isInfinite()) {
            number.toLong().toString()
        } else {
            number.toString()
        }
    }

    private fun writeString(sb: StringBuilder, value: String) {
        sb.append('"')
        for (ch in value) {
            when (ch) {
                '"' -> sb.append("\\\"")
                '\\' -> sb.append("\\\\")
                '\b' -> sb.append("\\b")
                '\u000C' -> sb.append("\\f")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                else -> {
                    if (ch < '\u0020') {
                        sb.append("\\u%04x".format(ch.code))
                    } else {
                        sb.append(ch)
                    }
                }
            }
        }
        sb.append('"')
    }

    private fun writeObject(sb: StringBuilder, obj: JsonObject, indent: Int, depth: Int) {
        if (obj.entries.isEmpty()) {
            sb.append("{}")
            return
        }
        val pretty = indent > 0
        sb.append('{')
        var first = true
        for ((key, value) in obj.entries) {
            if (!first) sb.append(',')
            first = false
            if (pretty) {
                sb.append('\n')
                repeat((depth + 1) * indent) { sb.append(' ') }
            }
            writeString(sb, key)
            sb.append(':')
            if (pretty) sb.append(' ')
            writeValue(sb, value, indent, depth + 1)
        }
        if (pretty) {
            sb.append('\n')
            repeat(depth * indent) { sb.append(' ') }
        }
        sb.append('}')
    }

    private fun writeArray(sb: StringBuilder, arr: JsonArray, indent: Int, depth: Int) {
        if (arr.items.isEmpty()) {
            sb.append("[]")
            return
        }
        val pretty = indent > 0
        sb.append('[')
        var first = true
        for (element in arr.items) {
            if (!first) sb.append(',')
            first = false
            if (pretty) {
                sb.append('\n')
                repeat((depth + 1) * indent) { sb.append(' ') }
            }
            writeValue(sb, element, indent, depth + 1)
        }
        if (pretty) {
            sb.append('\n')
            repeat(depth * indent) { sb.append(' ') }
        }
        sb.append(']')
    }
}
