package ru.viktorinavopros.quiz

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.viktorinavopros.quiz.data.QuizQuestion
import ru.viktorinavopros.quiz.data.Topic
import ru.viktorinavopros.quiz.game.BoardCell
import ru.viktorinavopros.quiz.game.BoardCategory
import ru.viktorinavopros.quiz.game.GameFactory
import ru.viktorinavopros.quiz.game.GameState

class GameStateTest {

    private fun question(id: String) = QuizQuestion(
        id = id,
        postId = 1,
        postTitle = "Тест",
        number = 1,
        text = "Вопрос $id",
        answer = "Ответ $id",
        topic = Topic.MIXED,
        sourceUrl = "",
    )

    private fun cell(id: String, value: Int) = BoardCell(
        id = id,
        topic = Topic.MIXED,
        value = value,
        question = question(id),
        options = listOf("Верно", "Неверно", "Другое", "Ещё"),
        correctIndex = 0,
    )

    private fun game(vararg cells: Pair<String, Int>) = GameState(
        players = GameFactory.createPlayers(3),
        categories = listOf(
            BoardCategory(
                topic = Topic.MIXED,
                cells = cells.map { (id, value) -> cell(id, value) },
            ),
        ),
    )

    @Test
    fun `correct answer adds value and rotates player`() {
        var state = game("c1" to 100, "c2" to 200)
        state = state.openCell("c1")
        state = state.answer(0)

        assertEquals(100, state.players[0].score)
        assertEquals(1, state.players[0].correct)
        assertEquals(0, state.players[0].wrong)

        state = state.continueAfterAnswer()
        assertEquals(1, state.currentPlayerIndex)
        assertNull(state.activeCell)
        assertFalse(state.finished)
    }

    @Test
    fun `wrong answer subtracts value`() {
        var state = game("c1" to 300)
        state = state.openCell("c1").answer(1)

        assertEquals(-300, state.players[0].score)
        assertEquals(0, state.players[0].correct)
        assertEquals(1, state.players[0].wrong)
    }

    @Test
    fun `turn cycles through players`() {
        var state = game("c1" to 100, "c2" to 200)

        state = state.openCell("c1").answer(0).continueAfterAnswer()
        assertEquals(1, state.currentPlayerIndex)

        state = state.openCell("c2").answer(0).continueAfterAnswer()
        assertEquals(200, state.players[1].score)
        assertTrue(state.finished)
    }
}
