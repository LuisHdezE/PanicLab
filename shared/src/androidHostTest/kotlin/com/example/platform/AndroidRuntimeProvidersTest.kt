package com.example.platform

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AndroidRuntimeProvidersTest {
    @Test
    fun androidSha256MatchesKnownVector() {
        assertEquals(
            "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad",
            AndroidSha256Hasher.sha256("abc")
        )
    }

    @Test
    fun androidRuntimeProvidersReturnUsableValues() {
        assertTrue(SystemEpochClock.nowEpochMillis() > 0L)
        assertTrue(UuidIdGenerator.nextId().isNotBlank())
    }
}
