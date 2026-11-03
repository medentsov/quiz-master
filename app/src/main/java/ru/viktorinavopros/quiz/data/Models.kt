package ru.viktorinavopros.quiz.data

/**
 * Темы определяются автоматически по ключевым словам в вопросе и ответе.
 * Это эвристика: сложные вопросы «Что? Где? Когда?» иногда попадают в «Разное».
 */
enum class Topic(
    val title: String,
    val emoji: String,
    val description: String,
) {
    SPORT("Спорт", "⚽", "Игры, турниры и рекорды"),
    HISTORY("История", "🏛", "События, эпохи и личности"),
    GEOGRAPHY("География", "🌍", "Страны, города и природа"),
    SCIENCE("Наука и техника", "🔬", "Открытия, космос и технологии"),
    CINEMA("Кино и мультфильмы", "🎬", "Фильмы, сериалы и герои"),
    MUSIC("Музыка", "🎵", "Песни, исполнители и жанры"),
    LITERATURE("Литература", "📚", "Книги, авторы и персонажи"),
    ART("Искусство и культура", "🎨", "Живопись, театр и музеи"),
    USSR("СССР", "★", "Советский быт и история"),
    HOLIDAYS("Праздники и традиции", "🎉", "Обычаи и торжества"),
    NATURE("Природа и животные", "🐾", "Животные, растения и климат"),
    FOOD("Еда и напитки", "🍽", "Кухни, продукты и рецепты"),
    RUSSIAN("Русский язык", "✍", "Слова, фразеологизмы и загадки"),
    MIXED("Разное", "🎲", "Вопросы на общую эрудицию"),
}

data class QuizQuestion(
    val id: String,
    val postId: Int,
    val postTitle: String,
    val number: Int,
    val text: String,
    val answer: String,
    val topic: Topic,
    val sourceUrl: String,
    /** Готовые варианты с сайта (интерактивные тесты Quiz Master Next). */
    val options: List<String> = emptyList(),
    /** Индекс верного варианта в [options], если варианты пришли с сайта. */
    val correctIndex: Int = -1,
    /** Пояснение к ответу, если оно есть на сайте. */
    val explanation: String = "",
    /** Картинка к вопросу (вопросы-картинки). */
    val imageUrl: String? = null,
)

data class QuizTest(
    val id: Int,
    val title: String,
    val url: String,
    val categoryIds: List<Int>,
    val questions: List<QuizQuestion>,
) {
    val topics: List<Topic>
        get() = questions.map { it.topic }.distinct().sortedBy { it.ordinal }
}

data class SiteCategory(
    val id: Int,
    val name: String,
    val parentId: Int,
)

data class QuizData(
    val tests: List<QuizTest>,
    val categories: List<SiteCategory>,
    val fetchedAt: Long,
) {
    val allQuestions: List<QuizQuestion> get() = tests.flatMap { it.questions }

    val questionCount: Int get() = tests.sumOf { it.questions.size }
}

enum class AnswerMode {
    MULTIPLE_CHOICE,
    SELF_CHECK,
}

data class QuizSettings(
    val answerMode: AnswerMode = AnswerMode.MULTIPLE_CHOICE,
    val topicQuestionCount: Int = 10,
    val shuffleQuestions: Boolean = true,
)
