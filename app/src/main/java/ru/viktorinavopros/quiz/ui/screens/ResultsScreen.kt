package ru.viktorinavopros.quiz.ui.screens

import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import ru.viktorinavopros.quiz.game.AnsweredQuestion
import ru.viktorinavopros.quiz.game.GameState
import ru.viktorinavopros.quiz.game.Player
import ru.viktorinavopros.quiz.ui.components.EmptyState
import ru.viktorinavopros.quiz.ui.components.JeopardyBackground
import ru.viktorinavopros.quiz.ui.components.JeopardyButton
import ru.viktorinavopros.quiz.ui.components.PlayerDot
import ru.viktorinavopros.quiz.ui.components.RemoteImage
import ru.viktorinavopros.quiz.ui.components.formatScore
import ru.viktorinavopros.quiz.ui.theme.JeopardyBlueLight
import ru.viktorinavopros.quiz.ui.theme.JeopardyGold
import ru.viktorinavopros.quiz.ui.theme.JeopardyGreen
import ru.viktorinavopros.quiz.ui.theme.JeopardyHeader
import ru.viktorinavopros.quiz.ui.theme.JeopardyMuted
import ru.viktorinavopros.quiz.ui.theme.JeopardyRed
import ru.viktorinavopros.quiz.ui.theme.JeopardyWhite
import kotlin.math.roundToInt

private enum class AnswerFilter(val title: String) {
    ALL("Все"),
    CORRECT("Верные"),
    WRONG("Ошибки"),
}

@Composable
fun ResultsScreen(
    game: GameState,
    onPlayAgain: () -> Unit,
    onNewGame: () -> Unit,
) {
    var filter by remember { mutableStateOf(AnswerFilter.ALL) }
    var expandedId by remember { mutableStateOf<String?>(null) }
    val ranked = remember(game.players) {
        game.players.sortedWith(
            compareByDescending<Player> { it.score }
                .thenByDescending { it.correct }
                .thenBy { it.name },
        )
    }
    val correctCount = game.history.count { it.isCorrect }
    val wrongCount = game.history.count { !it.isCorrect }
    val filtered = when (filter) {
        AnswerFilter.ALL -> game.history
        AnswerFilter.CORRECT -> game.history.filter { it.isCorrect }
        AnswerFilter.WRONG -> game.history.filter { !it.isCorrect }
    }

    JeopardyBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            ResultsTopBar(onClose = onNewGame)

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item {
                    Text(
                        text = "РЕЗУЛЬТАТ ТЕСТА",
                        style = MaterialTheme.typography.labelMedium,
                        color = JeopardyGold,
                        letterSpacing = 1.8.sp,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Итоги игры",
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color.White,
                    )
                    Text(
                        text = if (game.history.size >= game.totalCells) {
                            "Все вопросы табло сыграны"
                        } else {
                            "Игра завершена досрочно: отвечено ${game.history.size} из ${game.totalCells} вопросов"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = JeopardyMuted,
                    )
                }

                item {
                    ResultSummaryCard(
                        correctCount = correctCount,
                        wrongCount = wrongCount,
                        total = game.history.size,
                    )
                }

                if (ranked.isNotEmpty() && game.history.isNotEmpty()) {
                    item {
                        WinnerCard(
                            player = ranked.first(),
                            players = ranked,
                        )
                    }
                }

                item {
                    Text(
                        text = "Таблица игроков",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }

                items(ranked, key = { it.id }) { player ->
                    PlayerRankRow(
                        place = ranked.indexOf(player) + 1,
                        player = player,
                    )
                }

                item {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Разбор ответов",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Нажмите на карточку, чтобы увидеть вопрос, варианты и пояснение.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = JeopardyMuted,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AnswerFilter.entries.forEach { option ->
                            FilterChip(
                                selected = filter == option,
                                onClick = { filter = option },
                                label = {
                                    Text(
                                        text = option.title,
                                        style = MaterialTheme.typography.labelMedium,
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = Color.White.copy(alpha = 0.05f),
                                    labelColor = JeopardyMuted,
                                    selectedContainerColor = JeopardyGold.copy(alpha = 0.18f),
                                    selectedLabelColor = JeopardyGold,
                                ),
                            )
                        }
                    }
                }

                if (filtered.isEmpty()) {
                    item {
                        EmptyState(
                            emoji = if (game.history.isEmpty()) "🎲" else "🔍",
                            title = if (game.history.isEmpty()) "Пока нет ответов" else "Ничего не найдено",
                            subtitle = if (game.history.isEmpty()) {
                                "Вернитесь на табло и выберите первый вопрос."
                            } else {
                                "В этой категории ответов пока нет."
                            },
                        )
                    }
                } else {
                    items(filtered, key = { it.cellId }) { answer ->
                        AnswerReviewCard(
                            answer = answer,
                            expanded = expandedId == answer.cellId,
                            onToggle = {
                                expandedId = if (expandedId == answer.cellId) null else answer.cellId
                            },
                        )
                    }
                }

                item {
                    Spacer(Modifier.height(6.dp))
                    JeopardyButton(
                        text = "Играть снова",
                        onClick = onPlayAgain,
                    )
                    Spacer(Modifier.height(8.dp))
                    TextButton(
                        onClick = onNewGame,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = "Новая игра и настройки",
                            color = JeopardyMuted,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultsTopBar(onClose: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 70.dp, end = 70.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "QUIZ MASTER",
                style = MaterialTheme.typography.labelMedium,
                color = JeopardyGold,
                letterSpacing = 1.8.sp,
            )
            Text(
                text = "Результаты",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
            )
        }
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
                    contentDescription = "Закрыть",
                    tint = Color.White,
                )
            }
        }
    }
}

@Composable
private fun ResultSummaryCard(
    correctCount: Int,
    wrongCount: Int,
    total: Int,
) {
    val correctPercent = if (total == 0) 0 else (correctCount * 100f / total).roundToInt()
    val wrongPercent = if (total == 0) 0 else 100 - correctPercent

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = Color.White.copy(alpha = 0.045f),
        border = BorderStroke(1.dp, JeopardyGreen.copy(alpha = 0.38f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(JeopardyGreen.copy(alpha = 0.16f), Color.Transparent),
                    ),
                )
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.width(118.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "$correctPercent%",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Black,
                    color = JeopardyGreen,
                )
                Text(
                    text = "верно",
                    style = MaterialTheme.typography.labelMedium,
                    color = JeopardyMuted,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "$correctCount из $total",
                    style = MaterialTheme.typography.bodyMedium,
                    color = JeopardyMuted,
                    fontSize = 12.sp,
                )
            }

            Spacer(Modifier.width(18.dp))

            Column(modifier = Modifier.weight(1f)) {
                LegendRow(
                    color = JeopardyGreen,
                    label = "Верно",
                    count = correctCount,
                    percent = correctPercent,
                )
                Spacer(Modifier.height(10.dp))
                LegendRow(
                    color = JeopardyRed,
                    label = "Ошибки",
                    count = wrongCount,
                    percent = wrongPercent,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Всего вопросов: $total",
                    style = MaterialTheme.typography.bodyMedium,
                    color = JeopardyMuted,
                    fontSize = 12.sp,
                )
            }
        }
    }
}

@Composable
private fun LegendRow(
    color: Color,
    label: String,
    count: Int,
    percent: Int,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(10.dp)
                .background(color, CircleShape),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White,
        )
        Text(
            text = "$count",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "$percent%",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            color = color,
        )
    }
}

@Composable
private fun WinnerCard(
    player: Player,
    players: List<Player>,
) {
    val tied = players.count { it.score == player.score && it.correct == player.correct }
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = JeopardyBlueLight.copy(alpha = 0.28f),
        border = BorderStroke(1.5.dp, JeopardyGold.copy(alpha = 0.65f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(JeopardyGold.copy(alpha = 0.14f), Color.Transparent),
                    ),
                )
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(text = "🏆", fontSize = 44.sp)
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (tied > 1) "Победители" else "Победитель",
                style = MaterialTheme.typography.labelLarge,
                color = JeopardyGold,
                letterSpacing = 1.4.sp,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (tied > 1) {
                    players.filter { it.score == player.score && it.correct == player.correct }
                        .joinToString(", ") { it.name }
                } else {
                    player.name
                },
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${formatScore(player.score)} баллов",
                style = MaterialTheme.typography.titleLarge,
                color = JeopardyGold,
                fontWeight = FontWeight.Black,
            )
        }
    }
}

@Composable
private fun PlayerRankRow(
    place: Int,
    player: Player,
) {
    val placeColor = when (place) {
        1 -> JeopardyGold
        2 -> Color(0xFFC7D2E8)
        3 -> Color(0xFFD68B5A)
        else -> JeopardyMuted
    }
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = 0.045f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = CircleShape,
                color = placeColor.copy(alpha = 0.16f),
                border = BorderStroke(1.dp, placeColor.copy(alpha = 0.5f)),
            ) {
                Box(
                    modifier = Modifier.size(34.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = place.toString(),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Black,
                        color = placeColor,
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            PlayerDot(color = player.color, size = 12.dp)
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = player.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    maxLines = 1,
                )
                Text(
                    text = "${player.correct} верно · ${player.wrong} ошибок · ${player.accuracy}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = JeopardyMuted,
                    fontSize = 12.sp,
                )
            }
            Text(
                text = formatScore(player.score),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = if (place == 1) JeopardyGold else Color.White,
            )
        }
    }
}

@Composable
private fun AnswerReviewCard(
    answer: AnsweredQuestion,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    val color = if (answer.isCorrect) JeopardyGreen else JeopardyRed

    Surface(
        onClick = onToggle,
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = if (expanded) 0.07f else 0.04f),
        border = BorderStroke(
            1.dp,
            color.copy(alpha = if (expanded) 0.6f else 0.32f),
        ),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = JeopardyHeader.copy(alpha = 0.55f),
                ) {
                    Text(
                        text = "${answer.topic.emoji} ${answer.topic.title}",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "${answer.value}",
                    style = MaterialTheme.typography.labelLarge,
                    color = JeopardyGold,
                )
                Spacer(Modifier.weight(1f))
                Icon(
                    imageVector = if (answer.isCorrect) Icons.Default.CheckCircle else Icons.Default.Cancel,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(6.dp))
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Свернуть" else "Развернуть",
                    tint = JeopardyMuted,
                    modifier = Modifier.size(20.dp),
                )
            }

            Spacer(Modifier.height(10.dp))
            Text(
                text = answer.question.text,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                maxLines = if (expanded) Int.MAX_VALUE else 2,
                overflow = TextOverflow.Ellipsis,
            )

            if (expanded) {
                val imageUrl = answer.question.imageUrl
                if (!imageUrl.isNullOrBlank()) {
                    Spacer(Modifier.height(10.dp))
                    RemoteImage(
                        url = imageUrl,
                        contentDescription = "Картинка к вопросу",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(190.dp),
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                PlayerDot(color = answer.playerColor, size = 8.dp)
                Spacer(Modifier.width(6.dp))
                Text(
                    text = answer.playerName,
                    style = MaterialTheme.typography.labelMedium,
                    color = JeopardyMuted,
                    maxLines = 1,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (answer.isCorrect) {
                        "· ваш ответ: ${answer.selectedAnswer ?: "—"} — верно"
                    } else {
                        "· ваш ответ: ${answer.selectedAnswer ?: "время вышло"} — неверно"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = color,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (!answer.isCorrect) {
                Spacer(Modifier.height(3.dp))
                Text(
                    text = "Правильный ответ: ${answer.correctAnswer}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = JeopardyGreen,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (expanded) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                Spacer(Modifier.height(12.dp))

                Text(
                    text = "ОТВЕТЫ",
                    style = MaterialTheme.typography.labelMedium,
                    color = JeopardyGold,
                    letterSpacing = 1.sp,
                )
                Spacer(Modifier.height(8.dp))
                AnswerLine(
                    label = "Ответ игрока",
                    text = if (answer.timedOut) {
                        "Время вышло"
                    } else {
                        answer.selectedAnswer ?: "—"
                    },
                    color = if (answer.isCorrect) JeopardyGreen else JeopardyRed,
                )
                Spacer(Modifier.height(6.dp))
                AnswerLine(
                    label = "Правильный ответ",
                    text = answer.correctAnswer,
                    color = JeopardyGreen,
                )

                Spacer(Modifier.height(8.dp))
                ExplanationBlock(answer)

                if (answer.question.postTitle.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Источник: ${answer.question.postTitle}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = JeopardyMuted.copy(alpha = 0.7f),
                        fontSize = 11.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            } else {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Открыть разбор",
                    style = MaterialTheme.typography.labelMedium,
                    color = JeopardyMuted.copy(alpha = 0.8f),
                )
            }
        }
    }
}

@Composable
private fun AnswerLine(
    label: String,
    text: String,
    color: Color,
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = color.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 9.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = JeopardyMuted,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
            )
        }
    }
}

@Composable
private fun ExplanationBlock(answer: AnsweredQuestion) {
    val why = when {
        answer.isCorrect -> "Вы ответили «${answer.selectedAnswer ?: "—"}» — это верный ответ."
        answer.timedOut -> "Время вышло. Правильный ответ — «${answer.correctAnswer}»."
        else -> "Вы ответили «${answer.selectedAnswer ?: "—"}», а правильный ответ — " +
            "«${answer.correctAnswer}»."
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = JeopardyGold.copy(alpha = 0.06f),
        border = BorderStroke(1.dp, JeopardyGold.copy(alpha = 0.22f)),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "ПОЧЕМУ ТАК",
                style = MaterialTheme.typography.labelMedium,
                color = JeopardyGold,
                letterSpacing = 1.sp,
            )
            Spacer(Modifier.height(5.dp))
            Text(
                text = why,
                style = MaterialTheme.typography.bodyMedium,
                color = JeopardyWhite,
                fontSize = 14.sp,
                lineHeight = 19.sp,
            )
            if (answer.explanation.isNotBlank() &&
                answer.explanation.trim() != answer.correctAnswer.trim()
            ) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Пояснение с сайта",
                    style = MaterialTheme.typography.labelMedium,
                    color = JeopardyMuted,
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = answer.explanation,
                    style = MaterialTheme.typography.bodyMedium,
                    color = JeopardyMuted,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                )
            }
        }
    }
}
