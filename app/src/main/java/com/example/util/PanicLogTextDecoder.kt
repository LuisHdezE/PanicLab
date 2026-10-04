package com.example.util

import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction

object PanicLogTextDecoder {

    fun decode(bytes: ByteArray): String {
        if (bytes.isEmpty()) return ""

        val decoded = when {
            bytes.hasPrefix(0xEF, 0xBB, 0xBF) ->
                decodeStrict(bytes.copyOfRange(3, bytes.size), Charsets.UTF_8)

            bytes.hasPrefix(0xFF, 0xFE) ->
                String(bytes.copyOfRange(2, bytes.size), Charsets.UTF_16LE)

            bytes.hasPrefix(0xFE, 0xFF) ->
                String(bytes.copyOfRange(2, bytes.size), Charsets.UTF_16BE)

            else ->
                decodeStrict(bytes, Charsets.UTF_8)
        }

        return normalizeLegacyCharacterSpacing(decoded)
    }

    private fun decodeStrict(bytes: ByteArray, charset: java.nio.charset.Charset): String =
        charset.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(bytes))
            .toString()

    private fun normalizeLegacyCharacterSpacing(text: String): String {
        val sample = text.take(8192)
        val looksCharacterSpaced =
            sample.contains("b u g _ t y p e", ignoreCase = true) ||
                sample.contains("p a n i c S t r i n g", ignoreCase = true) ||
                sample.contains("i P h o n e", ignoreCase = true)

        if (!looksCharacterSpaced) return text

        return SPACE_RUN.replace(text) { match ->
            val originalSpaceCount = ((match.value.length - 1) / 2).coerceAtLeast(0)
            " ".repeat(originalSpaceCount)
        }
    }

    private fun ByteArray.hasPrefix(vararg values: Int): Boolean {
        if (size < values.size) return false
        return values.indices.all { index -> this[index].toInt() and 0xFF == values[index] }
    }

    private val SPACE_RUN = Regex(" +")
}
