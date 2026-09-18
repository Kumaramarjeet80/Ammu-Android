package com.ammu.player.audio

import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.util.Log

enum class EqEngineMode { VLC, VIVO }

data class VivoPreset(
    val key: String,
    val name: String,
    val gains: FloatArray
)

class DspEngine {

    companion object {
        val VLC_BANDS = intArrayOf(60, 170, 310, 600, 1000, 3000, 6000, 12000, 14000, 16000)
        val DEFAULT_VLC_GAINS = floatArrayOf(18.2f, 11.8f, 3.7f, -1.7f, -7.8f, 2.1f, 9.8f, 14.1f, -3.6f, 11.6f)

        val VIVO_BANDS = intArrayOf(31, 62, 125, 250, 500, 1000, 2000, 4000, 8000, 16000)

        val VIVO_OFFICIAL_PRESETS = mapOf(
            "custom" to floatArrayOf(12f, 8f, 5f, 2f, -2f, -6f, -8f, -11f, -3f, 7f),
            "pop" to floatArrayOf(6f, 4f, -3f, -2f, 4f, 2f, -4f, -3f, 6f, 3f),
            "dance" to floatArrayOf(6f, 4f, -3f, -3f, 3f, 2f, -2f, 0f, 6f, 4f),
            "blues" to floatArrayOf(3f, 4f, 0f, 0f, 4f, 2f, 2f, 1f, 3f, 3f),
            "classical" to floatArrayOf(6f, 8f, 2f, 3f, 0f, 2f, -2f, -6f, -7f, -8f),
            "jazz" to floatArrayOf(-4f, -3f, -1f, 0f, 4f, 2f, 2f, 0f, 3f, 3f),
            "slow" to floatArrayOf(-4f, -4f, -2f, -2f, 4f, 2f, 5f, 5f, 0f, 0f),
            "electro" to floatArrayOf(7f, 4f, 0f, -2f, -4f, -3f, 0f, 0f, 2f, 5f),
            "rock" to floatArrayOf(7f, 5f, 1f, 0f, -3f, -4f, 2f, 0f, 3f, 5f),
            "country" to floatArrayOf(4f, 4f, 2f, 0f, -2f, -2f, 0f, 2f, 4f, 4f),
            "close" to floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)
        )
    }

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null

    var isEnabled: Boolean = false
        private set

    var activeMode: EqEngineMode = EqEngineMode.VLC
        private set

    var currentVlcGains: FloatArray = DEFAULT_VLC_GAINS.clone()
        private set

    var currentVivoGains: FloatArray = VIVO_OFFICIAL_PRESETS["custom"]!!.clone()
        private set

    var activeVivoPresetKey: String = "custom"
        private set

    var bassBoostDb: Float = 0f
        private set

    var preampDb: Float = 14.1f
        private set

    fun attachAudioSession(audioSessionId: Int) {
        try {
            release()
            if (audioSessionId != 0) {
                equalizer = Equalizer(0, audioSessionId).apply {
                    enabled = this@DspEngine.isEnabled
                }
                bassBoost = BassBoost(0, audioSessionId).apply {
                    enabled = this@DspEngine.isEnabled
                    if (this@DspEngine.isEnabled) {
                        setStrength((bassBoostDb * 66.6f).toInt().coerceIn(0, 1000).toShort())
                    }
                }
                applyCurrentGainsToHardware()
            }
        } catch (e: Exception) {
            Log.w("DspEngine", "Equalizer attach error: ${e.message}")
        }
    }

    fun setDspEnabled(enabled: Boolean, onVolumeAdjust: (Float) -> Unit) {
        isEnabled = enabled
        equalizer?.enabled = enabled
        bassBoost?.enabled = enabled

        if (enabled) {
            // Auto gain reduction: 20% master volume in DSP mode to prevent digital clipping
            onVolumeAdjust(0.20f)
            applyCurrentGainsToHardware()
        } else {
            // Flat response: 100% volume
            onVolumeAdjust(1.0f)
            equalizer?.let { eq ->
                val numBands = eq.numberOfBands.toInt()
                for (i in 0 until numBands) {
                    eq.setBandLevel(i.toShort(), 0)
                }
            }
            bassBoost?.setStrength(0)
        }
    }

    fun setEngineMode(mode: EqEngineMode) {
        activeMode = mode
        if (isEnabled) {
            applyCurrentGainsToHardware()
        }
    }

    fun updateVlcBand(bandIndex: Int, gainDb: Float) {
        if (bandIndex in currentVlcGains.indices) {
            currentVlcGains[bandIndex] = gainDb
            if (isEnabled && activeMode == EqEngineMode.VLC) {
                applyCurrentGainsToHardware()
            }
        }
    }

    fun updateVivoBand(bandIndex: Int, gainDb: Float) {
        if (bandIndex in currentVivoGains.indices) {
            currentVivoGains[bandIndex] = gainDb
            activeVivoPresetKey = "custom"
            if (isEnabled && activeMode == EqEngineMode.VIVO) {
                applyCurrentGainsToHardware()
            }
        }
    }

    fun applyVivoPreset(key: String, customGains: FloatArray? = null) {
        activeVivoPresetKey = key
        val targetGains = customGains ?: VIVO_OFFICIAL_PRESETS[key] ?: VIVO_OFFICIAL_PRESETS["custom"]!!
        currentVivoGains = targetGains.clone()
        if (isEnabled && activeMode == EqEngineMode.VIVO) {
            applyCurrentGainsToHardware()
        }
    }

    fun setBassBoost(gainDb: Float) {
        bassBoostDb = gainDb
        if (isEnabled) {
            bassBoost?.setStrength((gainDb * 66.6f).toInt().coerceIn(0, 1000).toShort())
        }
    }

    fun setPreamp(gainDb: Float) {
        preampDb = gainDb
    }

    fun resetToDefaults(onVolumeAdjust: (Float) -> Unit) {
        currentVlcGains = DEFAULT_VLC_GAINS.clone()
        currentVivoGains = VIVO_OFFICIAL_PRESETS["close"]!!.clone()
        activeVivoPresetKey = "close"
        bassBoostDb = 0f
        preampDb = 14.1f
        setDspEnabled(false, onVolumeAdjust)
    }

    private fun applyCurrentGainsToHardware() {
        val eq = equalizer ?: return
        val numBands = eq.numberOfBands.toInt()
        val minLevel = eq.bandLevelRange[0]
        val maxLevel = eq.bandLevelRange[1]

        val activeGains = if (activeMode == EqEngineMode.VLC) currentVlcGains else currentVivoGains

        for (i in 0 until numBands) {
            val gainIdx = (i.toFloat() / numBands * activeGains.size).toInt().coerceIn(0, activeGains.size - 1)
            val db = activeGains[gainIdx]
            val mB = (db * 100).toInt().coerceIn(minLevel.toInt(), maxLevel.toInt()).toShort()
            try {
                eq.setBandLevel(i.toShort(), mB)
            } catch (e: Exception) {
                Log.w("DspEngine", "Failed to set band level for index $i: ${e.message}")
            }
        }
    }

    fun release() {
        try {
            equalizer?.release()
            bassBoost?.release()
        } catch (e: Exception) {}
        equalizer = null
        bassBoost = null
    }
}
