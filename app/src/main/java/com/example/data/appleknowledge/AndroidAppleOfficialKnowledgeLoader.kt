package com.example.data.appleknowledge

import android.content.Context
import com.example.appleknowledge.runtime.AppleOfficialKnowledgeRuntime
import com.example.appleknowledge.runtime.AppleOfficialKnowledgeRuntimeFactory

object AndroidAppleOfficialKnowledgeLoader {
    fun load(@Suppress("UNUSED_PARAMETER") context: Context): AppleOfficialKnowledgeRuntime =
        AppleOfficialKnowledgeRuntimeFactory.embedded()
}
