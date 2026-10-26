package org.santayn.testing.mobile.core

import org.santayn.testing.mobile.core.platform.splitForSpeech
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SpeechSplitTests {

    @Test
    fun shortTextStaysWhole() {
        assertEquals(listOf("Индексы ускоряют поиск."), splitForSpeech("  Индексы ускоряют поиск. ", 4000))
    }

    @Test
    fun longTextIsCutAtSentencesWithinLimit() {
        val sentence = "Индексы помогают СУБД быстро находить строки без полного просмотра таблицы. "
        val text = sentence.repeat(120)
        val parts = splitForSpeech(text, 3900)

        assertTrue(parts.size >= 3)
        assertTrue(parts.all { it.length <= 3900 })
        assertTrue(parts.dropLast(1).all { it.endsWith(".") })
        assertEquals(text.replace(" ", ""), parts.joinToString("").replace(" ", ""))
    }

    @Test
    fun paragraphBreakIsPreferredAndUnbrokenTextIsCutHard() {
        assertEquals(listOf("Первый абзац", "второй абзац"), splitForSpeech("Первый абзац\nвторой абзац", 20))
        assertEquals(listOf("aaaaa", "aaaaa", "aa"), splitForSpeech("a".repeat(12), 5))
    }
}
