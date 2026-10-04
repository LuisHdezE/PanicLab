package com.example.release

import android.content.Context
import java.security.MessageDigest
import kotlin.math.ceil

data class TrialAccessState(
    val isActivated: Boolean,
    val isExpired: Boolean,
    val daysRemaining: Int
) {
    val canUseApp: Boolean
        get() = isActivated || !isExpired
}

class TrialAccessManager(
    context: Context,
    private val activationCodeSha256: String = DEFAULT_ACTIVATION_CODE_SHA256
) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun currentState(nowEpochMs: Long = System.currentTimeMillis()): TrialAccessState {
        val firstLaunchEpochMs = preferences.getLong(KEY_FIRST_LAUNCH_EPOCH_MS, 0L)
            .takeIf { it > 0L }
            ?: nowEpochMs.also {
                preferences.edit().putLong(KEY_FIRST_LAUNCH_EPOCH_MS, it).apply()
            }

        val activated = preferences.getBoolean(KEY_ACTIVATED, false)
        val elapsedMs = (nowEpochMs - firstLaunchEpochMs).coerceAtLeast(0L)
        val remainingMs = (TRIAL_DURATION_MS - elapsedMs).coerceAtLeast(0L)
        val daysRemaining = if (activated) {
            0
        } else {
            ceil(remainingMs.toDouble() / DAY_MS.toDouble()).toInt()
        }

        return TrialAccessState(
            isActivated = activated,
            isExpired = !activated && elapsedMs >= TRIAL_DURATION_MS,
            daysRemaining = daysRemaining
        )
    }

    fun activate(code: String): Boolean {
        val normalized = code.trim().uppercase()
        if (sha256(normalized) != activationCodeSha256) {
            return false
        }

        preferences.edit().putBoolean(KEY_ACTIVATED, true).apply()
        return true
    }

    private fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { byte -> "%02x".format(byte) }

    private companion object {
        const val PREFERENCES_NAME = "paniclab_release_access"
        const val KEY_FIRST_LAUNCH_EPOCH_MS = "first_launch_epoch_ms"
        const val KEY_ACTIVATED = "activated"

        const val DAY_MS = 24L * 60L * 60L * 1000L
        const val TRIAL_DURATION_MS = 30L * DAY_MS

        // Temporary Google Play validation code. Replace with production licensing after publication.
        const val DEFAULT_ACTIVATION_CODE_SHA256 =
            "75576b6da2ce16717559a3107a1d935eaf899efd425bb77831923b987007f344"
    }
}
