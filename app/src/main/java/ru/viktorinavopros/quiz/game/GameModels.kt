package ru.viktorinavopros.quiz.game

import ru.viktorinavopros.quiz.data.QuizQuestion
import ru.viktorinavopros.quiz.data.Topic

data class Player(
    val id: Int,
    val name: String,
    val color: Long,
    val score: Int = 0,
    val correct: Int = 0,
    val wrong: Int = 0,
) {
    val answered: Int get() = correct + wrong
    val accuracy: Int get() = if (answered == 0) 0 else correct * 100 / answered
}

data class BoardCell(
    val id: String,
    val topic: Topic,
    val value: Int,
    val question: QuizQuestion,
    val answered: Boolean = false,
    val answeredBy: Int? = null,
    val answerText: String? = null,
    val timedOut: Boolean = false,
    val isCorrect: Boolean? = null,
)

data class BoardCategory(
    val topic: Topic,
    val cells: List<BoardCell>,
)

data class AnsweredQuestion(
    val cellId: String,
    val topic: Topic,
    val value: Int,
    val question: QuizQuestion,
    val playerId: Int,
    val playerName: String,
    val playerColor: Long,
    val selectedAnswer: String?,
    val correctAnswer: String,
    val isCorrect: Boolean,
    val timedOut: Boolean = false,
    /** Полный ответ с сайта: обычно содержит и ответ, и пояснение. */
    val explanation: String = "",
    val sourceUrl: String = "",
) {
    val delta: Int get() = if (isCorrect) value else -value
}

data class GameState(
    val players: List<Player>,
    val categories: List<BoardCategory>,
    val currentPlayerIndex: Int = 0,
    val activeCell: BoardCell? = null,
    val history: List<AnsweredQuestion> = emptyList(),
    val timerSeconds: Int = 0,
    val finished: Boolean = false,
) {
    val currentPlayer: Player
        get() = players[currentPlayerIndex.coerceIn(0, players.lastIndex)]

    val totalCells: Int
        get() = categories.sumOf { it.cells.size }

    val answeredCells: Int
        get() = categories.sumOf { category -> category.cells.count { it.answered } }

    val allAnswered: Boolean
        get() = totalCells > 0 && answeredCells >= totalCells

    fun openCell(cellId: String): GameState {
        val cell = categories.asSequence()
            .flatMap { it.cells.asSequence() }
            .firstOrNull { it.id == cellId }
            ?: return this
        if (cell.answered) return this
        return copy(activeCell = cell)
    }

    fun answer(text: String): GameState {
        val cell = activeCell ?: return this
        if (cell.answered) return this
        val clean = text.trim()
        if (clean.isEmpty()) return this

        val isCorrect = AnswerChecker.isCorrect(clean, cell.question.answer)
        return recordAnswer(cell, clean, isCorrect, timedOut = false)
    }

    /** Игрок не успел ответить: вопрос закрывается как ошибочный. */
    fun timeout(): GameState {
        val cell = activeCell ?: return this
        if (cell.answered) return this
        return recordAnswer(cell, answer = null, isCorrect = false, timedOut = true)
    }

    private fun recordAnswer(
        cell: BoardCell,
        answer: String?,
        isCorrect: Boolean,
        timedOut: Boolean,
    ): GameState {
        val player = currentPlayer
        val delta = if (isCorrect) cell.value else -cell.value
        val updatedPlayer = player.copy(
            score = player.score + delta,
            correct = player.correct + if (isCorrect) 1 else 0,
            wrong = player.wrong + if (isCorrect) 0 else 1,
        )
        val updatedCell = cell.copy(
            answered = true,
            answeredBy = player.id,
            answerText = answer,
            timedOut = timedOut,
            isCorrect = isCorrect,
        )
        val updatedCategories = categories.map { category ->
            category.copy(
                cells = category.cells.map { current ->
                    if (current.id == cell.id) updatedCell else current
                },
            )
        }
        val entry = AnsweredQuestion(
            cellId = cell.id,
            topic = cell.topic,
            value = cell.value,
            question = cell.question,
            playerId = player.id,
            playerName = player.name,
            playerColor = player.color,
            selectedAnswer = answer,
            correctAnswer = GameFactory.shortAnswer(cell.question.answer),
            isCorrect = isCorrect,
            timedOut = timedOut,
            explanation = cell.question.explanation
                .ifBlank { cell.question.answer },
            sourceUrl = cell.question.sourceUrl,
        )
        val updatedPlayers = players.toMutableList().also {
            it[currentPlayerIndex] = updatedPlayer
        }
        return copy(
            players = updatedPlayers,
            categories = updatedCategories,
            activeCell = updatedCell,
            history = history + entry,
        )
    }

    fun continueAfterAnswer(): GameState {
        if (activeCell == null) return this
        val nextPlayer = if (allAnswered) {
            currentPlayerIndex
        } else {
            (currentPlayerIndex + 1) % players.size.coerceAtLeast(1)
        }
        return copy(
            currentPlayerIndex = nextPlayer,
            activeCell = null,
            finished = allAnswered,
        )
    }

    fun finish(): GameState = copy(finished = true, activeCell = null)
}
