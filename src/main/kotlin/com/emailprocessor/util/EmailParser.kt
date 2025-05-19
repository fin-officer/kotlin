package com.emailprocessor.util

import javax.mail.Message
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart
import javax.mail.internet.MimeBodyPart
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

/**
 * Narzędzie do parsowania wiadomości email
 */
object EmailParser {
    
    /**
     * Ekstrahuje treść z wiadomości email
     */
    fun extractContent(message: Message): String {
        return when {
            message.isMimeType("text/plain") -> {
                message.content.toString()
            }
            message.isMimeType("text/html") -> {
                extractTextFromHtml(message.content.toString())
            }
            message.isMimeType("multipart/*") -> {
                extractTextFromMultipart(message.content as MimeMultipart)
            }
            else -> {
                "Nieobsługiwany format wiadomości"
            }
        }
    }
    
    /**
     * Ekstrahuje tekst z zawartości HTML
     */
    private fun extractTextFromHtml(html: String): String {
        // Prosta implementacja - w produkcyjnej wersji użylibyśmy biblioteki jak jsoup
        return html.replace(Regex("<[^>]*>"), "")
    }
    
    /**
     * Ekstrahuje tekst z wieloczęściowej wiadomości
     */
    private fun extractTextFromMultipart(multipart: MimeMultipart): String {
        val result = StringBuilder()
        
        for (i in 0 until multipart.count) {
            val bodyPart = multipart.getBodyPart(i)
            
            when {
                bodyPart.isMimeType("text/plain") -> {
                    result.append(bodyPart.content.toString())
                }
                bodyPart.isMimeType("text/html") -> {
                    result.append(extractTextFromHtml(bodyPart.content.toString()))
                }
                bodyPart.isMimeType("multipart/*") -> {
                    result.append(extractTextFromMultipart(bodyPart.content as MimeMultipart))
                }
            }
        }
        
        return result.toString()
    }
    
    /**
     * Pobiera załączniki z wiadomości email
     */
    fun extractAttachments(message: MimeMessage): List<EmailAttachment> {
        val attachments = mutableListOf<EmailAttachment>()
        
        if (message.isMimeType("multipart/*")) {
            val multipart = message.content as MimeMultipart
            for (i in 0 until multipart.count) {
                val bodyPart = multipart.getBodyPart(i)
                
                if (Part.ATTACHMENT.equals(bodyPart.disposition, ignoreCase = true) ||
                    bodyPart.fileName != null) {
                    
                    val fileName = bodyPart.fileName ?: "attachment-$i"
                    val contentType = bodyPart.contentType ?: "application/octet-stream"
                    val data = bodyPart.inputStream.readBytes()
                    
                    attachments.add(EmailAttachment(fileName, contentType, data))
                }
            }
        }
        
        return attachments
    }
    
    /**
     * Klasa reprezentująca załącznik email
     */
    data class EmailAttachment(
        val fileName: String,
        val contentType: String,
        val data: ByteArray
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            
            other as EmailAttachment
            
            if (fileName != other.fileName) return false
            if (contentType != other.contentType) return false
            if (!data.contentEquals(other.data)) return false
            
            return true
        }
        
        override fun hashCode(): Int {
            var result = fileName.hashCode()
            result = 31 * result + contentType.hashCode()
            result = 31 * result + data.contentHashCode()
            return result
        }
    }
}