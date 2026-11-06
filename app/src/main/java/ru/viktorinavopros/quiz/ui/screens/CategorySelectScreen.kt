package ru.viktorinavopros.quiz.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.viktorinavopros.quiz.data.QuizData
import ru.viktorinavopros.quiz.data.Topic
import ru.viktorinavopros.quiz.game.GameFactory
import ru.viktorinavopros.quiz.game.TopicInfo
import ru.viktorinavopros.quiz.ui.DataState
import ru.viktorinavopros.quiz.ui.components.ErrorState
import ru.viktorinavopros.quiz.ui.components.JeopardyBackground
import ru.viktorinavopros.quiz.ui.components.JeopardyButton
import ru.viktorinavopros.quiz.ui.components.JeopardyCard
import ru.viktorinavopros.quiz.ui.components.LoadingState
import ru.viktorinavopros.quiz.ui.components.formatNumber
import ru.viktorinavopros.quiz.ui.components.pluralRu
import ru.viktorinavopros.quiz.ui.theme.JeopardyBlueLight
import ru.viktorinavopros.quiz.ui.theme.JeopardyGold
import ru.viktorinavopros.quiz.ui.theme.JeopardyMuted

@Composable
fun CategorySelectScreen(
    dataState: DataState,
    selectedTopics: List<Topic>,
    onToggleTopic: (Topic) -> Unit,
    onRandomTopics: () -> Unit,
    onStart: () -> Unit,
    onBack: () -> Unit,
    onRetry: () -> Unit,
) {
    JeopardyBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
        ) {
            CategoryTopBar(onBack = onBack)

            when (dataState) {
                is DataState.Loading -> {
                    LoadingState(
                        message = "Загружаем категории…",
                        modifier = Modifier.weight(1f),
                    )
                }

                is DataState.Error -> {
                    ErrorState(
                        message = dataState.message,
                        onRetry = onRetry,
                        modifier = Modifier.weight(1f),
                    )
                }

                is DataState.Ready -> {
                    val data = dataState.data
                    val available = GameFactory.availableTopics(data)
                    val selectedSet = selectedTopics.toSet()

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(
                            start = 20.dp,
                            end = 20.dp,
                            top = 8.dp,
                            bottom = 120.dp,
                        ),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        item {
                            CategoryHeader(
                                selectedCount = selectedTopics.size,
                                total = GameFactory.CATEGORIES_PER_GAME,
                                onRandom = onRandomTopics,
                            )
                        }

                        items(
                            items = available,
                            key = { it.topic.name },
                        ) { info ->
                            TopicSelectCard(
                                info = info,
                                selected = info.topic in selectedSet,
                                enabled = info.topic in selectedSet ||
                                    selectedTopics.size < GameFactory.CATEGORIES_PER_GAME,
                                onClick = { onToggleTopic(info.topic) },
                            )
                        }

                        item {
                            Text(
                                text = "Категории определяются автоматически по ключевым словам. " +
                                    "В «Разное» попадают вопросы, которые не удалось отнести к теме.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = JeopardyMuted.copy(alpha = 0.7f),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                    }

                    BottomStartBar(
                        selectedCount = selectedTopics.size,
                        onStart = onStart,
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryTopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 70.dp, end = 70.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            onClick = onBack,
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.07f),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
        ) {
            Box(
                modifier = Modifier.size(42.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Назад",
                    tint = Color.White,
                )
            }
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(
                text = "QUIZ MASTER",
                style = MaterialTheme.typography.labelMedium,
                color = JeopardyGold,
                letterSpacing = 1.5.sp,
            )
            Text(
                text = "Категории",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
            )
        }
    }
}

@Composable
private fun CategoryHeader(
    selectedCount: Int,
    total: Int,
    onRandom: () -> Unit,
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = "Выберите от ${GameFactory.MIN_CATEGORIES} до $total категорий",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "В каждой категории будет по ${GameFactory.CELLS_PER_CATEGORY} вопросов " +
                "стоимостью от 100 до 500 баллов.",
            style = MaterialTheme.typography.bodyMedium,
            color = JeopardyMuted,
        )
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Surface(
                shape = RoundedCornerShape(50),
                color = if (selectedCount == total) {
                    JeopardyGold.copy(alpha = 0.16f)
                } else {
                    Color.White.copy(alpha = 0.06f)
                },
                border = BorderStroke(
                    1.dp,
                    if (selectedCount == total) JeopardyGold else Color.White.copy(alpha = 0.1f),
                ),
            ) {
                Text(
                    text = "Выбрано $selectedCount из $total",
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (selectedCount == total) JeopardyGold else JeopardyMuted,
                )
            }
            TextButton(onClick = onRandom) {
                Text(
                    text = "Случайные $total",
                    color = JeopardyGold,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

@Composable
private fun TopicSelectCard(
    info: TopicInfo,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(16.dp),
        color = when {
            selected -> JeopardyBlueLight.copy(alpha = 0.55f)
            else -> Color.White.copy(alpha = 0.045f)
        },
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) JeopardyGold else Color.White.copy(alpha = 0.08f),
        ),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (selected) JeopardyGold.copy(alpha = 0.18f) else JeopardyBlueLight,
            ) {
                Box(
                    modifier = Modifier.size(48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = info.topic.emoji, fontSize = 24.sp)
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = info.topic.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (enabled || selected) Color.White else Color.White.copy(alpha = 0.4f),
                )
                Text(
                    text = "${formatNumber(info.count)} " +
                        "${pluralRu(info.count, "вопрос", "вопроса", "вопросов")} · " +
                        info.topic.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (enabled || selected) JeopardyMuted else JeopardyMuted.copy(alpha = 0.4f),
                    fontSize = 12.sp,
                )
            }
            Spacer(Modifier.width(8.dp))
            if (selected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Выбрано",
                    tint = JeopardyGold,
                )
            } else {
                Surface(
                    shape = CircleShape,
                    color = Color.Transparent,
                    border = BorderStroke(
                        1.5.dp,
                        if (enabled) Color.White.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.1f),
                    ),
                    modifier = Modifier.size(20.dp),
                ) {}
            }
        }
    }
}

@Composable
private fun BottomStartBar(
    selectedCount: Int,
    onStart: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
    ) {
        Column {
            JeopardyButton(
                text = if (selectedCount >= GameFactory.MIN_CATEGORIES) {
                    "Начать игру · $selectedCount " +
                        pluralRu(selectedCount, "категория", "категории", "категорий")
                } else {
                    "Выберите хотя бы одну категорию"
                },
                onClick = onStart,
                enabled = selectedCount >= GameFactory.MIN_CATEGORIES,
            )
        }
    }
}
