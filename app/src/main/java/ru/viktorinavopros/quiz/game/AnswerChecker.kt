package ru.viktorinavopros.quiz.game

import java.text.Normalizer
import java.util.Locale
import kotlin.math.abs
import kotlin.math.min

/**
 * Сравнивает свободный ответ игрока с ответом сайта.
 *
 * Правила:
 *  - регистр, «ё», пунктуация и лишние пробелы не важны;
 *  - порядок слов не важен;
 *  - даты в разных форматах приводятся к одному виду;
 *  - допускается до двух опечаток, но короткие слова сравниваются строже;
 *  - неполный ответ считается верным, если все слова игрока есть в ответе;
 *  - падежные окончания русских слов не считаются ошибкой.
 */
object AnswerChecker {

    private const val MAX_TYPOS = 2
    private const val MIN_WORD_LENGTH = 4

    private val WORD = Regex("""[\p{L}\p{N}]+""")
    private val NUMERIC_DATE = Regex(
        """(?<!\d)(\d{1,2})\s*[./-]\s*(\d{1,2})\s*[./-]\s*(\d{2,4})(?!\d)""",
    )
    private val ISO_DATE = Regex(
        """(?<!\d)(\d{4})\s*-\s*(\d{1,2})\s*-\s*(\d{1,2})(?!\d)""",
    )
    private val PARENTHESES = Regex("""\([^()]*\)""")
    private val ALTERNATIVE_SEPARATOR = Regex("""\s*(?:/|;|\bили\b)\s*""", RegexOption.IGNORE_CASE)
    private val DASH_SEPARATOR = Regex("""\s+[—–-]\s+""")

    private val FILLER_WORDS = setOf(
        "это", "этот", "эта", "эти", "ну", "пожалуйста",
        "ответ", "вариант", "года", "год", "г", "гг", "лет",
        "в", "во", "на", "с", "со", "из", "от", "до", "по", "за",
        "под", "над", "к", "ко", "о", "об", "у", "при", "для",
        "без", "через", "около",
    )

    private val RU_ENDINGS = listOf(
        "иями", "ями", "ами", "ого", "его", "ому", "ему", "ыми", "ими",
        "ых", "их", "ой", "ый", "ий", "ая", "яя", "ое", "ее", "ые", "ие",
        "ым", "им", "ов", "ев", "ах", "ях", "ам", "ям", "ом", "ем",
        "а", "я", "ы", "и", "е", "у", "ю", "о",
    )

    fun isCorrect(userAnswer: String, correctAnswer: String): Boolean {
        if (userAnswer.isBlank() || correctAnswer.isBlank()) return false
        val userTokens = parseAnswer(userAnswer)
        if (userTokens.isEmpty()) return false

        return answerVariants(correctAnswer).any { variant ->
            val correctTokens = parseAnswer(variant)
            correctTokens.isNotEmpty() && covers(userTokens, correctTokens)
        }
    }

    /**
     * Ответ с сайта часто содержит пояснение, альтернативы или текст в скобках.
     * Проверяем несколько «коротких» вариантов, но не весь длинный абзац:
     * иначе слово из пояснения могло бы случайно засчитать неверный ответ.
     */
    private fun answerVariants(full: String): List<String> {
        val primary = GameFactory.shortAnswer(full)
        val variants = linkedSetOf<String>()

        fun add(value: String) {
            val clean = value.trim().trim('.', ',', ';', ':', '!', '?', ' ')
            if (clean.isNotBlank() && clean != "—") variants += clean
        }

        add(primary)
        add(PARENTHESES.replace(primary, " "))
        DASH_SEPARATOR.split(primary).forEach { add(it) }
        ALTERNATIVE_SEPARATOR.split(primary).forEach { add(it) }
        return variants.toList()
    }

    private fun parseAnswer(text: String): List<String> {
        var working = Normalizer.normalize(text, Normalizer.Form.NFKC)
            .lowercase(Locale.ROOT)
            .replace('ё', 'е')

        val dateTokens = mutableListOf<String>()

        working = ISO_DATE.replace(working) { match ->
            val year = match.groupValues[1]
            val month = match.groupValues[2].toIntOrNull()
            val day = match.groupValues[3].toIntOrNull()
            if (month != null && day != null && month in 1..12 && day in 1..31) {
                dateTokens += dateToken(day, month)
                dateTokens += yearToken(year)
                " "
            } else {
                match.value
            }
        }

        working = NUMERIC_DATE.replace(working) { match ->
            val day = match.groupValues[1].toIntOrNull()
            val month = match.groupValues[2].toIntOrNull()
            val year = match.groupValues[3]
            if (month != null && day != null && month in 1..12 && day in 1..31) {
                dateTokens += dateToken(day, month)
                dateTokens += yearToken(year)
                " "
            } else {
                match.value
            }
        }

        val rawTokens = WORD.findAll(working).map { it.value }.toMutableList()
        val tokens = mutableListOf<String>()

        var index = 0
        while (index < rawTokens.size) {
            val token = rawTokens[index]

            val dayFromToken = token.toIntOrNull()
            if (dayFromToken != null && dayFromToken in 1..31 && index + 1 < rawTokens.size) {
                val month = monthNumber(rawTokens[index + 1])
                if (month != null) {
                    var next = index + 2
                    val year = rawTokens.getOrNull(next)?.takeIf { isYear(it) }
                    if (year != null) next++
                    tokens += dateToken(dayFromToken, month)
                    if (year != null) tokens += yearToken(year)
                    index = next
                    continue
                }
            }

            val month = monthNumber(token)
            if (month != null && index + 1 < rawTokens.size) {
                val dayFromNext = rawTokens[index + 1].toIntOrNull()
                if (dayFromNext != null && dayFromNext in 1..31) {
                    var next = index + 2
                    val year = rawTokens.getOrNull(next)?.takeIf { isYear(it) }
                    if (year != null) next++
                    tokens += dateToken(dayFromNext, month)
                    if (year != null) tokens += yearToken(year)
                    index = next
                    continue
                }
            }

            tokens += normalizeStandaloneYear(token)
            index++
        }

        tokens += dateTokens
        return tokens.filterNot { it in FILLER_WORDS }
            .distinct()
    }

    private fun covers(userTokens: List<String>, correctTokens: List<String>): Boolean {
        val used = BooleanArray(correctTokens.size)
        val orderedUser = userTokens.sortedByDescending { it.length }

        fun search(userIndex: Int, budget: Int): Boolean {
            if (userIndex == orderedUser.size) return true
            val user = orderedUser[userIndex]
            for (correctIndex in correctTokens.indices) {
                if (used[correctIndex]) continue
                val cost = matchCost(user, correctTokens[correctIndex]) ?: continue
                if (cost > budget) continue
                used[correctIndex] = true
                if (search(userIndex + 1, budget - cost)) return true
                used[correctIndex] = false
            }
            return false
        }

        return search(0, MAX_TYPOS)
    }

    /**
     * Стоимость сопоставления одного слова: 0 — точное совпадение или тот же
     * корень, 1–2 — опечатки, null — слова не совпали.
     */
    private fun matchCost(user: String, correct: String): Int? {
        if (user == correct) return 0

        if (user.startsWith("@") || correct.startsWith("@")) {
            return dateMatchCost(user, correct)
        }

        // Цифры — значимая часть ответа: 1984 и 1985 не должны совпасть.
        if (user.any { it.isDigit() } || correct.any { it.isDigit() }) return null

        if (stem(user) == stem(correct)) return 0

        val shortest = min(user.length, correct.length)
        val allowed = when {
            shortest < 5 -> 0
            shortest == 5 -> 1
            else -> 2
        }
        if (allowed == 0) return null

        val distance = damerauLevenshtein(user, correct)
        return distance.takeIf { it in 1..allowed }
    }

    private fun dateMatchCost(user: String, correct: String): Int? {
        if (user == correct) return 0
        if (user.startsWith("@y:") && correct.startsWith("@y:")) {
            val a = user.removePrefix("@y:")
            val b = correct.removePrefix("@y:")
            if (a.length == 2 && b.length == 4 && b.endsWith(a)) return 0
            if (b.length == 2 && a.length == 4 && a.endsWith(b)) return 0
        }
        return null
    }

    private fun dateToken(day: Int, month: Int): String = "@d:${day}-${month}"

    private fun yearToken(year: String): String = "@y:$year"

    private fun normalizeStandaloneYear(token: String): String {
        val year = token.toIntOrNull() ?: return token
        return if (token.length == 4 && year in 1000..2100) "@y:$token" else token
    }

    private fun isYear(token: String): Boolean =
        token.length in 2..4 && token.all { it.isDigit() }

    private fun monthNumber(token: String): Int? = when {
        token.startsWith("январ") || token == "янв" -> 1
        token.startsWith("феврал") || token == "фев" -> 2
        token.startsWith("март") || token == "мар" -> 3
        token.startsWith("апрел") || token == "апр" -> 4
        token in setOf("май", "мая", "мае", "ма") -> 5
        token.startsWith("июн") -> 6
        token.startsWith("июл") -> 7
        token.startsWith("август") || token == "авг" -> 8
        token.startsWith("сентябр") || token in setOf("сен", "сент") -> 9
        token.startsWith("октябр") || token == "окт" -> 10
        token.startsWith("ноябр") || token == "ноя" -> 11
        token.startsWith("декабр") || token == "дек" -> 12
        else -> null
    }

    private fun stem(token: String): String {
        if (token.length < MIN_WORD_LENGTH) return token
        for (ending in RU_ENDINGS) {
            if (token.endsWith(ending) && token.length - ending.length >= 3) {
                return token.dropLast(ending.length)
            }
        }
        return token
    }

    /** Расстояние Дамерау — Левенштейна: перестановка соседних букв = 1 ошибка. */
    private fun damerauLevenshtein(first: String, second: String): Int {
        if (abs(first.length - second.length) > MAX_TYPOS) return MAX_TYPOS + 1
        val rows = first.length + 1
        val columns = second.length + 1
        val distance = Array(rows) { IntArray(columns) }

        for (i in 0 until rows) distance[i][0] = i
        for (j in 0 until columns) distance[0][j] = j

        for (i in 1 until rows) {
            for (j in 1 until columns) {
                val cost = if (first[i - 1] == second[j - 1]) 0 else 1
                var value = min(
                    min(distance[i - 1][j] + 1, distance[i][j - 1] + 1),
                    distance[i - 1][j - 1] + cost,
                )
                if (i > 1 && j > 1 &&
                    first[i - 1] == second[j - 2] &&
                    first[i - 2] == second[j - 1]
                ) {
                    value = min(value, distance[i - 2][j - 2] + 1)
                }
                distance[i][j] = value
            }
        }
        return distance[first.length][second.length]
    }
}
