package org.santayn.testing.mobile.domain

import org.santayn.testing.mobile.core.network.AppJson
import org.santayn.testing.mobile.data.model.LectureRequest
import org.santayn.testing.mobile.data.model.ResultItemDto
import org.santayn.testing.mobile.data.model.SubmitAttemptResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class MarkdownAndAiTests {

    @Test
    fun parsesLectureMarkdownBlocks() {
        val blocks = parseMarkdown(
            """
            ## Оперативная память
            Хранит данные
            программ.

            - быстрая
            - энергозависимая

            1. чтение
            2) выполнение

            > Определение

            | Память | Скорость |
            |---|---|
            | ОЗУ | высокая |
            """.trimIndent(),
        )

        assertEquals(6, blocks.size)
        assertEquals(2, assertIs<MdBlock.Heading>(blocks[0]).level)
        assertEquals("Хранит данные программ.", assertIs<MdBlock.Paragraph>(blocks[1]).inlines.plainText())
        assertFalse(assertIs<MdBlock.ListBlock>(blocks[2]).ordered)
        assertTrue(assertIs<MdBlock.ListBlock>(blocks[3]).ordered)
        assertIs<MdBlock.Quote>(blocks[4])
        assertEquals("ОЗУ", assertIs<MdBlock.Table>(blocks[5]).rows[0][0].plainText())
    }

    @Test
    fun keepsOnlyWebLinksAndStripsMarkupForSpeech() {
        val inlines = parseInline("**важно** [сайт](https://example.org) [клик](javascript:alert(1))")
        assertIs<MdInline.Strong>(inlines[0])
        assertIs<MdInline.Link>(inlines[2])
        assertIs<MdInline.Text>(inlines[4])
        assertEquals("Тема\n\nтекст и код", markdownPlainText("# Тема\nтекст и `код`"))
    }

    @Test
    fun readsReviewStateAndPendingAnswers() {
        val item = AppJson.decodeFromString(
            ResultItemDto.serializer(),
            """{"questionText":"Q","reviewStatus":"needs_teacher","aiFeedback":"Не хватает термина"}""",
        )
        assertTrue(item.reviewPending)
        val submit = AppJson.decodeFromString(
            SubmitAttemptResponse.serializer(),
            """{"attemptId":1,"pendingReviewCount":2,"details":[{"pendingReview":true}]}""",
        )
        assertEquals(2, submit.pendingReviewCount)
        assertTrue(submit.details.single().pendingReview)
    }

    @Test
    fun lectureRequestOmitsBodyWhenUnchanged() {
        val request = LectureRequest(1, 2, null, 1, "Лекция", null, "key", null, true)
        assertFalse(AppJson.encodeToString(LectureRequest.serializer(), request).contains("body"))
        assertTrue(AppJson.encodeToString(LectureRequest.serializer(), request.copy(body = "")).contains("\"body\":\"\""))
    }
}
