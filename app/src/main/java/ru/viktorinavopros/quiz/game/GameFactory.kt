package ru.viktorinavopros.quiz.game

import ru.viktorinavopros.quiz.data.QuizData
import ru.viktorinavopros.quiz.data.QuizQuestion
import ru.viktorinavopros.quiz.data.Topic
import java.util.Locale
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
        val allQuestions = data.allQuestions
        val grouped = allQuestions.groupBy { it.topic }
        // Готовим варианты один раз на всю игру: иначе для каждого вопроса
        // заново разбираются ответы всех 1300+ вопросов, и старт игры подвисает.
        val candidates = allQuestions.mapNotNull { question ->
            val text = distractorText(question.answer)
            if (text.isBlank()) {
                null
            } else {
                AnswerCandidate(
                    id = question.id,
                    postId = question.postId,
                    topic = question.topic,
                    text = text,
                    norm = normalize(text),
                )
            }
        }
        return topics.map { topic ->
            val questions = grouped[topic]
                .orEmpty()
                .shuffled(random)
                .take(CELLS_PER_CATEGORY)
            BoardCategory(
                topic = topic,
                cells = questions.mapIndexed { index, question ->
                    val optionSet = buildOptions(question, candidates, random)
                    BoardCell(
                        id = "${topic.name}-${VALUES[index]}",
                        topic = topic,
                        value = VALUES[index],
                        question = question,
                        options = optionSet.options,
                        correctIndex = optionSet.correctIndex,
                    )
                },
            )
        }
    }

    fun createGame(
        data: QuizData,
        topics: List<Topic>,
        players: List<Player>,
        random: Random = Random.Default,
    ): GameState = GameState(
        players = players.ifEmpty { createPlayers(1) },
        categories = createBoard(data, topics, random),
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

    /** Вариант-обманка: коротко и без пояснений в скобках. */
    private fun distractorText(full: String): String {
        val text = shortAnswer(full)
        val trimmed = text.replace(Regex("""\s*\([^()]*\)"""), "").trim()
        return trimmed.ifBlank { text }
    }

    fun normalize(text: String): String = text
        .lowercase(Locale.ROOT)
        .replace('ё', 'е')
        .replace(Regex("""[^а-яa-z0-9 ]"""), " ")
        .replace(Regex("""\s+"""), " ")
        .trim()

    private data class OptionSet(
        val options: List<String>,
        val correctIndex: Int,
    )

    private data class AnswerCandidate(
        val id: String,
        val postId: Int,
        val topic: Topic,
        val text: String,
        val norm: String,
    )

    private fun buildOptions(
        question: QuizQuestion,
        candidates: List<AnswerCandidate>,
        random: Random,
    ): OptionSet {
        // Интерактивные тесты приходят с готовыми вариантами и верным ответом.
        if (question.options.size >= 2 && question.correctIndex in question.options.indices) {
            val correct = question.options[question.correctIndex]
            val options = question.options.shuffled(random)
            return OptionSet(options, options.indexOf(correct))
        }

        val correct = shortAnswer(question.answer)
        val correctNorm = normalize(correct)
        val minLength = maxOf(2, (correct.length * 0.45f).toInt())
        val maxLength = maxOf(24, (correct.length * 2.2f).toInt())

        fun pick(source: List<AnswerCandidate>, limit: Int): List<AnswerCandidate> = source
            .asSequence()
            .filter { it.id != question.id }
            .filter { it.text.length in minLength..maxLength }
            .filter { it.norm != correctNorm }
            .shuffled(random)
            .take(limit)
            .toList()

        // Сначала ответы из этого же теста: они в том же стиле и той же длины.
        val samePost = pick(candidates.filter { it.postId == question.postId }, 40)
        val sameTopic = pick(candidates.filter { it.topic == question.topic }, 160)
        val otherTopics = pick(candidates.filter { it.topic != question.topic }, 120)

        val distractors = (samePost + sameTopic + otherTopics)
            .asSequence()
            .distinctBy { it.norm }
            .sortedByDescending { optionAffinity(correct, it.text) }
            .take(3)
            .map { it.text }
            .toMutableList()

        val fallbacks = listOf("Затрудняюсь ответить", "Ни один из вариантов", "Другое")
        for (fallback in fallbacks) {
            if (distractors.size >= 3) break
            if (normalize(fallback) == normalize(correct)) continue
            if (distractors.any { normalize(it) == normalize(fallback) }) continue
            distractors += fallback
        }

        val options = (distractors + correct).shuffled(random)
        return OptionSet(options, options.indexOf(correct))
    }

    /**
     * Насколько вариант похож по форме на правильный ответ: длина, цифры,
     * кавычки, число слов. Это делает неверные варианты правдоподобнее.
     */
    private fun optionAffinity(correct: String, candidate: String): Int {
        var score = 0
        val lengthDiff = kotlin.math.abs(correct.length - candidate.length)
        score += when {
            lengthDiff <= 2 -> 5
            lengthDiff <= 5 -> 3
            lengthDiff <= 10 -> 1
            else -> 0
        }
        val correctDigits = correct.any { it.isDigit() }
        val candidateDigits = candidate.any { it.isDigit() }
        if (correctDigits == candidateDigits) score += if (correctDigits) 3 else 1

        val correctQuoted = correct.contains('"') || correct.contains('«')
        val candidateQuoted = candidate.contains('"') || candidate.contains('«')
        if (correctQuoted == candidateQuoted) score += if (correctQuoted) 3 else 1

        val correctWords = correct.trim().split(Regex("""\s+""")).size
        val candidateWords = candidate.trim().split(Regex("""\s+""")).size
        score += when {
            correctWords == candidateWords -> 3
            kotlin.math.abs(correctWords - candidateWords) == 1 -> 1
            else -> 0
        }

        val correctFirst = correct.firstOrNull()?.uppercaseChar()
        val candidateFirst = candidate.firstOrNull()?.uppercaseChar()
        if (correctFirst != null && correctFirst == candidateFirst) score += 1
        return score
    }
}
