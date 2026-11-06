package ru.viktorinavopros.quiz.game

import ru.viktorinavopros.quiz.data.QuizData
import ru.viktorinavopros.quiz.data.Topic
import kotlin.random.Random

data class TopicInfo(
    val topic: Topic,
    val count: Int,
)

object GameFactory {

    const val CATEGORIES_PER_GAME = 6
    const val MIN_CATEGORIES = 1
    const val CELLS_PER_CATEGORY = 5
    const val MAX_PLAYERS = 12
    const val MIN_PLAYERS = 1

    val VALUES = listOf(100, 200, 300, 400, 500)

    val PLAYER_COLORS = listOf(
        0xFFE53935L, 0xFF43A047L, 0xFFFDD835L, 0xFF1E88E5L,
        0xFF8E24AAL, 0xFFFB8C00L, 0xFF00ACC1L, 0xFFD81B60L,
        0xFF7CB342L, 0xFF6D4C41L, 0xFF3949ABL, 0xFF00897BL,
    )

    fun availableTopics(data: QuizData): List<TopicInfo> =
        data.allQuestions
            .groupBy { it.topic }
            .map { (topic, questions) -> TopicInfo(topic, questions.size) }
            .filter { it.count >= CELLS_PER_CATEGORY }
            .sortedWith(compareByDescending<TopicInfo> { it.count }.thenBy { it.topic.ordinal })

    fun defaultTopics(data: QuizData): List<Topic> {
        val available = availableTopics(data)
        val preferred = listOf(
            Topic.LITERATURE,
            Topic.GEOGRAPHY,
            Topic.NATURE,
            Topic.SPORT,
            Topic.CINEMA,
            Topic.SCIENCE,
        )
        val result = preferred
            .filter { topic -> available.any { it.topic == topic } }
            .toMutableList()
        for (info in available) {
            if (result.size >= CATEGORIES_PER_GAME) break
            if (info.topic !in result) result += info.topic
        }
        return result.take(CATEGORIES_PER_GAME)
    }

    fun createPlayers(count: Int, names: List<String> = emptyList()): List<Player> {
        val safeCount = count.coerceIn(MIN_PLAYERS, MAX_PLAYERS)
        return (0 until safeCount).map { index ->
            Player(
                id = index,
                name = names.getOrNull(index)?.trim()?.takeIf { it.isNotEmpty() }
                    ?: "Игрок ${index + 1}",
                color = PLAYER_COLORS[index % PLAYER_COLORS.size],
            )
        }
    }

    fun createBoard(
        data: QuizData,
        topics: List<Topic>,
        random: Random = Random.Default,
    ): List<BoardCategory> {
        val grouped = data.allQuestions.groupBy { it.topic }
        return topics.map { topic ->
            val questions = grouped[topic]
                .orEmpty()
                .shuffled(random)
                .take(CELLS_PER_CATEGORY)
            BoardCategory(
                topic = topic,
                cells = questions.mapIndexed { index, question ->
                    BoardCell(
                        id = "${topic.name}-${VALUES[index]}",
                        topic = topic,
                        value = VALUES[index],
                        question = question,
                    )
                },
            )
        }
    }

    fun createGame(
        data: QuizData,
        topics: List<Topic>,
        players: List<Player>,
        timerSeconds: Int = 0,
        random: Random = Random.Default,
    ): GameState = GameState(
        players = players.ifEmpty { createPlayers(1) },
        categories = createBoard(data, topics, random),
        timerSeconds = timerSeconds.coerceIn(0, 60),
    )

    fun shortAnswer(full: String): String {
        val clean = full.replace(Regex("""\s+"""), " ").trim()
        if (clean.isEmpty()) return "—"
        // Берём первое предложение, если оно не похоже на инициалы («И. В. Сталин»).
        val candidate = Regex("""^(.{2,70}?[.!?])(?=\s|$)""")
            .find(clean)
            ?.groupValues
            ?.get(1)
        val isInitial = candidate != null &&
            Regex("""^(?:[A-ZА-ЯЁ]\.\s*)+$""").matches(candidate)
        val sentence = candidate?.takeIf { !isInitial }
        val base = sentence ?: clean
        if (base.length <= 70) return base
        val cut = base.take(70).substringBeforeLast(' ').trimEnd()
        return (cut.ifBlank { base.take(70) }) + "…"
    }

}
