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
    private val llmService: LlmService
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
     */
    fun generateReply(email: EmailMessage): String {
        val analysis = email.toneAnalysis ?: return DEFAULT_REPLY
        
        // Dla bardziej zaawansowanej implementacji można użyć LLM do generowania odpowiedzi
        return when {
            analysis.urgency == Urgency.CRITICAL -> URGENT_REPLY
            analysis.sentiment == Sentiment.VERY_NEGATIVE -> NEGATIVE_SENTIMENT_REPLY
            else -> DEFAULT_REPLY
        }
    }
    
    companion object {
        private const val DEFAULT_REPLY = "Dziękujemy za wiadomość. Zajmiemy się nią wkrótce."
        private const val URGENT_REPLY = "Dziękujemy za wiadomość. Zauważyliśmy, że sprawa jest pilna. Zajmiemy się nią priorytetowo."
        private const val NEGATIVE_SENTIMENT_REPLY = "Dziękujemy za wiadomość. Przepraszamy za niedogodności. Postaramy się rozwiązać problem jak najszybciej."
    }
}