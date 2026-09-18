package com.example.platform

fun interface Clock {
    fun nowEpochMillis(): Long
}

fun interface IdGenerator {
    fun nextId(): String
}

fun interface Sha256Hasher {
    fun sha256(text: String): String
}
