package ru.viktorinavopros.quiz

import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.viktorinavopros.quiz.data.QuizData
import ru.viktorinavopros.quiz.data.QuizParser
import ru.viktorinavopros.quiz.data.Topic
import ru.viktorinavopros.quiz.data.TopicClassifier
import ru.viktorinavopros.quiz.game.GameFactory
import java.io.File

class ParserSmokeTest {

    @Test
    fun `short answer keeps a short first sentence`() {
        assertEquals(
            "Гомер.",
            GameFactory.shortAnswer(
                "Гомер. В первом предложении описывается путь героя через покорение и возвращение.",
            ),
        )
        assertEquals(
            "И. В. Сталин",
            GameFactory.shortAnswer("И. В. Сталин"),
        )
    }

    @Test
    fun `parses questions and answers from real post`() {
        val html = """
            <p>1. В русском языке выражение, означающее «строить безосновательные догадки» связано с напитком. Назовите этот напиток.</p>
            <p>2. В Японии проводится косметическая процедура на ЭТОЙ части тела. На какой части тела проводится процедура?</p>
            <p>3. В рекламе бургеров с ЭТИМ ингредиентом используют слоган «Рыжие. Хитрые. Жареные».<br>4. Элемент декора. «Место сбора» провинившихся. Ответьте одним словом: о чём речь?</p>
            <div id="post-content-1" class="sh-content post-content sh-hide">
                1. Кофе (гадать на кофейной гуще).<br />
                2. Ладони.<br />
                3. Лисички (грибы).<br />
                4. Ковёр.<br />
            </div>
        """.trimIndent()

        val test = QuizParser.parseTest(
            id = 1,
            titleHtml = "Квиз вопросы и ответы",
            url = "https://example.test/quiz",
            categoryIds = emptyList(),
            categoryNames = emptyList(),
            html = html,
        )

        requireNotNull(test)
        assertEquals(4, test.questions.size)
        assertEquals("Кофе (гадать на кофейной гуще).", test.questions[0].answer)
        assertTrue(test.questions[2].text.contains("Рыжие"))
        assertTrue(test.questions[3].text.contains("Элемент декора"))
        assertEquals("Ковёр.", test.questions[3].answer)
    }

    @Test
    fun `classifies obvious topics`() {
        assertEquals(
            Topic.SPORT,
            TopicClassifier.classify("В каком виде спорта используется шайба и клюшка?"),
        )
        assertEquals(
            Topic.CINEMA,
            TopicClassifier.classify("В каком фильме снимался этот актёр и режиссёр?"),
        )
        assertEquals(
            Topic.GEOGRAPHY,
            TopicClassifier.classify("Столица какой страны расположена на реке?"),
        )
        assertEquals(
            Topic.MIXED,
            TopicClassifier.classify("Дайте ответ на этот вопрос."),
        )
    }

    @Test
    fun `builds board with four unique options`() {
        val postsFile = File("/tmp/opencode/posts.json")
        if (!postsFile.exists()) return

        val tests = parsePosts(postsFile)
        val data = QuizData(tests = tests, categories = emptyList(), fetchedAt = 0L)
        val topics = GameFactory.defaultTopics(data)

        assertEquals(6, topics.size)
        val board = GameFactory.createBoard(data, topics)
        assertEquals(6, board.size)
        board.forEach { category ->
            assertEquals(5, category.cells.size)
            category.cells.forEach { cell ->
                assertEquals(4, cell.options.size)
                assertEquals(
                    4,
                    cell.options.map { GameFactory.normalize(it) }.distinct().size,
                )
                assertEquals(
                    GameFactory.normalize(GameFactory.shortAnswer(cell.question.answer)),
                    GameFactory.normalize(cell.options[cell.correctIndex]),
                )
                assertTrue(cell.question.text.isNotBlank())
                assertTrue(cell.question.answer.isNotBlank())
            }
        }
    }

    @Test
    fun `board supports one to six categories`() {
        val postsFile = File("/tmp/opencode/posts.json")
        if (!postsFile.exists()) return

        val tests = parsePosts(postsFile)
        val data = QuizData(tests = tests, categories = emptyList(), fetchedAt = 0L)
        val topics = GameFactory.availableTopics(data).map { it.topic }

        for (count in 1..6) {
            val selected = topics.take(count)
            val board = GameFactory.createBoard(data, selected)
            assertEquals(count, board.size)
            board.forEach { category -> assertEquals(5, category.cells.size) }

            val game = GameFactory.createGame(
                data = data,
                topics = selected,
                players = GameFactory.createPlayers(1),
            )
            assertEquals(count * GameFactory.CELLS_PER_CATEGORY, game.totalCells)
        }
    }

    @Test
    fun `parses all real posts`() {
        val postsFile = File("/tmp/opencode/posts.json")
        if (!postsFile.exists()) return

        val tests = parsePosts(postsFile)
        val questions = tests.flatMap { it.questions }
        val withImages = questions.count { !it.imageUrl.isNullOrBlank() }
        val withSiteOptions = questions.count { it.options.size >= 2 }
        val byTopic = questions.groupingBy { it.topic }.eachCount()
        println("Тестов: ${tests.size}, вопросов: ${questions.size}")
        println("С картинками: $withImages, с готовыми вариантами: $withSiteOptions")
        println("Темы: $byTopic")

        assertTrue("Ожидалось больше 100 тестов, получено ${tests.size}", tests.size > 100)
        assertTrue("Ожидалось больше 1000 вопросов, получено ${questions.size}", questions.size > 1000)
        assertTrue(questions.all { it.text.length >= 8 && it.answer.isNotBlank() })
        assertTrue(tests.all { it.questions.isNotEmpty() })
        // Вопросы с картинками исключены: CDN сайта часто недоступен.
        assertEquals("Вопросы с картинками должны быть исключены", 0, withImages)
        assertTrue("Ожидался интерактивный тест с вариантами", withSiteOptions > 0)
        assertTrue((byTopic[Topic.MIXED] ?: 0) < questions.size)
    }

    private fun parsePosts(file: File): List<ru.viktorinavopros.quiz.data.QuizTest> {
        val array = JSONArray(file.readText())
        val tests = mutableListOf<ru.viktorinavopros.quiz.data.QuizTest>()
        for (index in 0 until array.length()) {
            val item = array.getJSONObject(index)
            val id = item.optInt("id", 0)
            val title = item.optJSONObject("title")?.optString("rendered").orEmpty()
            val link = item.optString("link")
            val content = item.optJSONObject("content")?.optString("rendered").orEmpty()
            if (id == 0 || content.isBlank()) continue
            val categoryIds = mutableListOf<Int>()
            item.optJSONArray("categories")?.let { categories ->
                for (categoryIndex in 0 until categories.length()) {
                    categoryIds += categories.optInt(categoryIndex, 0)
                }
            }
            QuizParser.parseTest(
                id = id,
                titleHtml = title,
                url = link,
                categoryIds = categoryIds,
                categoryNames = emptyList(),
                html = content,
            )?.let { tests += it }
        }
        return tests
    }
}
