package ru.viktorinavopros.quiz

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.viktorinavopros.quiz.game.AnswerChecker

class AnswerCheckerTest {

    @Test
    fun `date formats are equivalent`() {
        val correct = "24 марта 1984 года"
        assertTrue(AnswerChecker.isCorrect("24.03.1984", correct))
        assertTrue(AnswerChecker.isCorrect("24.03.84", correct))
        assertTrue(AnswerChecker.isCorrect("24 Марта 84", correct))
        assertTrue(AnswerChecker.isCorrect("24 марта 1984", correct))
        assertTrue(AnswerChecker.isCorrect("1984-03-24", correct))
        assertTrue(AnswerChecker.isCorrect("24/03/1984", correct))
    }

    @Test
    fun `case and word order do not matter`() {
        assertTrue(
            AnswerChecker.isCorrect(
                "толстой лев николаевич",
                "Лев Николаевич Толстой",
            ),
        )
        assertTrue(
            AnswerChecker.isCorrect(
                "ПУШКИН АЛЕКСАНДР СЕРГЕЕВИЧ",
                "Александр Сергеевич Пушкин",
            ),
        )
    }

    @Test
    fun `small typos and letter swaps are accepted`() {
        assertTrue(AnswerChecker.isCorrect("Максва", "Москва"))
        assertTrue(AnswerChecker.isCorrect("Моксва", "Москва"))
        assertTrue(AnswerChecker.isCorrect("Толтсой", "Толстой"))
        assertTrue(AnswerChecker.isCorrect("Пушкн", "Пушкин"))
    }

    @Test
    fun `short answers are checked strictly`() {
        assertFalse(AnswerChecker.isCorrect("мир", "Рим"))
        assertFalse(AnswerChecker.isCorrect("ток", "кот"))
        assertFalse(AnswerChecker.isCorrect("луна", "рана"))
    }

    @Test
    fun `different dates and numbers are rejected`() {
        assertFalse(AnswerChecker.isCorrect("24.03.1985", "24 марта 1984 года"))
        assertFalse(AnswerChecker.isCorrect("25.03.1984", "24 марта 1984 года"))
        assertFalse(AnswerChecker.isCorrect("1985", "1984"))
    }

    @Test
    fun `incomplete answer is accepted`() {
        assertTrue(AnswerChecker.isCorrect("Пушкин", "Александр Сергеевич Пушкин"))
        assertTrue(AnswerChecker.isCorrect("Толстой", "Лев Николаевич Толстой"))
        assertTrue(AnswerChecker.isCorrect("24 марта", "24 марта 1984 года"))
        assertTrue(AnswerChecker.isCorrect("1984", "24 марта 1984 года"))
    }

    @Test
    fun `extra unrelated words are rejected`() {
        assertFalse(AnswerChecker.isCorrect("Москва Париж", "Москва"))
        assertFalse(AnswerChecker.isCorrect("Пушкин Лермонтов", "Пушкин"))
    }

    @Test
    fun `russian case endings are ignored`() {
        assertTrue(AnswerChecker.isCorrect("в Москве", "Москва"))
        assertTrue(AnswerChecker.isCorrect("Толстого", "Толстой"))
        assertTrue(AnswerChecker.isCorrect("Пушкина", "Пушкин"))
    }

    @Test
    fun `parentheses and alternatives from site are handled`() {
        assertTrue(AnswerChecker.isCorrect("Кофе", "Кофе (гадать на кофейной гуще)."))
        assertTrue(AnswerChecker.isCorrect("Австралия", "Австралия или Океания"))
        assertTrue(AnswerChecker.isCorrect("Океания", "Австралия или Океания"))
    }

    @Test
    fun `explanation words do not make a wrong answer correct`() {
        assertFalse(
            AnswerChecker.isCorrect(
                "Париж",
                "Москва. Париж в этом случае не подходит.",
            ),
        )
    }
}
