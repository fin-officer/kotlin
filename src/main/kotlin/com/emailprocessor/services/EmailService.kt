package com.emailprocessor.services

import com.emailprocessor.model.EmailMessage
import com.emailprocessor.model.EmailStatus
import com.emailprocessor.model.Sentiment
import com.emailprocessor.model.ToneAnalysis
import com.emailprocessor.model.Urgency
import java.time.LocalDateTime

/**
 * Serwis do przetwarzania wiadomości email
 */
class EmailService(
    private val llmService: LlmService,
    private val advancedReplyService: AdvancedReplyService
) {
    /**
     * Przetwarza wiadomość email, wykonując analizę tonu
     */
    fun processEmail(email: EmailMessage): EmailMessage {
        // Analiza tonu wiadomości przez LLM
        val content = email.content ?: ""
        val toneAnalysis = llmService.analyzeTone(content)
        
        // Tworzenie przetworzonej wiadomości
        return email.copy(
            toneAnalysis = toneAnalysis,
            processedDate = LocalDateTime.now(),
            status = EmailStatus.PROCESSED
        )
    }
    
    /**
     * Decyduje, czy należy wysłać automatyczną odpowiedź na podstawie analizy
     */
    fun shouldAutoReply(email: EmailMessage): Boolean {
        val analysis = email.toneAnalysis ?: return false
        
        // Przykładowa logika decyzji o automatycznej odpowiedzi
        // W rzeczywistej implementacji możemy mieć bardziej złożoną logikę
        return when {
            analysis.urgency == Urgency.HIGH || analysis.urgency == Urgency.CRITICAL -> true
            analysis.sentiment == Sentiment.NEGATIVE || analysis.sentiment == Sentiment.VERY_NEGATIVE -> true
            else -> false
        }
    }
    
    /**
     * Generuje tekst odpowiedzi na podstawie analizy wiadomości
     * wykorzystując zaawansowany serwis odpowiedzi
     */
    fun generateReply(email: EmailMessage): String {
        return advancedReplyService.generateReply(email)
    }
    
    /**
     * Zapisuje otrzymaną wiadomość do pliku dla celów archiwalnych
     */
    fun archiveEmail(email: EmailMessage) {
        try {
            val archiveDir = java.io.File("data/archive")
            if (!archiveDir.exists()) {
                archiveDir.mkdirs()
            }
            
            val timestamp = java.time.format.DateTimeFormatter
                .ofPattern("yyyyMMdd_HHmmss")
                .format(LocalDateTime.now())
            
            val sanitizedFrom = email.from.replace("[^a-zA-Z0-9]", "_")
            val fileName = "${timestamp}_${sanitizedFrom}.txt"
            
            val archiveFile = java.io.File(archiveDir, fileName)
            archiveFile.writeText("" + 
                "From: ${email.from}\n" +
                "To: ${email.to}\n" +
                "Subject: ${email.subject}\n" +
                "Received: ${email.receivedDate}\n" +
                "Status: ${email.status}\n" +
                "\n" +
                "${email.content}\n"
            )
            
            println("Zarchiwizowano wiadomość do pliku: ${archiveFile.absolutePath}")
        } catch (e: Exception) {
            println("Błąd podczas archiwizacji wiadomości: ${e.message}")
            e.printStackTrace()
        }
    }
}