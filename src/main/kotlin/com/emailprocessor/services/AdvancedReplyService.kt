package com.emailprocessor.services

import com.emailprocessor.model.EmailMessage
import com.emailprocessor.model.Sentiment
import com.emailprocessor.model.ToneAnalysis
import com.emailprocessor.model.Urgency
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap

/**
 * Serwis do generowania zaawansowanych automatycznych odpowiedzi
 * wykorzystujący dane z bazy SQLite oraz plików szablonów
 */
class AdvancedReplyService(private val dbPath: String) {
    
    // Cache dla szablonów odpowiedzi
    private val templateCache = ConcurrentHashMap<String, String>()
    
    // Ścieżka do katalogu z szablonami
    private val templatesDir = "data/templates"
    
    init {
        // Upewnij się, że katalog z szablonami istnieje
        val templatesDirFile = File(templatesDir)
        if (!templatesDirFile.exists()) {
            templatesDirFile.mkdirs()
            createDefaultTemplates()
        }
        
        // Załaduj szablony do cache'u
        loadTemplates()
    }
    
    /**
     * Generuje odpowiedź na podstawie analizy wiadomości, historii komunikacji
     * i dostępnych szablonów
     */
    fun generateReply(email: EmailMessage): String {
        // Pobranie historii komunikacji z nadawcą
        val communicationHistory = getCommunicationHistory(email.from)
        
        // Wybór odpowiedniego szablonu na podstawie analizy tonu i historii
        val templateKey = selectTemplateKey(email, communicationHistory)
        
        // Pobranie szablonu
        val template = getTemplate(templateKey)
        
        // Wypełnienie szablonu danymi
        return fillTemplate(template, email, communicationHistory)
    }
    
    /**
     * Pobiera historię komunikacji z danym nadawcą
     */
    private fun getCommunicationHistory(emailAddress: String): List<EmailHistoryEntry> {
        val history = mutableListOf<EmailHistoryEntry>()
        
        try {
            DriverManager.getConnection("jdbc:sqlite:$dbPath").use { conn ->
                conn.prepareStatement(
                    """
                    SELECT id, subject, received_date, processed_date, tone_analysis, status 
                    FROM emails 
                    WHERE from_address = ? 
                    ORDER BY received_date DESC 
                    LIMIT 10
                    """
                ).use { stmt ->
                    stmt.setString(1, emailAddress)
                    val rs = stmt.executeQuery()
                    
                    while (rs.next()) {
                        history.add(EmailHistoryEntry(
                            id = rs.getLong("id"),
                            subject = rs.getString("subject"),
                            receivedDate = rs.getString("received_date"),
                            processedDate = rs.getString("processed_date"),
                            toneAnalysis = rs.getString("tone_analysis"),
                            status = rs.getString("status")
                        ))
                    }
                }
            }
        } catch (e: Exception) {
            println("Błąd podczas pobierania historii komunikacji: ${e.message}")
            e.printStackTrace()
        }
        
        return history
    }
    
    /**
     * Wybiera klucz szablonu na podstawie analizy wiadomości i historii komunikacji
     */
    private fun selectTemplateKey(email: EmailMessage, history: List<EmailHistoryEntry>): String {
        val analysis = email.toneAnalysis ?: return "default"
        
        // Sprawdzenie, czy to pierwszy email od tego nadawcy
        val isFirstEmail = history.isEmpty()
        
        // Sprawdzenie, czy nadawca często wysyła wiadomości
        val isFrequentSender = history.size >= 3
        
        // Sprawdzenie, czy poprzednie wiadomości miały negatywny ton
        val hasPreviousNegativeTone = history.any { 
            it.toneAnalysis?.contains("\"sentiment\":\"NEGATIVE\"") == true ||
            it.toneAnalysis?.contains("\"sentiment\":\"VERY_NEGATIVE\"") == true
        }
        
        return when {
            // Pilne wiadomości
            analysis.urgency == Urgency.CRITICAL -> "urgent_critical"
            analysis.urgency == Urgency.HIGH -> "urgent_high"
            
            // Wiadomości o negatywnym tonie
            analysis.sentiment == Sentiment.VERY_NEGATIVE && hasPreviousNegativeTone -> "negative_repeated"
            analysis.sentiment == Sentiment.VERY_NEGATIVE -> "negative_very"
            analysis.sentiment == Sentiment.NEGATIVE -> "negative"
            
            // Wiadomości o pozytywnym tonie
            analysis.sentiment == Sentiment.VERY_POSITIVE -> "positive_very"
            analysis.sentiment == Sentiment.POSITIVE -> "positive"
            
            // Pierwszy email od nadawcy
            isFirstEmail -> "first_contact"
            
            // Częsty nadawca
            isFrequentSender -> "frequent_sender"
            
            // Domyślny szablon
            else -> "default"
        }
    }
    
    /**
     * Pobiera szablon z cache'u lub z pliku
     */
    private fun getTemplate(templateKey: String): String {
        return templateCache.computeIfAbsent(templateKey) { key ->
            try {
                val templatePath = Paths.get(templatesDir, "$key.template")
                if (Files.exists(templatePath)) {
                    Files.readString(templatePath)
                } else {
                    getTemplate("default") // Fallback do domyślnego szablonu
                }
            } catch (e: Exception) {
                println("Błąd podczas wczytywania szablonu $key: ${e.message}")
                DEFAULT_TEMPLATE
            }
        }
    }
    
    /**
     * Wypełnia szablon danymi
     */
    private fun fillTemplate(template: String, email: EmailMessage, history: List<EmailHistoryEntry>): String {
        var result = template
        
        // Podstawowe dane
        result = result.replace("{{SENDER_NAME}}", extractName(email.from))
        result = result.replace("{{SUBJECT}}", email.subject ?: "")
        result = result.replace("{{CURRENT_DATE}}", LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
        
        // Dane z analizy tonu
        email.toneAnalysis?.let { analysis ->
            result = result.replace("{{SENTIMENT}}", translateSentiment(analysis.sentiment))
            result = result.replace("{{URGENCY}}", translateUrgency(analysis.urgency))
            result = result.replace("{{SUMMARY}}", analysis.summaryText)
        }
        
        // Dane z historii komunikacji
        val emailCount = history.size
        result = result.replace("{{EMAIL_COUNT}}", emailCount.toString())
        
        if (emailCount > 0) {
            val lastEmailDate = history.firstOrNull()?.receivedDate ?: ""
            result = result.replace("{{LAST_EMAIL_DATE}}", lastEmailDate)
        } else {
            result = result.replace("{{LAST_EMAIL_DATE}}", "")
        }
        
        return result
    }
    
    /**
     * Ekstrahuje imię z adresu email
     */
    private fun extractName(emailAddress: String): String {
        // Próba wyodrębnienia imienia z adresu email
        val namePart = if (emailAddress.contains("<")) {
            emailAddress.substringBefore("<").trim()
        } else {
            emailAddress.substringBefore("@").replace(".", " ").capitalize()
        }
        
        return if (namePart.isNotBlank()) namePart else "Szanowny Kliencie"
    }
    
    /**
     * Tłumaczy sentyment na język polski
     */
    private fun translateSentiment(sentiment: Sentiment): String {
        return when (sentiment) {
            Sentiment.VERY_NEGATIVE -> "bardzo negatywna"
            Sentiment.NEGATIVE -> "negatywna"
            Sentiment.NEUTRAL -> "neutralna"
            Sentiment.POSITIVE -> "pozytywna"
            Sentiment.VERY_POSITIVE -> "bardzo pozytywna"
        }
    }
    
    /**
     * Tłumaczy pilność na język polski
     */
    private fun translateUrgency(urgency: Urgency): String {
        return when (urgency) {
            Urgency.LOW -> "niska"
            Urgency.NORMAL -> "normalna"
            Urgency.HIGH -> "wysoka"
            Urgency.CRITICAL -> "krytyczna"
        }
    }
    
    /**
     * Ładuje wszystkie szablony do cache'u
     */
    private fun loadTemplates() {
        try {
            val templatesFolder = File(templatesDir)
            if (templatesFolder.exists() && templatesFolder.isDirectory) {
                templatesFolder.listFiles { file -> file.name.endsWith(".template") }?.forEach { file ->
                    val templateKey = file.nameWithoutExtension
                    templateCache[templateKey] = file.readText()
                }
            }
        } catch (e: Exception) {
            println("Błąd podczas ładowania szablonów: ${e.message}")
        }
    }
    
    /**
     * Tworzy domyślne szablony, jeśli nie istnieją
     */
    private fun createDefaultTemplates() {
        val templates = mapOf(
            "default" to DEFAULT_TEMPLATE,
            "urgent_critical" to URGENT_CRITICAL_TEMPLATE,
            "urgent_high" to URGENT_HIGH_TEMPLATE,
            "negative_very" to NEGATIVE_VERY_TEMPLATE,
            "negative" to NEGATIVE_TEMPLATE,
            "negative_repeated" to NEGATIVE_REPEATED_TEMPLATE,
            "positive_very" to POSITIVE_VERY_TEMPLATE,
            "positive" to POSITIVE_TEMPLATE,
            "first_contact" to FIRST_CONTACT_TEMPLATE,
            "frequent_sender" to FREQUENT_SENDER_TEMPLATE
        )
        
        templates.forEach { (key, content) ->
            try {
                val templateFile = File("$templatesDir/$key.template")
                templateFile.writeText(content)
            } catch (e: Exception) {
                println("Błąd podczas tworzenia szablonu $key: ${e.message}")
            }
        }
    }
    
    /**
     * Klasa reprezentująca wpis w historii komunikacji
     */
    data class EmailHistoryEntry(
        val id: Long,
        val subject: String?,
        val receivedDate: String?,
        val processedDate: String?,
        val toneAnalysis: String?,
        val status: String?
    )
    
    companion object {
        private const val DEFAULT_TEMPLATE = """
            Szanowny/a {{SENDER_NAME}},
            
            Dziękujemy za wiadomość dotyczącą: "{{SUBJECT}}".
            
            Otrzymaliśmy Twoją wiadomość i zajmiemy się nią wkrótce.
            
            Z poważaniem,
            Zespół Obsługi Klienta
        """.trimIndent()
        
        private const val URGENT_CRITICAL_TEMPLATE = """
            Szanowny/a {{SENDER_NAME}},
            
            Dziękujemy za wiadomość dotyczącą: "{{SUBJECT}}".
            
            Zauważyliśmy, że Twoja sprawa jest krytycznie pilna. Przekazaliśmy ją do natychmiastowego rozpatrzenia przez nasz zespół.
            
            Skontaktujemy się z Tobą najszybciej jak to możliwe.
            
            Z poważaniem,
            Zespół Obsługi Klienta
        """.trimIndent()
        
        private const val URGENT_HIGH_TEMPLATE = """
            Szanowny/a {{SENDER_NAME}},
            
            Dziękujemy za wiadomość dotyczącą: "{{SUBJECT}}".
            
            Rozumiemy, że Twoja sprawa jest pilna. Zajmiemy się nią priorytetowo.
            
            Z poważaniem,
            Zespół Obsługi Klienta
        """.trimIndent()
        
        private const val NEGATIVE_VERY_TEMPLATE = """
            Szanowny/a {{SENDER_NAME}},
            
            Dziękujemy za wiadomość dotyczącą: "{{SUBJECT}}".
            
            Bardzo przepraszamy za niedogodności, które napotkałeś/aś. Traktujemy Twoją sprawę bardzo poważnie i zajmiemy się nią najszybciej jak to możliwe.
            
            Prosimy o wyrozumiałość. Zrobimy wszystko, aby rozwiązać ten problem.
            
            Z poważaniem,
            Zespół Obsługi Klienta
        """.trimIndent()
        
        private const val NEGATIVE_TEMPLATE = """
            Szanowny/a {{SENDER_NAME}},
            
            Dziękujemy za wiadomość dotyczącą: "{{SUBJECT}}".
            
            Przepraszamy za niedogodności. Postaramy się rozwiązać ten problem jak najszybciej.
            
            Z poważaniem,
            Zespół Obsługi Klienta
        """.trimIndent()
        
        private const val NEGATIVE_REPEATED_TEMPLATE = """
            Szanowny/a {{SENDER_NAME}},
            
            Dziękujemy za ponowną wiadomość dotyczącą: "{{SUBJECT}}".
            
            Widzimy, że to nie pierwszy raz, gdy napotykasz problemy. Bardzo przepraszamy za tę sytuację. Twoja sprawa została przekazana do kierownika zespołu, który osobiście zajmie się jej rozwiązaniem.
            
            Skontaktujemy się z Tobą najszybciej jak to możliwe.
            
            Z poważaniem,
            Zespół Obsługi Klienta
        """.trimIndent()
        
        private const val POSITIVE_VERY_TEMPLATE = """
            Szanowny/a {{SENDER_NAME}},
            
            Dziękujemy za Twoją pozytywną wiadomość dotyczącą: "{{SUBJECT}}".
            
            Bardzo cieszymy się, że jesteś zadowolony/a z naszych usług. Twoja opinia jest dla nas niezwykle ważna.
            
            Jeśli będziesz mieć jakiekolwiek pytania, zawsze jesteśmy do Twojej dyspozycji.
            
            Z poważaniem,
            Zespół Obsługi Klienta
        """.trimIndent()
        
        private const val POSITIVE_TEMPLATE = """
            Szanowny/a {{SENDER_NAME}},
            
            Dziękujemy za Twoją wiadomość dotyczącą: "{{SUBJECT}}".
            
            Cieszymy się, że możemy Ci pomóc. Jeśli będziesz mieć jakiekolwiek pytania, zawsze jesteśmy do Twojej dyspozycji.
            
            Z poważaniem,
            Zespół Obsługi Klienta
        """.trimIndent()
        
        private const val FIRST_CONTACT_TEMPLATE = """
            Szanowny/a {{SENDER_NAME}},
            
            Dziękujemy za Twoją pierwszą wiadomość dotyczącą: "{{SUBJECT}}".
            
            Witamy w gronie naszych klientów! Cieszymy się, że zdecydowałeś/aś się skontaktować z nami.
            
            Zajmiemy się Twoją sprawą najszybciej jak to możliwe.
            
            Z poważaniem,
            Zespół Obsługi Klienta
        """.trimIndent()
        
        private const val FREQUENT_SENDER_TEMPLATE = """
            Szanowny/a {{SENDER_NAME}},
            
            Dziękujemy za Twoją wiadomość dotyczącą: "{{SUBJECT}}".
            
            Doceniamy Twoją lojalność i częsty kontakt z nami. Jako nasz stały klient, Twoja sprawa zostanie rozpatrzona priorytetowo.
            
            To już Twoja {{EMAIL_COUNT}}. wiadomość do nas. Ostatnio kontaktowałeś/aś się z nami {{LAST_EMAIL_DATE}}.
            
            Z poważaniem,
            Zespół Obsługi Klienta
        """.trimIndent()
    }
}
