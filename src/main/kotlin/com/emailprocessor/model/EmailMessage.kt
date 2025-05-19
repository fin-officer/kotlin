package com.emailprocessor.model

import java.time.LocalDateTime

/**
 * Model wiadomości email
 */
data class EmailMessage(
    val id: Long? = null,
    val from: String,
    val to: String,
    val subject: String? = null,
    val content: String? = null,
    val receivedDate: LocalDateTime = LocalDateTime.now(),
    val processedDate: LocalDateTime? = null,
    val toneAnalysis: ToneAnalysis? = null,
    val status: EmailStatus = EmailStatus.RECEIVED
)

/**
 * Status przetwarzania wiadomości email
 */
enum class EmailStatus {
    RECEIVED,      // Wiadomość została otrzymana
    PROCESSING,    // Wiadomość jest przetwarzana
    PROCESSED,     // Wiadomość została przetworzona
    REPLIED,       // Wysłano odpowiedź na wiadomość
    ERROR          // Wystąpił błąd podczas przetwarzania
}