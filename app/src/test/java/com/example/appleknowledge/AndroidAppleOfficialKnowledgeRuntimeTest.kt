package com.example.appleknowledge

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.appleknowledge.AndroidAppleOfficialKnowledgeLoader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AndroidAppleOfficialKnowledgeRuntimeTest {

    @Test
    fun androidCompositionLoadsFrozenEmbeddedCatalogWithoutRulePackCoupling() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val runtime = AndroidAppleOfficialKnowledgeLoader.load(context)
        val catalog = runtime.catalog

        assertEquals("1.0.0", catalog.datasetVersion)
        assertEquals(19, catalog.capabilities.size)
        assertEquals(48, catalog.exactModels.size)

        val mapped = catalog.exactModels
            .flatMap(catalog::getCardsForExactModel)
            .associateBy { it.id }
        assertEquals(241, mapped.size)
        assertTrue(mapped.values.all { it.rulePackEffect.name == "NONE" })

        assertEquals(13, catalog.getCardsForExactModel("iPhone 12").size)
        assertEquals(9, catalog.getCardsForExactModel("iPhone 12 mini").size)
        assertTrue(catalog.getCardsForExactModel("iPhone 12 Pro").any { it.id == "AOKF-12-013" })
        assertFalse(catalog.getCardsForExactModel("iPhone 12 mini").any { it.id == "AOKF-12-013" })
    }
}
