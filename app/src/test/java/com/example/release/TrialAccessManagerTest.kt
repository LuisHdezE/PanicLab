package com.example.release

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TrialAccessManagerTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("paniclab_release_access", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun firstLaunchStartsUsableThirtyDayTrial() {
        val manager = TrialAccessManager(context)

        val state = manager.currentState(nowEpochMs = 1_000_000L)

        assertTrue(state.canUseApp)
        assertFalse(state.isExpired)
        assertTrue(state.daysRemaining == 30)
    }

    @Test
    fun trialExpiresAfterThirtyDays() {
        val manager = TrialAccessManager(context)
        val start = 1_000_000L
        manager.currentState(nowEpochMs = start)

        val state = manager.currentState(nowEpochMs = start + THIRTY_DAYS_MS)

        assertTrue(state.isExpired)
        assertFalse(state.canUseApp)
    }

    @Test
    fun invalidActivationCodeKeepsTrialLocked() {
        val manager = TrialAccessManager(context)

        assertFalse(manager.activate("NOT-A-VALID-CODE"))
    }

    @Test
    fun temporaryReleaseCodeActivatesTheApp() {
        val manager = TrialAccessManager(
            context,
            activationCodeSha256 = "e49f2c0ee9a1370e1ebeb3d14564210df48514965ac88fcd155b5e6caf0f4071"
        )

        assertTrue(manager.activate("TEST-CODE"))
        assertTrue(manager.currentState(nowEpochMs = Long.MAX_VALUE / 4).canUseApp)
    }

    private companion object {
        const val THIRTY_DAYS_MS = 30L * 24L * 60L * 60L * 1000L
    }
}
