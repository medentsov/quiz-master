package ru.viktorinavopros.quiz.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.viktorinavopros.quiz.audio.AppSounds
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ru.viktorinavopros.quiz.game.BoardCell
import ru.viktorinavopros.quiz.game.BoardCategory
import ru.viktorinavopros.quiz.game.GameState
import ru.viktorinavopros.quiz.game.Player
import ru.viktorinavopros.quiz.ui.components.HamsterReaction
import ru.viktorinavopros.quiz.ui.components.JeopardyBackground
import ru.viktorinavopros.quiz.ui.components.JeopardyButton
import ru.viktorinavopros.quiz.ui.components.PlayerDot
import ru.viktorinavopros.quiz.ui.components.PlayerScoreChip
import ru.viktorinavopros.quiz.ui.components.ReactionTone
import ru.viktorinavopros.quiz.ui.components.RemoteImage
import ru.viktorinavopros.quiz.ui.components.formatScore
import ru.viktorinavopros.quiz.ui.theme.JeopardyBlue
import ru.viktorinavopros.quiz.ui.theme.JeopardyCell
import ru.viktorinavopros.quiz.ui.theme.JeopardyCellDark
import ru.viktorinavopros.quiz.ui.theme.JeopardyGold
import ru.viktorinavopros.quiz.ui.theme.JeopardyGreen
import ru.viktorinavopros.quiz.ui.theme.JeopardyGreenDark
import ru.viktorinavopros.quiz.ui.theme.JeopardyHeader
import ru.viktorinavopros.quiz.ui.theme.JeopardyHeaderDark
import ru.viktorinavopros.quiz.ui.theme.JeopardyMuted
import ru.viktorinavopros.quiz.ui.theme.JeopardyNavy
import ru.viktorinavopros.quiz.ui.theme.JeopardyRed
import ru.viktorinavopros.quiz.ui.theme.JeopardyRedDark
import kotlinx.coroutines.delay

@Composable
fun BoardScreen(
    game: GameState,
    onOpenCell: (String) -> Unit,
    onAnswer: (Int) -> Unit,
    onContinue: () -> Unit,
    onFinish: () -> Unit,
) {
    var showExitDialog by remember { mutableStateOf(false) }

    BackHandler { showExitDialog = true }

    JeopardyBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            BoardTopBar(
                game = game,
                onClose = { showExitDialog = true },
            )

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(game.players, key = { it.id }) { player ->
                    PlayerScoreChip(
                        name = player.name,
                        score = player.score,
                        color = player.color,
                        current = player.id == game.currentPlayer.id,
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text(
                        text = "Игровое табло",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                    )
                    Text(
                        text = "Выберите категорию и стоимость вопроса",
                        style = MaterialTheme.typography.bodyMedium,
                        color = JeopardyMuted,
                        fontSize = 12.sp,
                    )
                }
                Text(
                    text = "${game.answeredCells} / ${game.totalCells}",
                    style = MaterialTheme.typography.labelLarge,
                    color = JeopardyGold,
                )
            }

            Spacer(Modifier.height(10.dp))

            val columns = if (game.categories.size == 1) 1 else 2
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(
                    start = 12.dp,
                    end = 12.dp,
                    bottom = 16.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(game.categories.chunked(columns), key = { row -> row.joinToString { it.topic.name } }) { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        for (category in row) {
                            CategoryBlock(
                                category = category,
                                modifier = Modifier.weight(1f),
                                onOpenCell = onOpenCell,
                            )
                        }
                        if (row.size < columns) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }

                item {
                    Text(
                        text = "После каждого вопроса ход переходит следующему игроку. " +
                            "Верный ответ приносит стоимость вопроса, ошибка отнимает её.",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = JeopardyMuted.copy(alpha = 0.65f),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                    )
                }

                item {
                    TextButton(
                        onClick = { showExitDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = "Завершить игру и посмотреть итоги",
                            color = JeopardyMuted,
                        )
                    }
                }
            }
        }

        game.activeCell?.let { cell ->
            QuestionDialog(
                game = game,
                cell = cell,
                onAnswer = onAnswer,
                onContinue = onContinue,
            )
        }
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = {
                Text(text = "Завершить игру?", color = Color.White)
            },
            text = {
                Text(
                    text = "Отвечено ${game.answeredCells} из ${game.totalCells} вопросов. " +
                        "Все результаты будут показаны на итоговом экране.",
                    color = JeopardyMuted,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showExitDialog = false
                        onFinish()
                    },
                ) {
                    Text(text = "Завершить", color = JeopardyGold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text(text = "Продолжить", color = Color.White)
                }
            },
        )
    }
}

@Composable
private fun BoardTopBar(
    game: GameState,
    onClose: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 70.dp, end = 70.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "СВОЯ ИГРА",
                style = MaterialTheme.typography.labelMedium,
                color = JeopardyGold,
                letterSpacing = 1.8.sp,
            )
            Text(
                text = "Ход: ${game.currentPlayer.name}",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        Surface(
            onClick = { AppSounds.toggleRain() },
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.07f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
        ) {
            Box(
                modifier = Modifier.size(42.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (AppSounds.rainEnabled) {
                        Icons.Default.VolumeUp
                    } else {
                        Icons.Default.VolumeOff
                    },
                    contentDescription = if (AppSounds.rainEnabled) {
                        "Выключить дождь"
                    } else {
                        "Включить дождь"
                    },
                    tint = if (AppSounds.rainEnabled) JeopardyGold else JeopardyMuted,
                )
            }
        }
        Spacer(Modifier.width(8.dp))
        Surface(
            onClick = onClose,
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.07f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
        ) {
            Box(
                modifier = Modifier.size(42.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Завершить игру",
                    tint = Color.White,
                )
            }
        }
    }
}

@Composable
private fun CategoryBlock(
    category: BoardCategory,
    modifier: Modifier = Modifier,
    onOpenCell: (String) -> Unit,
) {
    Column(
        modifier = modifier
            .background(JeopardyBlue.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
            .padding(4.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .background(
                    Brush.verticalGradient(listOf(JeopardyHeader, JeopardyHeaderDark)),
                    RoundedCornerShape(topStart = 9.dp, topEnd = 9.dp),
                )
                .padding(horizontal = 6.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = category.topic.title.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Black,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 2,
                fontSize = 11.sp,
                lineHeight = 13.sp,
            )
        }
        Spacer(Modifier.height(4.dp))
        for (cell in category.cells) {
            QuestionCell(cell = cell, onOpen = { onOpenCell(cell.id) })
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
private fun QuestionCell(
    cell: BoardCell,
    onOpen: () -> Unit,
) {
    val background = when {
        !cell.answered -> JeopardyCell
        cell.isCorrect == true -> JeopardyGreenDark
        else -> JeopardyRedDark
    }
    val borderColor = when {
        !cell.answered -> JeopardyCellDark
        cell.isCorrect == true -> JeopardyGreen
        else -> JeopardyRed
    }
    Surface(
        onClick = onOpen,
        enabled = !cell.answered,
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp),
        shape = RoundedCornerShape(7.dp),
        color = background,
        border = BorderStroke(1.dp, borderColor.copy(alpha = 0.75f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (cell.answered) {
                Icon(
                    imageVector = if (cell.isCorrect == true) Icons.Default.CheckCircle else Icons.Default.Cancel,
                    contentDescription = null,
                    tint = if (cell.isCorrect == true) JeopardyGreen else JeopardyRed,
                    modifier = Modifier.size(15.dp),
                )
                Spacer(Modifier.width(4.dp))
            }
            Text(
                text = cell.value.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = if (cell.answered) Color.White.copy(alpha = 0.75f) else JeopardyGold,
                fontSize = 15.sp,
            )
        }
    }
}

@Composable
private fun QuestionDialog(
    game: GameState,
    cell: BoardCell,
    onAnswer: (Int) -> Unit,
    onContinue: () -> Unit,
) {
    val answered = cell.answered
    val answeringPlayer = cell.answeredBy
        ?.let { id -> game.players.firstOrNull { it.id == id } }
        ?: game.currentPlayer
    val scrollState = rememberScrollState()
    val reaction = remember { Animatable(0f) }

    // После ответа прокручиваем вниз, чтобы был виден правильный ответ.
    LaunchedEffect(answered) {
        if (answered) {
            delay(120)
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    LaunchedEffect(answered, cell.isCorrect) {
        if (answered) {
            if (cell.isCorrect == true) AppSounds.onCorrect() else AppSounds.onWrong()
            reaction.snapTo(0f)
            reaction.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 1150, easing = LinearEasing),
            )
        }
    }

    val tone = when {
        !answered -> ReactionTone.NEUTRAL
        cell.isCorrect == true -> ReactionTone.CORRECT
        else -> ReactionTone.WRONG
    }
    val toneProgress = if (answered) {
        (reaction.value / 0.30f).coerceIn(0f, 1f)
    } else {
        0f
    }

    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
        ),
    ) {
        JeopardyBackground(tone = tone, toneProgress = toneProgress) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = cell.topic.title.uppercase(),
                            style = MaterialTheme.typography.labelMedium,
                            color = JeopardyGold,
                            letterSpacing = 1.4.sp,
                        )
                        Text(
                            text = "Вопрос за ${cell.value}",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(answeringPlayer.color).copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, Color(answeringPlayer.color).copy(alpha = 0.5f)),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            PlayerDot(color = answeringPlayer.color, size = 9.dp)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = answeringPlayer.name,
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                maxLines = 1,
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = formatScore(answeringPlayer.score),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Black,
                                color = JeopardyGold,
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(scrollState)
                        .padding(horizontal = 18.dp),
                ) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Color.White.copy(alpha = 0.055f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.09f)),
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            val imageUrl = cell.question.imageUrl
                            if (!imageUrl.isNullOrBlank()) {
                                RemoteImage(
                                    url = imageUrl,
                                    contentDescription = "Картинка к вопросу",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(210.dp),
                                )
                                Spacer(Modifier.height(14.dp))
                            }
                            Text(
                                text = cell.question.text,
                                style = MaterialTheme.typography.headlineMedium,
                                color = Color.White,
                                lineHeight = 32.sp,
                            )
                        }
                    }

                    Spacer(Modifier.height(18.dp))

                    Text(
                        text = if (answered) "Ваш ответ" else "Выберите вариант ответа",
                        style = MaterialTheme.typography.labelLarge,
                        color = JeopardyMuted,
                    )
                    Spacer(Modifier.height(8.dp))

                    cell.options.forEachIndexed { index, option ->
                        OptionCard(
                            index = index,
                            text = option,
                            answered = answered,
                            selected = cell.selectedIndex == index,
                            correct = index == cell.correctIndex,
                            onClick = { onAnswer(index) },
                        )
                        Spacer(Modifier.height(8.dp))
                    }

                    if (answered) {
                        Spacer(Modifier.height(10.dp))
                        AnswerFeedback(correctAnswer = cell.question.answer)
                    }

                    Spacer(Modifier.height(18.dp))
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                ) {
                    if (answered) {
                        ReactionPanel(
                            correct = cell.isCorrect == true,
                            progress = reaction.value,
                            value = cell.value,
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                    JeopardyButton(
                        text = if (answered) "Продолжить" else "Сначала выберите ответ",
                        onClick = onContinue,
                        enabled = answered,
                    )
                }
            }
        }
    }
}

@Composable
private fun OptionCard(
    index: Int,
    text: String,
    answered: Boolean,
    selected: Boolean,
    correct: Boolean,
    onClick: () -> Unit,
) {
    val showCorrect = answered && correct
    val showWrong = answered && selected && !correct
    val container = when {
        showCorrect -> JeopardyGreenDark
        showWrong -> JeopardyRedDark
        answered -> Color.White.copy(alpha = 0.035f)
        else -> JeopardyBlue.copy(alpha = 0.78f)
    }
    val border = when {
        showCorrect -> JeopardyGreen
        showWrong -> JeopardyRed
        answered -> Color.White.copy(alpha = 0.07f)
        else -> Color.White.copy(alpha = 0.16f)
    }
    val letters = listOf("А", "Б", "В", "Г", "Д", "Е")

    Surface(
        onClick = onClick,
        enabled = !answered,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = container,
        border = BorderStroke(if (showCorrect || showWrong) 1.5.dp else 1.dp, border),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = CircleShape,
                color = when {
                    showCorrect -> JeopardyGreen.copy(alpha = 0.2f)
                    showWrong -> JeopardyRed.copy(alpha = 0.2f)
                    else -> JeopardyGold.copy(alpha = 0.15f)
                },
            ) {
                Box(
                    modifier = Modifier.size(30.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = letters.getOrElse(index) { "?" },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Black,
                        color = when {
                            showCorrect -> JeopardyGreen
                            showWrong -> JeopardyRed
                            else -> JeopardyGold
                        },
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = text,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                color = if (answered && !showCorrect && !showWrong) {
                    Color.White.copy(alpha = 0.45f)
                } else {
                    Color.White
                },
            )
            if (showCorrect || showWrong) {
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = if (showCorrect) Icons.Default.CheckCircle else Icons.Default.Cancel,
                    contentDescription = null,
                    tint = if (showCorrect) JeopardyGreen else JeopardyRed,
                )
            }
        }
    }
}

@Composable
private fun AnswerFeedback(correctAnswer: String) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.White.copy(alpha = 0.05f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Правильный ответ",
                style = MaterialTheme.typography.labelMedium,
                color = JeopardyGold,
                letterSpacing = 1.sp,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = correctAnswer,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
            )
        }
    }
}

@Composable
private fun ReactionPanel(
    correct: Boolean,
    progress: Float,
    value: Int,
) {
    val color = if (correct) JeopardyGreen else JeopardyRed
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = color.copy(alpha = 0.13f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.55f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HamsterReaction(
                correct = correct,
                progress = progress,
                modifier = Modifier.size(104.dp),
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (correct) "Верно! Ням!" else "Ой! Разряд тока",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = color,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = if (correct) "+$value баллов" else "−$value баллов",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                )
                Text(
                    text = if (correct) {
                        "Орех отправлен в рот — хомяк доволен."
                    } else {
                        "Хомяк удивлён и потерял орех."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = JeopardyMuted,
                    fontSize = 12.sp,
                    lineHeight = 15.sp,
                )
            }
        }
    }
}
