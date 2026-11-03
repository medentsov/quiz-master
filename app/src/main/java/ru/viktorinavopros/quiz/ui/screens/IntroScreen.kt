package ru.viktorinavopros.quiz.ui.screens

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.viktorinavopros.quiz.data.QuizData
import ru.viktorinavopros.quiz.game.GameFactory
import ru.viktorinavopros.quiz.ui.DataState
import ru.viktorinavopros.quiz.ui.components.ErrorState
import ru.viktorinavopros.quiz.ui.components.JeopardyBackground
import ru.viktorinavopros.quiz.ui.components.JeopardyButton
import ru.viktorinavopros.quiz.ui.components.JeopardyCard
import ru.viktorinavopros.quiz.ui.components.LoadingState
import ru.viktorinavopros.quiz.ui.components.PlayerDot
import ru.viktorinavopros.quiz.ui.components.formatNumber
import ru.viktorinavopros.quiz.ui.components.pluralRu
import ru.viktorinavopros.quiz.ui.theme.JeopardyGold
import ru.viktorinavopros.quiz.ui.theme.JeopardyMuted
import ru.viktorinavopros.quiz.ui.theme.JeopardyNavy
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun IntroScreen(
    dataState: DataState,
    refreshing: Boolean,
    playerCount: Int,
    playerNames: List<String>,
    lastUpdated: Long?,
    onPlayerCountChange: (Int) -> Unit,
    onPlayerNameChange: (Int, String) -> Unit,
    onStart: () -> Unit,
    onRetry: () -> Unit,
) {
    val data = (dataState as? DataState.Ready)?.data
    JeopardyBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .imePadding(),
        ) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item {
                IntroHeader()
            }

            item {
                JeopardyCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text(
                                text = "Игроки",
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.White,
                            )
                            Text(
                                text = "От 1 до ${GameFactory.MAX_PLAYERS}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = JeopardyMuted,
                            )
                        }
                        PlayerCountStepper(
                            count = playerCount,
                            onChange = onPlayerCountChange,
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    repeat(playerCount) { index ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            PlayerDot(
                                color = GameFactory.PLAYER_COLORS[index % GameFactory.PLAYER_COLORS.size],
                                size = 14.dp,
                            )
                            Spacer(Modifier.width(12.dp))
                            OutlinedTextField(
                                value = playerNames.getOrNull(index).orEmpty(),
                                onValueChange = { onPlayerNameChange(index, it) },
                                modifier = Modifier.weight(1f),
                                placeholder = {
                                    Text(
                                        text = "Игрок ${index + 1}",
                                        color = JeopardyMuted.copy(alpha = 0.7f),
                                    )
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    cursorColor = JeopardyGold,
                                    focusedBorderColor = JeopardyGold,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                                    focusedContainerColor = Color.White.copy(alpha = 0.04f),
                                    unfocusedContainerColor = Color.White.copy(alpha = 0.03f),
                                ),
                            )
                        }
                    }
                }
            }

            item {
                RulesCard()
            }

            item {
                DataStatusCard(
                    dataState = dataState,
                    refreshing = refreshing,
                    lastUpdated = lastUpdated,
                    onRetry = onRetry,
                )
            }

            item {
                Text(
                    text = "Вопросы и ответы загружаются с viktorinavopros.ru\n" +
                        "Приложение не является официальным",
                    modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = JeopardyMuted.copy(alpha = 0.65f),
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp,
                )
            }
        }

            IntroBottomBar(
                enabled = data != null,
                onStart = onStart,
            )
        }
    }
}

@Composable
private fun IntroBottomBar(
    enabled: Boolean,
    onStart: () -> Unit,
) {
    Surface(
        color = JeopardyNavy.copy(alpha = 0.94f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.07f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            JeopardyButton(
                text = "Начать игру",
                onClick = onStart,
                enabled = enabled,
            )
        }
    }
}

@Composable
private fun IntroHeader() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(
            shape = CircleShape,
            color = JeopardyGold.copy(alpha = 0.14f),
            border = BorderStroke(1.dp, JeopardyGold.copy(alpha = 0.5f)),
        ) {
            Box(
                modifier = Modifier.size(74.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "?", fontSize = 42.sp, fontWeight = FontWeight.Black, color = JeopardyGold)
            }
        }
        Spacer(Modifier.height(14.dp))
        Text(
            text = "СВОЯ ИГРА",
            style = MaterialTheme.typography.displayMedium,
            color = JeopardyGold,
            textAlign = TextAlign.Center,
            letterSpacing = 2.sp,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Интеллектуальная викторина по вопросам с сайта viktorinavopros.ru",
            style = MaterialTheme.typography.bodyMedium,
            color = JeopardyMuted,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PlayerCountStepper(
    count: Int,
    onChange: (Int) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        StepButton(
            text = "−",
            enabled = count > GameFactory.MIN_PLAYERS,
            onClick = { onChange(count - 1) },
        )
        Text(
            text = count.toString(),
            modifier = Modifier.width(46.dp),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = JeopardyGold,
            textAlign = TextAlign.Center,
        )
        StepButton(
            text = "+",
            enabled = count < GameFactory.MAX_PLAYERS,
            onClick = { onChange(count + 1) },
        )
    }
}

@Composable
private fun StepButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        color = Color.White.copy(alpha = 0.07f),
        contentColor = if (enabled) Color.White else JeopardyMuted.copy(alpha = 0.4f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
    ) {
        Box(
            modifier = Modifier.size(40.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun RulesCard() {
    JeopardyCard {
        Text(
            text = "Как играть",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
        )
        Spacer(Modifier.height(12.dp))
        RuleRow(number = "1", text = "Выберите от одной до шести категорий для игрового табло")
        RuleRow(number = "2", text = "Игроки по очереди выбирают категорию и стоимость вопроса")
        RuleRow(number = "3", text = "Верный ответ приносит баллы, ошибка отнимает их")
        RuleRow(number = "4", text = "В конце — таблица результатов и разбор всех ответов")
    }
}

@Composable
private fun RuleRow(number: String, text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            shape = CircleShape,
            color = JeopardyGold.copy(alpha = 0.16f),
        ) {
            Box(
                modifier = Modifier.size(28.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = number,
                    style = MaterialTheme.typography.labelMedium,
                    color = JeopardyGold,
                    fontWeight = FontWeight.Black,
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = JeopardyMuted,
        )
    }
}

@Composable
private fun DataStatusCard(
    dataState: DataState,
    refreshing: Boolean,
    lastUpdated: Long?,
    onRetry: () -> Unit,
) {
    when (dataState) {
        is DataState.Ready -> {
            val data = dataState.data
            JeopardyCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .size(10.dp)
                            .background(
                                color = if (refreshing) JeopardyGold else Color(0xFF35D07F),
                                shape = CircleShape,
                            ),
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "${formatNumber(data.questionCount)} " +
                                "${pluralRu(data.questionCount, "вопрос", "вопроса", "вопросов")} · " +
                                "${data.tests.size} " +
                                pluralRu(data.tests.size, "тест", "теста", "тестов"),
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                        )
                        Text(
                            text = if (refreshing) {
                                "Обновляем данные…"
                            } else {
                                lastUpdated?.let { "Обновлено ${formatDate(it)}" } ?: "Данные готовы"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = JeopardyMuted,
                            fontSize = 12.sp,
                        )
                    }
                }
                if (refreshing) {
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = JeopardyGold,
                        trackColor = Color.White.copy(alpha = 0.08f),
                    )
                }
            }
        }

        is DataState.Loading -> {
            LoadingState(message = "Загружаем вопросы и ответы с сайта…")
        }

        is DataState.Error -> {
            ErrorState(
                message = dataState.message,
                onRetry = onRetry,
            )
        }
    }
}

private fun formatDate(timestamp: Long): String {
    if (timestamp <= 0) return ""
    return SimpleDateFormat("d MMM, HH:mm", Locale("ru")).format(Date(timestamp))
}
