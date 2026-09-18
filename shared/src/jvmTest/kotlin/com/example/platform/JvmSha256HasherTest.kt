package com.example.platform

import kotlin.test.Test
import kotlin.test.assertEquals

class JvmSha256HasherTest {
    @Test
    fun matchesKnownSha256Vectors() {
        assertEquals(
            "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
            JvmSha256Hasher.sha256("")
        )
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            JvmSha256Hasher.sha256("abc")
        )
        assertEquals(
            "d7a8fbb307d7809469ca9abcb0082e4f8d5651e46d3cdb762d02d0bf37c9e592",
            JvmSha256Hasher.sha256("The quick brown fox jumps over the lazy dog")
        )
    }
}
