package ru.viktorinavopros.quiz.data

import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode

/**
 * Разбирает HTML поста WordPress:
 *  - обычные тесты: вопросы — абзацы/пункты с номером, ответы — скрытый блок «Ответы»;
 *  - вопросы-картинки: изображения сопоставляются с ответами по порядку/номеру;
 *  - интерактивные тесты Quiz Master Next: варианты и верный ответ лежат в JSON.
 */
object QuizParser {

    /**
     * Вопросы-картинки грузятся с CDN ВКонтакте и часто недоступны. Пока
     * картинка не проверена, такие вопросы считаются неполными и в игру не
     * попадают: лучше меньше вопросов, чем вопрос без условия.
     */
    private const val INCLUDE_IMAGE_QUESTIONS = false

    private val NUMBERED = Regex(
        """^(?:вопрос\s*)?(\d{1,3})\s*[.)]\s*(.+)$""",
        RegexOption.IGNORE_CASE,
    )

    private val QUESTION_LABEL = Regex(
        """^(?:вопрос|картинка|изображение)\s*№?\s*(\d{1,3})\s*[.:]?\s*$""",
        RegexOption.IGNORE_CASE,
    )

    private val BLOCK_TAGS = setOf(
        "p", "div", "li", "h1", "h2", "h3", "h4", "h5", "h6",
        "td", "th", "tr", "table", "blockquote", "section", "article",
    )

    private val QUESTION_TAGS = setOf("p", "li", "h1", "h2", "h3", "h4", "td", "th")

    private val CATEGORY_TOPICS = mapOf(
        "кино" to Topic.CINEMA,
        "назад в ссср" to Topic.USSR,
        "праздники" to Topic.HOLIDAYS,
        "литературная викторина" to Topic.LITERATURE,
        "фразеологизмы" to Topic.RUSSIAN,
        "спорт" to Topic.SPORT,
        "литература" to Topic.LITERATURE,
    )

    fun parseTest(
        id: Int,
        titleHtml: String,
        url: String,
        categoryIds: List<Int>,
        categoryNames: List<String>,
        html: String,
    ): QuizTest? {
        val doc = Jsoup.parse(html)
        val title = Jsoup.parse(titleHtml).text().trim()
        val siteTopic = resolveSiteTopic(title, categoryNames)

        parseInteractive(doc, id, title, url, categoryIds, siteTopic)?.let { return it }
        return parseClassic(doc, id, title, url, categoryIds, siteTopic)
    }

    // ---------------------------------------------------------------- обычные тесты

    private fun parseClassic(
        doc: org.jsoup.nodes.Document,
        id: Int,
        title: String,
        url: String,
        categoryIds: List<Int>,
        siteTopic: Topic?,
    ): QuizTest? {
        val answers = LinkedHashMap<Int, String>()
        val answersBlock = doc.selectFirst("div[id^=post-content-]")
        if (answersBlock != null) {
            for (line in extractLines(answersBlock)) {
                val parsed = parseNumbered(line) ?: continue
                answers.putIfAbsent(parsed.first, parsed.second)
            }
            answersBlock.remove()
        }
        if (answers.isEmpty()) return null

        val questions = LinkedHashMap<Int, String>()
        for (element in doc.select("p, li, h1, h2, h3, h4, td")) {
            for (line in extractLines(element)) {
                val parsed = parseNumbered(line) ?: continue
                if (parsed.second.length >= 8) {
                    questions.putIfAbsent(parsed.first, parsed.second)
                }
            }
        }

        // Редкий случай: вопрос лежит в div без абзацев.
        if (questions.size < answers.size) {
            for (element in doc.select("div")) {
                if (element.children().any { it.tagName().lowercase() in BLOCK_TAGS }) continue
                for (line in extractLines(element)) {
                    val parsed = parseNumbered(line) ?: continue
                    if (parsed.second.length >= 8) {
                        questions.putIfAbsent(parsed.first, parsed.second)
                    }
                }
            }
        }

        val images = collectImages(doc)
        val imageByNumber = LinkedHashMap<Int, String>()
        var currentNumber: Int? = null
        for (element in doc.getAllElements()) {
            val tag = element.tagName().lowercase()
            if (tag == "img") {
                val src = cleanImageUrl(element)
                if (src != null) {
                    currentNumber?.let { imageByNumber.putIfAbsent(it, src) }
                }
                continue
            }
            if (tag in QUESTION_TAGS) {
                val text = element.text().trim()
                parseNumbered(text)?.let { currentNumber = it.first }
                QUESTION_LABEL.find(text)?.let {
                    currentNumber = it.groupValues[1].toIntOrNull()
                }
            }
        }

        if (questions.isEmpty()) {
            return parsePictureQuiz(
                id = id,
                title = title,
                url = url,
                categoryIds = categoryIds,
                siteTopic = siteTopic,
                answers = answers,
                images = images,
                doc = doc,
            )
        }

        val parsedQuestions = questions.keys.sorted().mapNotNull { number ->
            val text = questions[number] ?: return@mapNotNull null
            val answer = answers[number] ?: return@mapNotNull null
            QuizQuestion(
                id = "$id-$number",
                postId = id,
                postTitle = title,
                number = number,
                text = text,
                answer = answer,
                topic = TopicClassifier.classify("$text $answer", siteTopic),
                sourceUrl = url,
                explanation = answer,
                imageUrl = imageByNumber[number],
            )
        }.filter { it.isPlayable() }

        if (parsedQuestions.isEmpty()) return null
        return QuizTest(
            id = id,
            title = title,
            url = url,
            categoryIds = categoryIds,
            questions = parsedQuestions,
        )
    }

    /**
     * Тесты, где вопрос — это картинка (или подборка картинок), а текст только в ответах.
     */
    private fun parsePictureQuiz(
        id: Int,
        title: String,
        url: String,
        categoryIds: List<Int>,
        siteTopic: Topic?,
        answers: Map<Int, String>,
        images: List<Pair<Int?, String>>,
        doc: org.jsoup.nodes.Document,
    ): QuizTest? {
        // Без картинки такой вопрос не имеет условия.
        if (!INCLUDE_IMAGE_QUESTIONS) return null
        if (answers.isEmpty() || images.isEmpty()) return null

        val intro = doc.select("p")
            .map { it.text().trim() }
            .firstOrNull {
                it.length in 15..220 &&
                    !it.startsWith("Ответ", ignoreCase = true) &&
                    !it.startsWith("Другие вопросы", ignoreCase = true) &&
                    !it.startsWith("—", ignoreCase = true)
            }
            .orEmpty()

        val labelled = images.filter { it.first != null }
        val pairs: List<Pair<Int, String>> = if (
            labelled.size == images.size && labelled.all { answers.containsKey(it.first) }
        ) {
            labelled.map { it.first!! to it.second }
        } else {
            val keys = answers.keys.sorted()
            images.take(keys.size).mapIndexed { index, image -> keys[index] to image.second }
        }
        if (pairs.isEmpty()) return null

        val parsed = pairs.distinctBy { it.first }.mapNotNull { (number, imageUrl) ->
            val answer = answers[number] ?: return@mapNotNull null
            val text = if (intro.isBlank()) {
                "Что изображено на картинке №$number?"
            } else {
                "$intro Картинка №$number."
            }
            QuizQuestion(
                id = "$id-$number",
                postId = id,
                postTitle = title,
                number = number,
                text = text,
                answer = answer,
                topic = TopicClassifier.classify("$text $answer", siteTopic),
                sourceUrl = url,
                explanation = answer,
                imageUrl = imageUrl,
            )
        }
        if (parsed.isEmpty()) return null
        return QuizTest(
            id = id,
            title = title,
            url = url,
            categoryIds = categoryIds,
            questions = parsed,
        )
    }

    private fun collectImages(doc: org.jsoup.nodes.Document): List<Pair<Int?, String>> {
        val result = mutableListOf<Pair<Int?, String>>()
        var current: Int? = null
        for (element in doc.getAllElements()) {
            val tag = element.tagName().lowercase()
            if (tag == "img") {
                val src = cleanImageUrl(element) ?: continue
                if (result.lastOrNull()?.second != src) result += current to src
            } else if (tag in QUESTION_TAGS) {
                val text = element.text().trim()
                parseNumbered(text)?.let { current = it.first }
                QUESTION_LABEL.find(text)?.let { current = it.groupValues[1].toIntOrNull() }
            }
        }
        return result
    }

    private fun cleanImageUrl(element: Element): String? {
        val src = element.attr("src").trim()
        if (src.isEmpty() || src.startsWith("data:")) return null
        if (!src.startsWith("http")) return null
        val classes = element.className().lowercase()
        if ("emoji" in classes || "avatar" in classes || "icon" in classes) return null
        val width = element.attr("width").toIntOrNull()
        val height = element.attr("height").toIntOrNull()
        if (width != null && width < 80) return null
        if (height != null && height < 80) return null
        return src
    }

    // ------------------------------------------------------- интерактивные тесты QMN

    private fun parseInteractive(
        doc: org.jsoup.nodes.Document,
        id: Int,
        title: String,
        url: String,
        categoryIds: List<Int>,
        siteTopic: Topic?,
    ): QuizTest? {
        val json = doc.select("script")
            .asSequence()
            .mapNotNull { extractQuizJson(it.data()) }
            .firstOrNull()
            ?: return null
        val root = runCatching { JSONObject(json) }.getOrNull() ?: return null
        val list = root.optJSONObject("question_list") ?: return null

        data class Parsed(
            val order: Int,
            val question: QuizQuestion,
        )

        val parsed = mutableListOf<Parsed>()
        val keys = list.keys().asSequence().toList()
        keys.forEachIndexed { fallbackOrder, key ->
            val item = list.optJSONObject(key) ?: return@forEachIndexed
            val settings = parsePhpMap(item.optString("question_settings")).orEmpty()
            val answerArray = parsePhpMap(item.optString("answer_array")) ?: return@forEachIndexed
            if (answerArray.isEmpty()) return@forEachIndexed

            val options = mutableListOf<String>()
            var correctIndex = -1
            for (index in 0 until answerArray.size) {
                val entry = answerArray[index.toString()] as? Map<*, *> ?: continue
                val text = cleanHtml(entry["0"]?.toString().orEmpty())
                if (text.isBlank()) continue
                val correct = (entry["2"] as? Number)?.toInt() ?: 0
                if (correct != 0) correctIndex = options.size
                options += text
            }
            if (options.size < 2 || correctIndex !in options.indices) return@forEachIndexed

            val questionTitle = cleanHtml(
                settings["question_title"]?.toString()
                    ?: item.optString("question_name"),
            )
            if (questionTitle.length < 5) return@forEachIndexed

            val image = settings["featureImageSrc"]?.toString()
                ?.takeIf { it.startsWith("http") }
                ?: firstImage(item.optString("question_name"))
            val info = cleanHtml(item.optString("question_answer_info"))
            val order = item.optString("question_order").toIntOrNull() ?: fallbackOrder

            parsed += Parsed(
                order = order,
                question = QuizQuestion(
                    id = "$id-${item.optString("question_id", key)}",
                    postId = id,
                    postTitle = title,
                    number = order,
                    text = questionTitle,
                    answer = options[correctIndex],
                    topic = TopicClassifier.classify(
                        "$questionTitle ${options[correctIndex]} $info",
                        siteTopic,
                    ),
                    sourceUrl = url,
                    options = options,
                    correctIndex = correctIndex,
                    explanation = info,
                    imageUrl = image,
                ),
            )
        }

        val playable = parsed
            .sortedBy { it.order }
            .map { it.question }
            .filter { it.isPlayable() }
        if (playable.isEmpty()) return null
        return QuizTest(
            id = id,
            title = title,
            url = url,
            categoryIds = categoryIds,
            questions = playable,
        )
    }

    /** Достаёт JSON из `window.qmn_quiz_data["N"] = { ... };`. */
    private fun extractQuizJson(script: String): String? {
        var from = 0
        while (true) {
            val marker = script.indexOf("qmn_quiz_data[", from)
            if (marker < 0) return null
            val start = script.indexOf('{', marker)
            if (start < 0) return null
            var depth = 0
            var inString = false
            var escaped = false
            for (index in start until script.length) {
                val c = script[index]
                if (inString) {
                    when {
                        escaped -> escaped = false
                        c == '\\' -> escaped = true
                        c == '"' -> inString = false
                    }
                } else {
                    when (c) {
                        '"' -> inString = true
                        '{' -> depth++
                        '}' -> {
                            depth--
                            if (depth == 0) return script.substring(start, index + 1)
                        }
                    }
                }
            }
            from = marker + 1
        }
    }

    /** Минимальный PHP-unserialize для строк/чисел/массивов из Quiz Master Next. */
    private fun parsePhpMap(raw: String): Map<String, Any?>? {
        if (raw.isBlank()) return null
        return runCatching {
            val parser = PhpParser(raw)
            @Suppress("UNCHECKED_CAST")
            parser.parseValue() as? Map<String, Any?>
        }.getOrNull()
    }

    private class PhpParser(private val src: String) {
        private var pos = 0

        fun parseValue(): Any? {
            if (pos >= src.length) return null
            return when (src[pos]) {
                'N' -> {
                    pos = (pos + 2).coerceAtMost(src.length)
                    null
                }
                'b' -> {
                    pos += 2
                    val value = src.getOrNull(pos) == '1'
                    pos += 2
                    value
                }
                'i' -> {
                    pos += 2
                    val end = src.indexOf(';', pos)
                    val value = src.substring(pos, end).toIntOrNull() ?: 0
                    pos = end + 1
                    value
                }
                'd' -> {
                    pos += 2
                    val end = src.indexOf(';', pos)
                    val value = src.substring(pos, end).toDoubleOrNull() ?: 0.0
                    pos = end + 1
                    value
                }
                's' -> parseString()
                'a' -> parseArray()
                else -> {
                    pos++
                    null
                }
            }
        }

        private fun parseString(): String {
            pos += 2
            val lengthEnd = src.indexOf(':', pos)
            val length = src.substring(pos, lengthEnd).toIntOrNull() ?: 0
            pos = lengthEnd + 1
            if (src.getOrNull(pos) == '"') pos++
            val start = pos
            var bytes = 0
            while (pos < src.length && bytes < length) {
                val ch = src[pos]
                bytes += when {
                    ch.code < 0x80 -> 1
                    ch.code < 0x800 -> 2
                    Character.isHighSurrogate(ch) -> 4
                    else -> 3
                }
                pos += if (Character.isHighSurrogate(ch) && pos + 1 < src.length) 2 else 1
            }
            val value = src.substring(start, pos)
            if (src.getOrNull(pos) == '"') pos++
            if (src.getOrNull(pos) == ';') pos++
            return value
        }

        private fun parseArray(): Map<String, Any?> {
            pos += 2
            val lengthEnd = src.indexOf(':', pos)
            val count = src.substring(pos, lengthEnd).toIntOrNull() ?: 0
            pos = lengthEnd + 1
            if (src.getOrNull(pos) == '{') pos++
            val map = LinkedHashMap<String, Any?>()
            repeat(count) {
                val key = parseValue()?.toString() ?: ""
                map[key] = parseValue()
            }
            if (src.getOrNull(pos) == '}') pos++
            return map
        }
    }

    private fun firstImage(html: String): String? {
        if (html.isBlank()) return null
        return Jsoup.parse(html).selectFirst("img")?.attr("src")
            ?.takeIf { it.startsWith("http") }
    }

    private fun cleanHtml(html: String): String =
        if (html.isBlank()) "" else Jsoup.parse(html).text().trim()

    /** Вопрос считается полным, только если у него есть условие и ответ. */
    private fun QuizQuestion.isPlayable(): Boolean {
        if (text.length < 8 || answer.isBlank()) return false
        val cleanAnswer = answer.trim()
        // «4.» — обрывок нумерации, а не ответ.
        if (cleanAnswer.matches(Regex("""\d{1,2}\."""))) return false
        if (cleanAnswer.equals("Ответ", ignoreCase = true) ||
            cleanAnswer.equals("Ответы", ignoreCase = true)
        ) {
            return false
        }
        if (!INCLUDE_IMAGE_QUESTIONS && !imageUrl.isNullOrBlank()) return false
        if (options.isNotEmpty()) {
            if (options.size < 2 || correctIndex !in options.indices) return false
            if (options.any { it.isBlank() }) return false
        }
        return true
    }

    // ------------------------------------------------------------------- утилиты

    private fun resolveSiteTopic(title: String, categoryNames: List<String>): Topic? {
        categoryNames.firstNotNullOfOrNull { CATEGORY_TOPICS[it.lowercase()] }?.let { return it }
        val normalized = title.lowercase()
        return when {
            "спорт" in normalized -> Topic.SPORT
            "литератур" in normalized -> Topic.LITERATURE
            "ссср" in normalized -> Topic.USSR
            "кино" in normalized -> Topic.CINEMA
            "праздник" in normalized -> Topic.HOLIDAYS
            "фразеолог" in normalized -> Topic.RUSSIAN
            "выражени" in normalized -> Topic.RUSSIAN
            else -> null
        }
    }

    private fun parseNumbered(line: String): Pair<Int, String>? {
        val match = NUMBERED.find(line) ?: return null
        val number = match.groupValues[1].toIntOrNull() ?: return null
        val text = match.groupValues[2]
            .replace('\u00A0', ' ')
            .replace(Regex("""\s+"""), " ")
            .trim()
            .trimEnd('—', '-', '–')
            .trim()
        if (text.isEmpty()) return null
        return number to text
    }

    private fun extractLines(root: Element): List<String> {
        val lines = mutableListOf<String>()
        val buffer = StringBuilder()

        fun flush() {
            val text = buffer.toString()
                .replace('\u00A0', ' ')
                .replace(Regex("""\s+"""), " ")
                .trim()
            if (text.isNotEmpty()) lines.add(text)
            buffer.clear()
        }

        fun walk(node: Node) {
            when (node) {
                is TextNode -> buffer.append(node.text())
                is Element -> {
                    val tag = node.tagName().lowercase()
                    if (tag == "br") {
                        flush()
                        return
                    }
                    val block = tag in BLOCK_TAGS
                    if (block && buffer.isNotBlank()) flush()
                    for (child in node.childNodes()) walk(child)
                    if (block) flush()
                }
            }
        }

        walk(root)
        flush()
        return lines
    }
}
