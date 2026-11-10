package ru.viktorinavopros.quiz.audio

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import androidx.annotation.RawRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import ru.viktorinavopros.quiz.R

/** Вариант звука реакции на ответ. Название берётся из имени исходного файла. */
data class AnswerSound(
    val id: String,
    val label: String,
    @param:RawRes val resId: Int,
)

/**
 * Звуковое оформление: бесшовный фоновый эмбиент, тиканье таймера
 * и выбранные игроком звуки верного и неверного ответа.
 * Живёт столько же, сколько процесс приложения.
 */
object AppSounds {

    private const val PREFS = "sound_settings"
    private const val KEY_MUTED = "muted"
    private const val KEY_AMBIENT = "ambient_enabled"
    private const val KEY_AMBIENT_LEGACY = "rain_enabled"
    private const val KEY_CORRECT_SOUND = "correct_sound"
    private const val KEY_WRONG_SOUND = "wrong_sound"

    val correctSounds = listOf(
        AnswerSound("lucky", "О счастливчик", R.raw.correct_lucky),
        AnswerSound("hundred", "Сто к одному", R.raw.correct_hundred),
        AnswerSound("field", "Поле чудес", R.raw.correct_field),
    )

    val wrongSounds = listOf(
        AnswerSound("hundred", "Сто к одному", R.raw.wrong_hundred),
        AnswerSound("basin", "Тазик", R.raw.wrong_basin),
        AnswerSound("sigh", "Да блин, нууу...", R.raw.wrong_sigh),
    )

    private var appContext: Context? = null
    private var ambient: AmbientLoop? = null
    private var correctPlayer: MediaPlayer? = null
    private var wrongPlayer: MediaPlayer? = null
    private var soundPool: SoundPool? = null
    private var tickId = 0
    private var prefs: SharedPreferences? = null
    private var initialized = false

    /** Общий выключатель: эмбиент, тиканье и звуки ответов. */
    var muted by mutableStateOf(false)
        private set

    /** Фоновый эмбиент по умолчанию выключен: игрок включает его сам. */
    var ambientEnabled by mutableStateOf(false)
        private set

    /** Выбранный звук верного ответа. */
    var correctSound by mutableStateOf(correctSounds.first())
        private set

    /** Выбранный звук неверного ответа. */
    var wrongSound by mutableStateOf(wrongSounds.first())
        private set

    fun init(context: Context) {
        if (initialized) return
        initialized = true

        val app = context.applicationContext
        appContext = app
        val store = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs = store
        muted = store.getBoolean(KEY_MUTED, false)
        ambientEnabled = store.getBoolean(
            KEY_AMBIENT,
            store.getBoolean(KEY_AMBIENT_LEGACY, false),
        )
        correctSound = correctSounds.firstOrNull {
            it.id == store.getString(KEY_CORRECT_SOUND, null)
        } ?: correctSounds.first()
        wrongSound = wrongSounds.firstOrNull {
            it.id == store.getString(KEY_WRONG_SOUND, null)
        } ?: wrongSounds.first()

        soundPool = SoundPool.Builder()
            .setMaxStreams(2)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            .build()
            .also { pool -> tickId = pool.load(app, R.raw.clock_tick, 1) }

        prepareAnswerPlayers()
        ambient = AmbientLoop(app, R.raw.ambient_loop)
        if (!muted && ambientEnabled) ambient?.start()
    }

    /** Включает или выключает весь звук приложения. */
    fun toggleMute() {
        muted = !muted
        prefs?.edit()?.putBoolean(KEY_MUTED, muted)?.apply()
        if (muted) {
            ambient?.pause()
            pauseEffect(correctPlayer)
            pauseEffect(wrongPlayer)
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

    fun selectCorrectSound(sound: AnswerSound) {
        if (sound.id == correctSound.id) return
        correctSound = sound
        prefs?.edit()?.putString(KEY_CORRECT_SOUND, sound.id)?.apply()
        prepareAnswerPlayers()
    }

    fun selectWrongSound(sound: AnswerSound) {
        if (sound.id == wrongSound.id) return
        wrongSound = sound
        prefs?.edit()?.putString(KEY_WRONG_SOUND, sound.id)?.apply()
        prepareAnswerPlayers()
    }

    private fun applyAmbient() {
        if (ambientEnabled && !muted) ambient?.start() else ambient?.pause()
    }

    private fun prepareAnswerPlayers() {
        val context = appContext ?: return
        correctPlayer?.release()
        wrongPlayer?.release()
        correctPlayer = createEffect(context, correctSound.resId)
        wrongPlayer = createEffect(context, wrongSound.resId)
    }

    private fun createEffect(context: Context, @RawRes resId: Int): MediaPlayer? =
        runCatching {
            MediaPlayer.create(context, resId)?.apply { setVolume(1f, 1f) }
        }.getOrNull()

    private fun pauseEffect(player: MediaPlayer?) {
        runCatching { if (player?.isPlaying == true) player.pause() }
    }

    private fun playEffect(player: MediaPlayer?, other: MediaPlayer?) {
        if (muted) return
        runCatching {
            pauseEffect(other)
            player ?: return
            if (player.isPlaying) player.pause()
            player.seekTo(0)
            player.start()
        }
    }

    /** Ненавязчивый тик часов во время отсчёта таймера. */
    fun onTick() {
        if (muted) return
        soundPool?.play(tickId, 0.34f, 0.34f, 1, 0, 1f)
    }

    fun onCorrect() {
        playEffect(correctPlayer, wrongPlayer)
    }

    fun onWrong() {
        playEffect(wrongPlayer, correctPlayer)
    }

    fun release() {
        ambient?.release()
        ambient = null
        correctPlayer?.release()
        correctPlayer = null
        wrongPlayer?.release()
        wrongPlayer = null
        soundPool?.release()
        soundPool = null
        appContext = null
        initialized = false
    }
}

/**
 * Бесшовный цикл из двух MediaPlayer: пока играет один, второй уже подготовлен
 * и назначен следующим. Переход происходит без паузы между повторами.
 */
private class AmbientLoop(context: Context, @RawRes resId: Int) {

    private var first: MediaPlayer? = null
    private var second: MediaPlayer? = null
    private var active: MediaPlayer? = null
    private var standby: MediaPlayer? = null

    init {
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()

        first = create(context, resId, attributes, 0)
        val sessionId = first?.audioSessionId ?: 0
        second = create(context, resId, attributes, sessionId)
        active = first
        standby = second

        first?.setOnCompletionListener { player ->
            second?.let { onCompleted(player, it) }
        }
        second?.setOnCompletionListener { player ->
            first?.let { onCompleted(player, it) }
        }

        runCatching {
            val current = first ?: return@runCatching
            val next = second ?: return@runCatching
            current.setNextMediaPlayer(next)
        }
    }

    private fun create(
        context: Context,
        @RawRes resId: Int,
        attributes: AudioAttributes,
        sessionId: Int,
    ): MediaPlayer? = runCatching {
        MediaPlayer.create(context, resId, attributes, sessionId)?.apply {
            setVolume(VOLUME, VOLUME)
        }
    }.getOrNull()

    private fun onCompleted(finished: MediaPlayer, next: MediaPlayer) {
        active = next
        standby = finished
        runCatching {
            finished.seekTo(0)
            next.setNextMediaPlayer(finished)
        }
    }

    fun start() {
        val current = active ?: return
        val next = standby ?: return
        runCatching {
            if (!current.isPlaying) {
                current.setNextMediaPlayer(next)
                current.start()
            }
        }
    }

    fun pause() {
        runCatching { if (active?.isPlaying == true) active?.pause() }
    }

    fun release() {
        runCatching { first?.release() }
        runCatching { second?.release() }
        first = null
        second = null
        active = null
        standby = null
    }

    private companion object {
        const val VOLUME = 0.22f
    }
}
