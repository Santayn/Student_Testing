package org.santayn.testing.mobile.core

import io.ktor.utils.io.ByteChannel
import io.ktor.utils.io.readRemaining
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.io.readByteArray
import org.santayn.testing.mobile.core.network.UploadFile
import org.santayn.testing.mobile.core.network.multipartBody
import kotlin.test.Test
import kotlin.test.assertTrue

class MultipartTests {
    /** Бэкенд (Spring/Tomcat) читает имя файла в multipart как UTF-8 — кириллица должна уйти без искажений. */
    @Test
    fun cyrillicFileNameIsSentAsUtf8() = runTest {
        val body = multipartBody("files", listOf(UploadFile("Лекция 1.pdf", byteArrayOf(1, 2, 3), "application/pdf")), emptyMap())
        val channel = ByteChannel()
        launch {
            body.writeTo(channel)
            channel.flushAndClose()
        }
        val bytes = channel.readRemaining().readByteArray()
        val text = bytes.decodeToString()
        assertTrue("filename=\"Лекция 1.pdf\"" in text, text.take(300))
    }
}
