package ru.viktorinavopros.quiz.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.viktorinavopros.quiz.ui.components.EmptyState
import ru.viktorinavopros.quiz.ui.components.HamsterBadge
import ru.viktorinavopros.quiz.ui.components.JeopardyBackground
import ru.viktorinavopros.quiz.ui.components.SoundToggle
import ru.viktorinavopros.quiz.ui.screens.BoardScreen
import ru.viktorinavopros.quiz.ui.screens.CategorySelectScreen
import ru.viktorinavopros.quiz.ui.screens.IntroScreen
import ru.viktorinavopros.quiz.ui.screens.ResultsScreen

@Composable
fun ViktorinaApp(viewModel: AppViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.dismissError()
        }
    }

    BackHandler(
        enabled = state.screen == Screen.Categories || state.screen == Screen.Results,
    ) {
        viewModel.back()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = state.screen,
            transitionSpec = {
                fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(160))
            },
            label = "screen",
        ) { screen ->
            when (screen) {
                Screen.Intro -> IntroScreen(
                    dataState = state.dataState,
                    refreshing = state.refreshing,
                    playerCount = state.playerCount,
                    playerNames = state.playerNames,
                    lastUpdated = state.lastUpdated,
                    onPlayerCountChange = viewModel::setPlayerCount,
                    onPlayerNameChange = viewModel::setPlayerName,
                    onStart = viewModel::goToCategories,
                    onRetry = viewModel::refresh,
                )

                Screen.Categories -> CategorySelectScreen(
                    dataState = state.dataState,
                    selectedTopics = state.selectedTopics,
                    onToggleTopic = viewModel::toggleTopic,
                    onRandomTopics = viewModel::randomTopics,
                    onStart = viewModel::startGame,
                    onBack = viewModel::back,
                    onRetry = viewModel::refresh,
                )

                Screen.Board -> {
                    val game = state.game
                    if (game != null) {
                        BoardScreen(
                            game = game,
                            onOpenCell = viewModel::openCell,
                            onAnswer = viewModel::answer,
                            onContinue = viewModel::continueGame,
                            onFinish = viewModel::finishGame,
                        )
                    } else {
                        JeopardyBackground {
                            EmptyState(
                                emoji = "🎲",
                                title = "Игра не найдена",
                                subtitle = "Вернитесь на главный экран и начните заново.",
                            )
                        }
                    }
                }

                Screen.Results -> {
                    val game = state.game
                    if (game != null) {
                        ResultsScreen(
                            game = game,
                            onPlayAgain = viewModel::playAgain,
                            onNewGame = viewModel::newGame,
                        )
                    } else {
                        JeopardyBackground {
                            EmptyState(
                                emoji = "🏁",
                                title = "Результатов пока нет",
                                subtitle = "Начните новую игру, чтобы увидеть итоги.",
                            )
                        }
                    }
                }
            }
        }

        HamsterBadge(
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(start = 10.dp, top = 6.dp),
        )

        SoundToggle(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .statusBarsPadding()
                .padding(end = 10.dp, top = 6.dp),
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(16.dp),
        )
    }
}
