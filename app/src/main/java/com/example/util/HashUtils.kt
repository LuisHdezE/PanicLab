package com.example.util

import com.example.platform.AndroidSha256Hasher

object HashUtils {
    fun sha256(text: String): String = AndroidSha256Hasher.sha256(text)
}
