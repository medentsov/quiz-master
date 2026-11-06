package ru.viktorinavopros.quiz.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.speech.tts.TextToSpeech
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import ru.viktorinavopros.quiz.R
import java.util.Locale

/**
 * Звуковое оформление: спокойный фоновый эмбиент, тиканье таймера,
 * короткие реакции хомяка и голос.
 * Живёт столько же, сколько процесс приложения.
 */
object AppSounds {

    private const val PREFS = "sound_settings"
    private const val KEY_MUTED = "muted"
    private const val KEY_AMBIENT = "ambient_enabled"
    private const val KEY_AMBIENT_LEGACY = "rain_enabled"

    private var ambientPlayer: MediaPlayer? = null
    private var soundPool: SoundPool? = null
    private var crowdId = 0
    private var chimeId = 0
    private var tickId = 0
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var prefs: android.content.SharedPreferences? = null

    /** Общий выключатель: эмбиент, тиканье, реакции и голос. */
    var muted by mutableStateOf(false)
        private set

    /** Фоновый эмбиент по умолчанию выключен: игрок включает его сам. */
    var ambientEnabled by mutableStateOf(false)
        private set

    fun init(context: Context) {
        if (ambientPlayer != null || soundPool != null) return
        val app = context.applicationContext
        val store = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs = store
        muted = store.getBoolean(KEY_MUTED, false)
        ambientEnabled = store.getBoolean(
            KEY_AMBIENT,
            store.getBoolean(KEY_AMBIENT_LEGACY, false),
        )

        soundPool = SoundPool.Builder()
            .setMaxStreams(4)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            .build()
            .also { pool ->
                crowdId = pool.load(app, R.raw.wrong_crowd, 1)
                chimeId = pool.load(app, R.raw.correct_chime, 1)
                tickId = pool.load(app, R.raw.clock_tick, 1)
            }

        ambientPlayer = runCatching {
            MediaPlayer.create(app, R.raw.ambient_loop)?.apply {
                isLooping = true
                setVolume(0.22f, 0.22f)
                if (!muted && ambientEnabled) start()
            }
        }.getOrNull()

        tts = runCatching {
            TextToSpeech(app) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    runCatching {
                        val result = tts?.setLanguage(Locale("ru", "RU"))
                        ttsReady = result != TextToSpeech.LANG_MISSING_DATA &&
                            result != TextToSpeech.LANG_NOT_SUPPORTED
                        tts?.setPitch(1.28f)
                        tts?.setSpeechRate(0.96f)
                    }
                }
            }
        }.getOrNull()
    }

    /** Включает или выключает весь звук приложения. */
    fun toggleMute() {
        muted = !muted
        prefs?.edit()?.putBoolean(KEY_MUTED, muted)?.apply()
        if (muted) {
            runCatching {
                if (ambientPlayer?.isPlaying == true) ambientPlayer?.pause()
                tts?.stop()
            }
        } else {
            applyAmbient()
        }
    }

    fun changeAmbient(enabled: Boolean) {
        ambientEnabled = enabled
        prefs?.edit()?.putBoolean(KEY_AMBIENT, enabled)?.apply()
        if (enabled && muted) {
            muted = false
            prefs?.edit()?.putBoolean(KEY_MUTED, false)?.apply()
        }
        applyAmbient()
    }

    private fun applyAmbient() {
        val player = ambientPlayer ?: return
        runCatching {
            if (ambientEnabled && !muted) {
                if (!player.isPlaying) player.start()
            } else {
                if (player.isPlaying) player.pause()
            }
        }
    }

    /** Ненавязчивый тик часов во время отсчёта таймера. */
    fun onTick() {
        if (muted) return
        soundPool?.play(tickId, 0.34f, 0.34f, 1, 0, 1f)
    }

    fun onCorrect() {
        if (muted) return
        soundPool?.play(chimeId, 0.75f, 0.75f, 1, 0, 1f)
        speak("Ну да!")
    }

    fun onWrong() {
        if (muted) return
        soundPool?.play(crowdId, 0.85f, 0.85f, 1, 0, 1f)
        speak("А ну да?")
    }

    private fun speak(text: String) {
        if (muted || !ttsReady) return
        runCatching {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "hamster")
        }
    }

    fun release() {
        runCatching { ambientPlayer?.release() }
        ambientPlayer = null
        runCatching { soundPool?.release() }
        soundPool = null
        runCatching { tts?.shutdown() }
        tts = null
        ttsReady = false
    }
}
