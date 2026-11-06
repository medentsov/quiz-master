package ru.viktorinavopros.quiz.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.jsoup.HttpStatusException
import org.jsoup.Jsoup
import java.io.File
import java.io.IOException

class SiteRepository(private val context: Context) {

    companion object {
        private const val BASE_URL = "https://viktorinavopros.ru/wp-json/wp/v2"
        private const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/120.0 Mobile Safari/537.36 QuizMasterApp/1.0"
        private const val POSTS_FILE = "posts_cache.json"
        private const val CATEGORIES_FILE = "categories_cache.json"
        private const val META_FILE = "cache_meta.txt"
    }

    private val mutex = Mutex()
    private val cacheDir: File by lazy {
        File(context.filesDir, "quiz_cache").also { it.mkdirs() }
    }

    suspend fun loadCached(): QuizData? = withContext(Dispatchers.IO) {
        val posts = readCache(POSTS_FILE) ?: return@withContext null
        val categories = readCache(CATEGORIES_FILE) ?: "[]"
        val fetchedAt = readCache(META_FILE)?.toLongOrNull() ?: 0L
        runCatching { buildData(posts, categories, fetchedAt) }.getOrNull()
            ?.takeIf { it.tests.isNotEmpty() }
    }

    suspend fun refresh(): QuizData = withContext(Dispatchers.IO) {
        mutex.withLock {
            val categoriesJson = fetchCategories()
            val postsJson = fetchPosts()
            val data = buildData(postsJson, categoriesJson, System.currentTimeMillis())
            if (data.tests.isEmpty()) {
                throw IOException("На сайте не найдено ни одного теста с ответами")
            }
            writeCache(CATEGORIES_FILE, categoriesJson)
            writeCache(POSTS_FILE, postsJson)
            writeCache(META_FILE, data.fetchedAt.toString())
            data
        }
    }

    private fun buildData(
        postsJson: String,
        categoriesJson: String,
        fetchedAt: Long,
    ): QuizData {
        val categories = mutableListOf<SiteCategory>()
        val categoryArray = JSONArray(categoriesJson)
        for (index in 0 until categoryArray.length()) {
            val item = categoryArray.getJSONObject(index)
            categories += SiteCategory(
                id = item.getInt("id"),
                name = decodeHtml(item.optString("name")),
                parentId = item.optInt("parent", 0),
            )
        }

        val categoryNames = categories.associate { it.id to it.name }
        val tests = mutableListOf<QuizTest>()
        val posts = JSONArray(postsJson)
        for (index in 0 until posts.length()) {
            val item = posts.getJSONObject(index)
            val id = item.optInt("id", 0)
            if (id == 0) continue
            val titleHtml = item.optJSONObject("title")?.optString("rendered").orEmpty()
            val link = item.optString("link")
            val content = item.optJSONObject("content")?.optString("rendered").orEmpty()
            if (content.isBlank()) continue

            val categoryIds = mutableListOf<Int>()
            item.optJSONArray("categories")?.let { array ->
                for (categoryIndex in 0 until array.length()) {
                    categoryIds += array.optInt(categoryIndex, 0)
                }
            }
            val names = categoryIds.mapNotNull { categoryNames[it] }

            QuizParser.parseTest(
                id = id,
                titleHtml = titleHtml,
                url = link,
                categoryIds = categoryIds,
                categoryNames = names,
                html = content,
            )?.let { tests += it }
        }

        return QuizData(
            tests = tests.sortedByDescending { it.id },
            categories = categories,
            fetchedAt = fetchedAt,
        )
    }

    private fun fetchCategories(): String {
        val all = JSONArray()
        var page = 1
        while (page <= 5) {
            val url = "$BASE_URL/categories?per_page=100&page=$page" +
                "&_fields=id,name,parent,count"
            val body = try {
                httpGet(url)
            } catch (error: HttpStatusException) {
                if (page > 1 && error.statusCode == 400) break
                throw error
            }
            val array = JSONArray(body)
            if (array.length() == 0) break
            for (index in 0 until array.length()) all.put(array.get(index))
            if (array.length() < 100) break
            page++
        }
        return all.toString()
    }

    private fun fetchPosts(): String {
        val all = JSONArray()
        var page = 1
        while (page <= 10) {
            val url = "$BASE_URL/posts?per_page=100&page=$page" +
                "&_fields=id,title,link,categories,content"
            val body = try {
                httpGet(url)
            } catch (error: HttpStatusException) {
                // WordPress отвечает 400, когда страницы закончились.
                if (page > 1 && error.statusCode == 400) break
                throw error
            }
            val array = JSONArray(body)
            if (array.length() == 0) break
            for (index in 0 until array.length()) all.put(array.get(index))
            if (array.length() < 100) break
            page++
        }
        return all.toString()
    }

    private fun httpGet(url: String): String {
        return Jsoup.connect(url)
            .userAgent(USER_AGENT)
            .header("Accept", "application/json")
            .header("Accept-Language", "ru-RU,ru;q=0.9")
            .timeout(30_000)
            .maxBodySize(0)
            .ignoreContentType(true)
            .execute()
            .body()
    }

    private fun readCache(name: String): String? {
        val file = File(cacheDir, name)
        if (!file.exists()) return null
        return runCatching { file.readText() }.getOrNull()?.takeIf { it.isNotBlank() }
    }

    private fun writeCache(name: String, content: String) {
        runCatching { File(cacheDir, name).writeText(content) }
    }

    private fun decodeHtml(html: String): String = Jsoup.parse(html).text().trim()
}
