package ru.viktorinavopros.quiz.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.viktorinavopros.quiz.ui.theme.JeopardyBlue
import ru.viktorinavopros.quiz.ui.theme.JeopardyBlueLight
import ru.viktorinavopros.quiz.ui.theme.JeopardyGold
import ru.viktorinavopros.quiz.ui.theme.JeopardyMuted
import ru.viktorinavopros.quiz.ui.theme.JeopardyNavy
import ru.viktorinavopros.quiz.ui.theme.JeopardyNavyDeep
import kotlin.math.PI
import kotlin.math.sin

/** Настроение экрана после ответа: нейтральное, верный ответ или ошибка. */
enum class ReactionTone { NEUTRAL, CORRECT, WRONG }

@Composable
fun JeopardyBackground(
    tone: ReactionTone = ReactionTone.NEUTRAL,
    toneProgress: Float = 0f,
    content: @Composable BoxScope.() -> Unit,
) {
    val p = toneProgress.coerceIn(0f, 1f)
    val base = listOf(JeopardyNavyDeep, JeopardyNavy, Color(0xFF0A2354))
    val target = when (tone) {
        ReactionTone.CORRECT -> listOf(
            Color(0xFF032114),
            Color(0xFF075A31),
            Color(0xFF0E8A4A),
        )
        ReactionTone.WRONG -> listOf(
            Color(0xFF1A0410),
            Color(0xFF4A0A1E),
            Color(0xFF7A1028),
        )
        ReactionTone.NEUTRAL -> base
    }
    val colors = if (tone == ReactionTone.NEUTRAL) {
        base
    } else {
        base.zip(target) { from, to -> lerp(from, to, p) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(colors)),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            if (tone == ReactionTone.CORRECT) {
                drawCircle(
                    color = Color(0xFF35D07F).copy(alpha = 0.14f * p),
                    radius = size.width * 0.8f,
                    center = Offset(size.width * 0.85f, size.height * 0.08f),
                )
            }
            drawCircle(
                color = JeopardyBlueLight.copy(alpha = 0.18f),
                radius = size.width * 0.75f,
                center = Offset(size.width * 0.92f, size.height * 0.04f),
            )
            drawCircle(
                color = JeopardyGold.copy(alpha = 0.05f),
                radius = size.width * 0.6f,
                center = Offset(size.width * 0.02f, size.height * 0.92f),
            )
            if (tone == ReactionTone.WRONG && p > 0.01f) {
                val flash = sin(p * PI * 9.0).coerceAtLeast(0.0).toFloat() *
                    (1f - p * 0.45f) * 0.25f
                drawRect(Color(0xFFC4ECFF).copy(alpha = flash))
            }
        }
        content()
    }
}

@Composable
fun JeopardyButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = JeopardyGold,
            contentColor = Color(0xFF2A1A00),
            disabledContainerColor = Color(0xFF303A54),
            disabledContentColor = Color(0xFF818AA2),
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.8.sp,
        )
    }
}

@Composable
fun JeopardyCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            content = content,
        )
    }
}

@Composable
fun PlayerDot(color: Long, size: Dp = 12.dp) {
    Box(
        Modifier
            .size(size)
            .background(Color(color), CircleShape),
    )
}

@Composable
fun PlayerScoreChip(
    name: String,
    score: Int,
    color: Long,
    current: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = if (current) JeopardyGold.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.06f),
        border = BorderStroke(
            width = if (current) 1.5.dp else 1.dp,
            color = if (current) JeopardyGold else Color.White.copy(alpha = 0.08f),
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlayerDot(color = color, size = 9.dp)
            Spacer(Modifier.width(8.dp))
            Column {
                Text(
                    text = name,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (current) JeopardyGold else JeopardyMuted,
                    maxLines = 1,
                )
                Text(
                    text = formatScore(score),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                )
            }
        }
    }
}

@Composable
fun StatPill(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = color.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.35f)),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = color,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = JeopardyMuted,
            )
        }
    }
}

@Composable
fun LoadingState(
    message: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator(color = JeopardyGold)
        Spacer(Modifier.height(18.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = JeopardyMuted,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = "⚠", fontSize = 40.sp)
        Spacer(Modifier.height(12.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = JeopardyMuted,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(18.dp))
        JeopardyButton(
            text = "Повторить",
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth(0.7f),
        )
    }
}

@Composable
fun EmptyState(
    emoji: String,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = emoji, fontSize = 44.sp)
        Spacer(Modifier.height(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = JeopardyMuted,
            textAlign = TextAlign.Center,
        )
    }
}

fun formatScore(score: Int): String = if (score > 0) "+$score" else score.toString()

/** Русское склонение: 1 вопрос, 2 вопроса, 5 вопросов. */
fun pluralRu(count: Int, one: String, few: String, many: String): String {
    val mod100 = kotlin.math.abs(count) % 100
    val mod10 = mod100 % 10
    return when {
        mod100 in 11..14 -> many
        mod10 == 1 -> one
        mod10 in 2..4 -> few
        else -> many
    }
}

fun formatNumber(value: Int): String {
    val text = value.toString()
    val builder = StringBuilder()
    for ((index, char) in text.withIndex()) {
        if (index > 0 && (text.length - index) % 3 == 0 && char.isDigit()) {
            builder.append(' ')
        }
        builder.append(char)
    }
    return builder.toString()
}
