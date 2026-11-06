package ru.viktorinavopros.quiz.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ru.viktorinavopros.quiz.data.QuizData
import ru.viktorinavopros.quiz.data.SiteRepository
import ru.viktorinavopros.quiz.data.Topic
import ru.viktorinavopros.quiz.game.GameFactory
import ru.viktorinavopros.quiz.game.GameState
import kotlin.random.Random

sealed interface Screen {
    data object Intro : Screen
    data object Categories : Screen
    data object Board : Screen
    data object Results : Screen
}

sealed interface DataState {
    data object Loading : DataState
    data class Ready(val data: QuizData) : DataState
    data class Error(val message: String) : DataState
}

data class AppUiState(
    val screen: Screen = Screen.Intro,
    val dataState: DataState = DataState.Loading,
    val refreshing: Boolean = false,
    val errorMessage: String? = null,
    val playerCount: Int = 1,
    val playerNames: List<String> = List(1) { "" },
    val selectedTopics: List<Topic> = emptyList(),
    val game: GameState? = null,
    val timerSeconds: Int = 0,
    val lastUpdated: Long? = null,
)

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private companion object {
        const val SETTINGS = "game_settings"
        const val KEY_TIMER_SECONDS = "timer_seconds"
        val TIMER_OPTIONS = setOf(0, 30, 60)
    }

    private val repository = SiteRepository(application)
    private val settings = application.getSharedPreferences(SETTINGS, Context.MODE_PRIVATE)
    private val _state = MutableStateFlow(
        AppUiState(timerSeconds = loadTimerSeconds()),
    )
    val state: StateFlow<AppUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val cached = repository.loadCached()
            if (cached != null) {
                _state.update { current ->
                    current.copy(
                        dataState = DataState.Ready(cached),
                        lastUpdated = cached.fetchedAt,
                        selectedTopics = ensureTopics(cached, current.selectedTopics),
                    )
                }
            }
            load()
        }
    }

    fun refresh() {
        if (_state.value.refreshing) return
        load()
    }

    private fun load() {
        viewModelScope.launch {
            _state.update { it.copy(refreshing = true, errorMessage = null) }
            try {
                val data = repository.refresh()
                _state.update { current ->
                    current.copy(
                        dataState = DataState.Ready(data),
                        refreshing = false,
                        lastUpdated = data.fetchedAt,
                        selectedTopics = ensureTopics(data, current.selectedTopics),
                    )
                }
            } catch (error: Throwable) {
                _state.update { current ->
                    val hasData = current.dataState is DataState.Ready
                    current.copy(
                        refreshing = false,
                        dataState = if (hasData) {
                            current.dataState
                        } else {
                            DataState.Error(
                                error.message?.takeIf { it.isNotBlank() }
                                    ?: "Не удалось загрузить вопросы с сайта",
                            )
                        },
                        errorMessage = if (hasData) {
                            "Не удалось обновить вопросы. Показаны сохранённые данные."
                        } else {
                            null
                        },
                    )
                }
            }
        }
    }

    fun dismissError() {
        _state.update { it.copy(errorMessage = null) }
    }

    fun goToCategories() {
        val data = readyData() ?: return
        _state.update { current ->
            current.copy(
                screen = Screen.Categories,
                selectedTopics = ensureTopics(data, current.selectedTopics),
            )
        }
    }

    fun setPlayerCount(count: Int) {
        val safeCount = count.coerceIn(GameFactory.MIN_PLAYERS, GameFactory.MAX_PLAYERS)
        _state.update { current ->
            val names = current.playerNames.toMutableList()
            while (names.size < safeCount) names += ""
            while (names.size > safeCount) names.removeAt(names.lastIndex)
            current.copy(playerCount = safeCount, playerNames = names)
        }
    }

    fun setPlayerName(index: Int, name: String) {
        _state.update { current ->
            if (index !in current.playerNames.indices) return@update current
            val names = current.playerNames.toMutableList()
            names[index] = name.take(24)
            current.copy(playerNames = names)
        }
    }

    fun setTimerSeconds(seconds: Int) {
        val safeSeconds = seconds.takeIf { it in TIMER_OPTIONS } ?: 0
        settings.edit().putInt(KEY_TIMER_SECONDS, safeSeconds).apply()
        _state.update { it.copy(timerSeconds = safeSeconds) }
    }

    fun toggleTopic(topic: Topic) {
        _state.update { current ->
            val selected = current.selectedTopics
            when {
                topic in selected -> current.copy(selectedTopics = selected - topic)
                selected.size < GameFactory.CATEGORIES_PER_GAME -> {
                    current.copy(selectedTopics = selected + topic)
                }
                else -> current.copy(
                    errorMessage = "Можно выбрать не больше ${GameFactory.CATEGORIES_PER_GAME} категорий",
                )
            }
        }
    }

    fun randomTopics() {
        val data = readyData() ?: return
        val available = GameFactory.availableTopics(data)
        val picked = available
            .shuffled(Random.Default)
            .take(GameFactory.CATEGORIES_PER_GAME)
            .map { it.topic }
        _state.update { it.copy(selectedTopics = picked) }
    }

    fun startGame() {
        val data = readyData() ?: return
        val current = _state.value
        if (current.selectedTopics.size !in GameFactory.MIN_CATEGORIES..GameFactory.CATEGORIES_PER_GAME) {
            _state.update {
                it.copy(
                    errorMessage = "Выберите от ${GameFactory.MIN_CATEGORIES} до " +
                        "${GameFactory.CATEGORIES_PER_GAME} категорий",
                )
            }
            return
        }
        val players = GameFactory.createPlayers(current.playerCount, current.playerNames)
        val game = GameFactory.createGame(
            data = data,
            topics = current.selectedTopics,
            players = players,
            timerSeconds = current.timerSeconds,
        )
        _state.update {
            it.copy(
                screen = Screen.Board,
                game = game,
                errorMessage = null,
            )
        }
    }

    fun openCell(cellId: String) {
        val game = _state.value.game ?: return
        _state.update { it.copy(game = game.openCell(cellId)) }
    }

    fun answer(text: String) {
        val game = _state.value.game ?: return
        _state.update { it.copy(game = game.answer(text)) }
    }

    fun timeout() {
        val game = _state.value.game ?: return
        _state.update { it.copy(game = game.timeout()) }
    }

    fun continueGame() {
        val game = _state.value.game ?: return
        val next = game.continueAfterAnswer()
        _state.update {
            it.copy(
                game = next,
                screen = if (next.finished) Screen.Results else Screen.Board,
            )
        }
    }

    fun finishGame() {
        val game = _state.value.game ?: return
        _state.update {
            it.copy(
                game = game.finish(),
                screen = Screen.Results,
            )
        }
    }

    fun playAgain() {
        val data = readyData() ?: return
        val game = _state.value.game ?: return
        val topics = game.categories.map { it.topic }
        val players = GameFactory.createPlayers(
            count = game.players.size,
            names = game.players.map { it.name },
        )
        val newGame = GameFactory.createGame(
            data = data,
            topics = topics,
            players = players,
            timerSeconds = game.timerSeconds,
        )
        _state.update {
            it.copy(
                game = newGame,
                screen = Screen.Board,
                errorMessage = null,
            )
        }
    }

    fun newGame() {
        val data = readyData()
        _state.update {
            it.copy(
                screen = Screen.Intro,
                game = null,
                selectedTopics = data?.let { current -> GameFactory.defaultTopics(current) }
                    ?: emptyList(),
                errorMessage = null,
            )
        }
    }

    fun back() {
        when (_state.value.screen) {
            Screen.Categories -> _state.update { it.copy(screen = Screen.Intro) }
            Screen.Results -> _state.update { it.copy(screen = Screen.Intro, game = null) }
            else -> Unit
        }
    }

    private fun loadTimerSeconds(): Int =
        settings.getInt(KEY_TIMER_SECONDS, 0).takeIf { it in TIMER_OPTIONS } ?: 0

    private fun readyData(): QuizData? = (_state.value.dataState as? DataState.Ready)?.data

    private fun ensureTopics(data: QuizData, current: List<Topic>): List<Topic> {
        val available = GameFactory.availableTopics(data).map { it.topic }.toSet()
        val valid = current.filter { it in available }.distinct()
        return if (valid.size in GameFactory.MIN_CATEGORIES..GameFactory.CATEGORIES_PER_GAME) {
            valid
        } else {
            GameFactory.defaultTopics(data)
        }
    }
}
