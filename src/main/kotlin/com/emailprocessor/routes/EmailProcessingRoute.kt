package com.emailprocessor.routes

import com.emailprocessor.model.EmailMessage
import com.emailprocessor.model.EmailStatus
import com.emailprocessor.services.EmailService
import org.apache.camel.Exchange
import org.apache.camel.builder.RouteBuilder
import org.apache.camel.component.jackson.JacksonDataFormat
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.Timestamp
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.mail.internet.MimeMessage

/**
 * Trasa do przetwarzania wiadomości email odbieranych przez IMAP
 */
class EmailProcessingRoute(
    private val emailHost: String,
    private val emailPort: Int,
    private val emailUser: String,
    private val emailPassword: String,
    private val emailService: EmailService,
    private val dbPath: String
) : RouteBuilder() {

    override fun configure() {
        // Obsługa błędów
        errorHandler(defaultErrorHandler()
            .logExhaustedMessageHistory(true)
            .maximumRedeliveries(3)
            .redeliveryDelay(1000)
            .backOffMultiplier(2.0)
            .useExponentialBackOff()
        )
        
        // Trasa do odbierania wiadomości email przez IMAP
        from("imaps://$emailHost:$emailPort?" +
             "username=$emailUser&" +
             "password=$emailPassword&" +
             "delete=false&" +
             "unseen=true&" +
             "delay=60000")
        .routeId("email-receiver")
        .log("Odebrano nową wiadomość email: \${header.subject}")
        .process { exchange ->
            val message = exchange.getIn().getBody(MimeMessage::class.java)
            val emailMessage = parseEmail(message)
            exchange.getIn().body = emailMessage
        }
        .to("direct:process-email")
        
        // Trasa do przetwarzania wiadomości email
        from("direct:process-email")
        .routeId("email-processor")
        .log("Przetwarzanie wiadomości: \${body.subject}")
        .process { exchange ->
            val email = exchange.getIn().getBody(EmailMessage::class.java)
            // Aktualizacja statusu w bazie danych
            updateEmailStatus(email.id!!, EmailStatus.PROCESSING)
            
            // Przetwarzanie wiadomości przez serwis
            val processedEmail = emailService.processEmail(email)
            
            // Zapisanie wyników analizy w bazie danych
            saveAnalysisResults(processedEmail)
            
            exchange.getIn().body = processedEmail
        }
        .choice()
            .when { exchange ->
                val email = exchange.getIn().getBody(EmailMessage::class.java)
                // Decyzja o automatycznej odpowiedzi na podstawie analizy
                emailService.shouldAutoReply(email)
            }
            .to("direct:send-auto-reply")
            .otherwise()
            .log("Brak automatycznej odpowiedzi dla wiadomości: \${body.subject}")
        .end()
    }
    
    private fun parseEmail(mimeMessage: MimeMessage): EmailMessage {
        // Ekstrakcja danych z wiadomości email
        val from = mimeMessage.from[0].toString()
        val to = mimeMessage.allRecipients.joinToString(", ") { it.toString() }
        val subject = mimeMessage.subject ?: ""
        
        // Pobranie treści wiadomości
        val content = if (mimeMessage.isMimeType("text/plain")) {
            mimeMessage.content.toString()
        } else if (mimeMessage.isMimeType("multipart/*")) {
            val multipart = mimeMessage.content as javax.mail.Multipart
            val result = StringBuilder()
            
            for (i in 0 until multipart.count) {
                val bodyPart = multipart.getBodyPart(i)
                if (bodyPart.isMimeType("text/plain")) {
                    result.append(bodyPart.content.toString())
                }
            }
            
            result.toString()
        } else {
            "Nieobsługiwany format wiadomości"
        }
        
        // Zapisanie wiadomości w bazie danych i pobranie ID
        val id = saveEmailToDatabase(from, to, subject, content)
        
        return EmailMessage(
            id = id,
            from = from,
            to = to,
            subject = subject,
            content = content,
            receivedDate = LocalDateTime.now(),
            status = EmailStatus.RECEIVED
        )
    }
    
    private fun saveEmailToDatabase(
        from: String,
        to: String,
        subject: String,
        content: String
    ): Long {
        var id: Long = -1
        
        DriverManager.getConnection("jdbc:sqlite:$dbPath").use { conn ->
            conn.prepareStatement(
                """
                INSERT INTO emails (from_address, to_address, subject, content, received_date, status)
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                PreparedStatement.RETURN_GENERATED_KEYS
            ).use { stmt ->
                stmt.setString(1, from)
                stmt.setString(2, to)
                stmt.setString(3, subject)
                stmt.setString(4, content)
                stmt.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()))
                stmt.setString(6, EmailStatus.RECEIVED.name)
                
                stmt.executeUpdate()
                
                val rs = stmt.generatedKeys
                if (rs.next()) {
                    id = rs.getLong(1)
                }
            }
        }
        
        return id
    }
    
    private fun updateEmailStatus(id: Long, status: EmailStatus) {
        DriverManager.getConnection("jdbc:sqlite:$dbPath").use { conn ->
            conn.prepareStatement(
                "UPDATE emails SET status = ? WHERE id = ?"
            ).use { stmt ->
                stmt.setString(1, status.name)
                stmt.setLong(2, id)
                stmt.executeUpdate()
            }
        }
    }
    
    private fun saveAnalysisResults(email: EmailMessage) {
        DriverManager.getConnection("jdbc:sqlite:$dbPath").use { conn ->
            conn.prepareStatement(
                """
                UPDATE emails 
                SET tone_analysis = ?, processed_date = ?, status = ?
                WHERE id = ?
                """
            ).use { stmt ->
                stmt.setString(1, email.toneAnalysis?.toJson() ?: "")
                stmt.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()))
                stmt.setString(3, EmailStatus.PROCESSED.name)
                stmt.setLong(4, email.id!!)
                stmt.executeUpdate()
            }
        }
    }
}