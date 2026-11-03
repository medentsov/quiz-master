package ru.viktorinavopros.quiz.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ru.viktorinavopros.quiz.ui.theme.JeopardyGold
import ru.viktorinavopros.quiz.ui.theme.JeopardyNavyDeep
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

private val Fur = Color(0xFFF6C45C)
private val FurDark = Color(0xFFAA6C26)
private val Belly = Color(0xFFFFEEC6)
private val BellyLine = Color(0xFFE0AA62)
private val Cheek = Color(0xFFFFD07C)
private val CheekLine = Color(0xFFC67E30)
private val CheekBump = Color(0xFFF8BE74)
private val EarInner = Color(0xFFF29C9C)
private val Eye = Color(0xFF2B1A0F)
private val Nose = Color(0xFFD66876)
private val Nut = Color(0xFFA4602A)
private val NutDark = Color(0xFF683A1A)
private val NutLight = Color(0xFFD69250)
private val Mouth = Color(0xFF5C2C20)
private val Tongue = Color(0xFFF08C96)
private val Whisker = Color(0xFFB98448)
private val Spark = Color(0xFFFFE08A)
private val Electric = Color(0xFFB9E6FF)
private val Sweat = Color(0xFF8FD8FF)

/**
 * Хомяк-реакция на ответ.
 *
 * @param correct true — хомяк ест орех, false — его бьёт разряд тока.
 * @param progress 0..1, обычно анимируется один раз после ответа.
 */
@Composable
fun HamsterReaction(
    correct: Boolean,
    progress: Float,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier) {
        val u = size.minDimension / 300f
        val p = progress.coerceIn(0f, 1f)
        val shock = if (correct) 0f else shockEnvelope(p)
        val shakeX = if (correct) 0f else sin(p * 70f) * 5.5f * shock
        val shakeY = if (correct) 0f else cos(p * 53f) * 2.5f * shock
        translate(shakeX * u, shakeY * u) {
            drawHamsterScene(correct, p, u)
        }
    }
}

/**
 * Маленький довольный хомяк для верхнего левого угла приложения.
 */
@Composable
fun HamsterBadge(
    modifier: Modifier = Modifier,
    size: Dp = 52.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    listOf(Color(0xFF2A5FA8), JeopardyNavyDeep),
                ),
            )
            .border(1.5.dp, JeopardyGold.copy(alpha = 0.65f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        HamsterReaction(
            correct = true,
            progress = 0.62f,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(scaleX = 1.45f, scaleY = 1.45f),
        )
    }
}

private fun shockEnvelope(p: Float): Float {
    if (p < 0.12f) return 0f
    val u = ((p - 0.12f) / 0.52f).coerceIn(0f, 1f)
    return sin(u * PI).toFloat()
}

private fun DrawScope.drawHamsterScene(correct: Boolean, p: Float, u: Float) {
    val bounce = if (correct) {
        val b = ((p - 0.40f) / 0.20f).coerceIn(0f, 1f)
        -sin(b * PI).toFloat() * 7f
    } else {
        0f
    }

    translate(0f, bounce * u) {
        val happy = correct && p > 0.40f
        val surprised = !correct && p > 0.15f
        val shock = if (correct) 0f else shockEnvelope(p)

        val chew = if (correct && p > 0.44f && p < 0.82f) {
            val local = (p - 0.44f) / 0.38f
            abs(sin(local * PI * 5.0f)).toFloat() * (1f - local)
        } else {
            0f
        }
        val biteOpen = if (correct) {
            (((p - 0.20f) / 0.12f).coerceIn(0f, 1f) *
                (1f - ((p - 0.46f) / 0.10f).coerceIn(0f, 1f)))
        } else {
            0f
        }
        val mouthOpen = if (correct) {
            maxOf(biteOpen, chew * 0.85f)
        } else if (surprised) {
            1f
        } else {
            0f
        }

        val cheekScale = if (correct) {
            1f + 0.11f * sin(((p - 0.34f) / 0.46f).coerceIn(0f, 1f) * PI).toFloat()
        } else {
            1f
        }

        // Уши.
        for (cx in listOf(100f, 200f)) {
            ovalOutlined(cx, 98f, 24f, 24f, Fur, FurDark, u, 2.6f)
            oval(cx, 98f, 13.5f, 13.5f, EarInner, u)
            oval(cx - 4f, 93f, 4f, 5f, Color(0xFFFFC8C8), u)
        }

        // Тело.
        ovalOutlined(150f, 214f, 78f, 68f, Fur, FurDark, u, 2.6f)
        ovalOutlined(150f, 232f, 50f, 42f, Belly, BellyLine, u, 1.8f)
        oval(138f, 218f, 24f, 15f, Color(0xFFFFF8DC), u)

        // Лапки-ножки.
        for (cx in listOf(112f, 188f)) {
            ovalOutlined(cx, 283f, 24f, 10f, Fur, FurDark, u, 2.4f)
            for (k in listOf(-8f, 0f, 8f)) {
                line(cx + k, 279f, cx + k, 287f, FurDark, u, 1.6f)
            }
        }

        // Голова.
        ovalOutlined(150f, 150f, 76f, 66f, Fur, FurDark, u, 2.6f)

        // Хохолок.
        line(140f, 86f, 136f, 72f, FurDark, u, 2.2f)
        line(150f, 84f, 150f, 68f, FurDark, u, 2.2f)
        line(160f, 86f, 164f, 72f, FurDark, u, 2.2f)

        // Вздыбленная шёрстка при разряде.
        if (surprised) {
            val spikes = listOf(
                -155f to 0.8f, -125f to 0.7f, -95f to 0.9f, -65f to 0.7f,
                -35f to 0.8f, -5f to 0.7f, 25f to 0.9f, 55f to 0.7f,
            )
            for ((angle, length) in spikes) {
                val rad = angle * PI.toFloat() / 180f
                val r1 = 70f
                val r2 = r1 + 9f * length * (0.6f + 0.4f * shock)
                line(
                    150f + cos(rad) * r1,
                    150f + sin(rad) * r1,
                    150f + cos(rad) * r2,
                    150f + sin(rad) * r2,
                    FurDark,
                    u,
                    2.0f,
                )
            }
        }

        // Щёки.
        for ((cx, flip) in listOf(105f to 1f, 195f to -1f)) {
            val rx = 33f * cheekScale
            val ry = 28f * cheekScale
            ovalOutlined(cx, 174f, rx, ry, Cheek, CheekLine, u, 2.2f)
            for ((bx, by, br) in listOf(
                Triple(-11f, 6f, 8f),
                Triple(4f, 10f, 7f),
                Triple(11f, -3f, 6f),
            )) {
                oval(
                    cx + flip * bx * cheekScale,
                    174f + by * cheekScale,
                    br * cheekScale,
                    br * 0.8f * cheekScale,
                    CheekBump,
                    u,
                )
            }
            oval(
                cx - flip * 12f * cheekScale,
                166f - 8f * cheekScale,
                10f * cheekScale,
                5f * cheekScale,
                Color(0xFFFFE7B8),
                u,
            )
        }

        // Глаза.
        for (cx in listOf(126f, 174f)) {
            when {
                happy -> {
                    val path = Path().apply {
                        moveTo((cx - 9f) * u, 140f * u)
                        quadraticBezierTo(cx * u, 128f * u, (cx + 9f) * u, 140f * u)
                    }
                    drawPath(path, Eye, style = Stroke(width = 3.4f * u, cap = StrokeCap.Round))
                }

                surprised -> {
                    oval(cx, 137f, 10f, 11f, Eye, u)
                    oval(cx - 3f, 133f, 2.6f, 2.6f, Color.White, u)
                    line(cx - 11f, 121f, cx - 3f, 118f, Eye, u, 2.0f)
                    line(cx + 3f, 118f, cx + 11f, 121f, Eye, u, 2.0f)
                }

                else -> {
                    oval(cx, 140f, 8f, 8f, Eye, u)
                    oval(cx - 2.5f, 137f, 2.4f, 2.4f, Color.White, u)
                }
            }
        }

        // Нос.
        ovalOutlined(150f, 164f, 5f, 4f, Nose, Color(0xFF963C50), u, 1.2f)
        oval(148.5f, 163f, 1.6f, 1.2f, Color(0xFFFFBEC8), u)

        // Рот.
        when {
            mouthOpen > 0.06f -> {
                val rx = 6f + 3.5f * mouthOpen
                val ry = 2.5f + 5.5f * mouthOpen
                oval(150f, 177f, rx, ry, Mouth, u)
                oval(150f, 177f + ry * 0.35f, rx * 0.5f, ry * 0.32f, Tongue, u)
            }

            surprised -> {
                oval(150f, 178f, 6f, 7f, Mouth, u)
            }

            else -> {
                val path = Path().apply {
                    moveTo(142f * u, 175f * u)
                    quadraticBezierTo(150f * u, 182f * u, 158f * u, 175f * u)
                }
                drawPath(path, Mouth, style = Stroke(width = 2.4f * u, cap = StrokeCap.Round))
            }
        }

        // Усы.
        for (side in listOf(-1f, 1f)) {
            val baseX = 150f + side * 72f
            for ((dy, tilt) in listOf(-9f to -4f, 0f to 0f, 9f to 5f)) {
                line(
                    baseX,
                    174f + dy,
                    baseX + side * 28f,
                    172f + dy + tilt,
                    Whisker,
                    u,
                    1.8f,
                )
            }
        }

        // Левая лапа.
        ovalOutlined(105f, 228f, 16f, 11f, Fur, FurDark, u, 2.2f)
        for (k in listOf(-5f, 0f, 5f)) {
            line(105f + k, 224f, 105f + k, 233f, FurDark, u, 1.4f)
        }

        // Орех и правая лапа.
        if (correct) {
            val nutT = ((p - 0.04f) / 0.36f).coerceIn(0f, 1f)
            val nutX = quad(205f, 186f, 150f, nutT)
            val nutY = quad(210f, 186f, 178f, nutT)
            val nutScale = if (p < 0.38f) {
                1f
            } else {
                1f - 0.86f * ((p - 0.38f) / 0.12f).coerceIn(0f, 1f)
            }
            val nutAlpha = if (p < 0.40f) {
                1f
            } else {
                1f - ((p - 0.40f) / 0.12f).coerceIn(0f, 1f)
            }

            val pawX = if (p < 0.40f) nutX + 8f else lerp(158f, 205f, ((p - 0.40f) / 0.30f).coerceIn(0f, 1f))
            val pawY = if (p < 0.40f) nutY + 15f else lerp(193f, 228f, ((p - 0.40f) / 0.30f).coerceIn(0f, 1f))
            drawRightPaw(pawX, pawY, u)
            if (nutAlpha > 0.02f) {
                drawNut(nutX, nutY, nutScale, nutAlpha, u)
                line(pawX - 6f, pawY - 6f, pawX + 6f, pawY - 8f, FurDark, u, 1.6f)
            }
        } else {
            val fall = ((p - 0.15f) / 0.50f).coerceIn(0f, 1f)
            val nutX = 205f + 20f * fall
            val nutY = 210f + 86f * fall * fall
            if (fall < 0.98f) {
                rotate(fall * 140f, Offset(nutX * u, nutY * u)) {
                    drawNut(nutX, nutY, 1f, 1f, u)
                }
            }
            drawRightPaw(205f, 228f, u)
        }

        // Искры при верном ответе.
        if (correct && p > 0.44f) {
            val intensity = sin(((p - 0.44f) / 0.56f).coerceIn(0f, 1f) * PI).toFloat()
            if (intensity > 0.02f) {
                val spots = listOf(
                    70f to 100f to 9f,
                    232f to 108f to 7f,
                    58f to 192f to 6f,
                    244f to 196f to 8f,
                    150f to 58f to 5f,
                )
                for ((spot, size) in spots) {
                    star(spot.first, spot.second, size * (0.7f + 0.3f * intensity), intensity, u)
                }
            }
        }

        // Разряд тока.
        if (!correct && p > 0.12f && p < 0.78f) {
            val flicker = 0.45f + 0.55f * abs(sin(p * 57f))
            val alpha = shock * flicker
            if (alpha > 0.03f) {
                drawElectric(alpha, u)
            }
        }

        // Капля пота после разряда.
        if (!correct && p > 0.55f) {
            val dropAlpha = ((p - 0.55f) / 0.15f).coerceIn(0f, 1f)
            drawSweatDrop(224f, 102f, dropAlpha, u)
        }
    }
}

private fun DrawScope.drawRightPaw(cx: Float, cy: Float, u: Float) {
    ovalOutlined(cx, cy, 15f, 10f, Fur, FurDark, u, 2.2f)
    for (k in listOf(-5f, 0f, 5f)) {
        line(cx + k, cy - 4f, cx + k, cy + 4f, FurDark, u, 1.4f)
    }
}

private fun DrawScope.drawNut(cx: Float, cy: Float, scale: Float, alpha: Float, u: Float) {
    if (alpha <= 0.02f || scale <= 0.03f) return
    val body = Nut.copy(alpha = alpha)
    val dark = NutDark.copy(alpha = alpha)
    val light = NutLight.copy(alpha = alpha)
    val texture = Color(0xFF80481E).copy(alpha = alpha)

    ovalOutlined(
        cx,
        cy + 2f * scale,
        14f * scale,
        17f * scale,
        body,
        dark,
        u,
        1.6f * scale,
    )
    oval(cx, cy - 13f * scale, 12f * scale, 6f * scale, dark, u)
    oval(cx, cy - 17f * scale, 6.5f * scale, 3f * scale, dark, u)
    line(cx, cy - 19f * scale, cx + 2f * scale, cy - 24f * scale, dark, u, 1.8f * scale)
    arc(cx - 2f * scale, cy + 1f * scale, 8f * scale, 12f * scale, 120f, 110f, texture, u, 1.2f * scale)
    arc(cx + 5f * scale, cy + 5f * scale, 5f * scale, 7f * scale, 300f, 90f, texture, u, 1.2f * scale)
    oval(cx - 5f * scale, cy - 2f * scale, 3f * scale, 5f * scale, light, u)
    oval(cx + 4f * scale, cy + 6f * scale, 1.6f * scale, 2.6f * scale, light, u)
}

private fun DrawScope.drawElectric(alpha: Float, u: Float) {
    val bolts = listOf(
        listOf(32f to 82f, 50f to 108f, 38f to 113f, 60f to 146f),
        listOf(268f to 72f, 250f to 102f, 262f to 108f, 240f to 142f),
        listOf(56f to 226f, 72f to 208f, 64f to 200f, 82f to 184f),
        listOf(244f to 222f, 228f to 206f, 236f to 198f, 218f to 182f),
        listOf(150f to 34f, 138f to 58f, 148f to 62f, 136f to 82f),
    )
    for (bolt in bolts) {
        val path = Path()
        bolt.forEachIndexed { index, (x, y) ->
            if (index == 0) path.moveTo(x * u, y * u) else path.lineTo(x * u, y * u)
        }
        drawPath(
            path,
            Electric.copy(alpha = alpha * 0.22f),
            style = Stroke(width = 8f * u, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
        drawPath(
            path,
            Electric.copy(alpha = alpha),
            style = Stroke(width = 2.6f * u, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}

private fun DrawScope.drawSweatDrop(cx: Float, cy: Float, alpha: Float, u: Float) {
    val a = alpha * 0.9f
    val path = Path().apply {
        moveTo(cx * u, (cy - 8f) * u)
        quadraticBezierTo((cx + 7f) * u, (cy + 2f) * u, (cx + 4f) * u, (cy + 7f) * u)
        quadraticBezierTo(cx * u, (cy + 12f) * u, (cx - 4f) * u, (cy + 7f) * u)
        quadraticBezierTo((cx - 7f) * u, (cy + 2f) * u, cx * u, (cy - 8f) * u)
    }
    drawPath(path, Sweat.copy(alpha = a))
    oval(cx - 1.5f, cy + 2f, 1.2f, 2f, Color.White.copy(alpha = a * 0.7f), u)
}

private fun DrawScope.star(cx: Float, cy: Float, size: Float, alpha: Float, u: Float) {
    val path = Path()
    val points = 8
    for (i in 0 until points) {
        val angle = (-PI / 2 + i * PI / 4).toFloat()
        val radius = if (i % 2 == 0) size else size * 0.28f
        val x = cx + cos(angle) * radius
        val y = cy + sin(angle) * radius
        if (i == 0) path.moveTo(x * u, y * u) else path.lineTo(x * u, y * u)
    }
    path.close()
    drawPath(path, Spark.copy(alpha = alpha))
    oval(cx, cy, size * 0.18f, size * 0.18f, Color.White.copy(alpha = alpha), u)
}

private fun DrawScope.oval(
    cx: Float,
    cy: Float,
    rx: Float,
    ry: Float,
    color: Color,
    u: Float,
) {
    drawOval(
        color = color,
        topLeft = Offset((cx - rx) * u, (cy - ry) * u),
        size = Size(2f * rx * u, 2f * ry * u),
    )
}

private fun DrawScope.ovalOutlined(
    cx: Float,
    cy: Float,
    rx: Float,
    ry: Float,
    fill: Color,
    outline: Color,
    u: Float,
    width: Float,
) {
    oval(cx, cy, rx, ry, fill, u)
    drawOval(
        color = outline,
        topLeft = Offset((cx - rx) * u, (cy - ry) * u),
        size = Size(2f * rx * u, 2f * ry * u),
        style = Stroke(width = width * u),
    )
}

private fun DrawScope.line(
    x1: Float,
    y1: Float,
    x2: Float,
    y2: Float,
    color: Color,
    u: Float,
    width: Float,
) {
    drawLine(
        color = color,
        start = Offset(x1 * u, y1 * u),
        end = Offset(x2 * u, y2 * u),
        strokeWidth = width * u,
        cap = StrokeCap.Round,
    )
}

private fun DrawScope.arc(
    cx: Float,
    cy: Float,
    rx: Float,
    ry: Float,
    start: Float,
    sweep: Float,
    color: Color,
    u: Float,
    width: Float,
) {
    drawArc(
        color = color,
        startAngle = start,
        sweepAngle = sweep,
        useCenter = false,
        topLeft = Offset((cx - rx) * u, (cy - ry) * u),
        size = Size(2f * rx * u, 2f * ry * u),
        style = Stroke(width = width * u, cap = StrokeCap.Round),
    )
}

private fun quad(p0: Float, p1: Float, p2: Float, t: Float): Float {
    val inv = 1f - t
    return inv * inv * p0 + 2f * inv * t * p1 + t * t * p2
}

private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t
