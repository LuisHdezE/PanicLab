package com.example.platform

import java.security.MessageDigest
import java.util.UUID

object SystemEpochClock : Clock {
    override fun nowEpochMillis(): Long = System.currentTimeMillis()
}

object UuidIdGenerator : IdGenerator {
    override fun nextId(): String = UUID.randomUUID().toString()
}

object AndroidSha256Hasher : Sha256Hasher {
    override fun sha256(text: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { byte -> byte.toUByte().toString(16).padStart(2, '0') }
    }
}
