package com.example.data.appleknowledge

import android.content.Context
import com.example.appleknowledge.runtime.AppleOfficialKnowledgeResourceBundle
import com.example.appleknowledge.runtime.AppleOfficialKnowledgeResourceNames
import com.example.appleknowledge.runtime.AppleOfficialKnowledgeRuntime

object AndroidAppleOfficialKnowledgeLoader {
    fun load(context: Context): AppleOfficialKnowledgeRuntime {
        val assets = context.applicationContext.assets

        fun read(path: String): String = assets.open(path).bufferedReader().use { it.readText() }

        return AppleOfficialKnowledgeRuntime.create(
            AppleOfficialKnowledgeResourceBundle(
                capabilitiesJson = read(AppleOfficialKnowledgeResourceNames.capabilities),
                sourceJsonParts = AppleOfficialKnowledgeResourceNames.sources.map(::read),
                cardJsonParts = AppleOfficialKnowledgeResourceNames.cards.map(::read)
            )
        )
    }
}
