package com.emailprocessor.routes

import org.apache.camel.builder.RouteBuilder

/**
 * Trasa do wysyłania wiadomości email przez SMTP
 */
class EmailSendingRoute(
    private val emailHost: String,
    private val emailPort: Int,
    private val emailUser: String,
    private val emailPassword: String
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
        
        // Trasa do wysyłania automatycznych odpowiedzi
        from("direct:send-auto-reply")
        .routeId("email-auto-responder")
        .log("Przygotowanie automatycznej odpowiedzi dla: \${body.from}")
        .process { exchange ->
            val email = exchange.getIn().getBody(com.emailprocessor.model.EmailMessage::class.java)
            
            // Tworzenie odpowiedzi
            val replyBody = """
            Witaj,
            
            Dziękujemy za wiadomość. To jest automatyczna odpowiedź.
            
            Otrzymaliśmy Twoją wiadomość o temacie: "${email.subject}"
            
            ${if (email.toneAnalysis != null) "Zauważyliśmy, że Twoja wiadomość wydaje się ${translateSentiment(email.toneAnalysis.sentiment)}. " else ""}
            
            Zajmiemy się nią tak szybko, jak to możliwe.
            
            Pozdrawiamy,
            System Email LLM Processor
            """.trimIndent()
            
            // Przygotowanie danych dla komponentu SMTP
            exchange.getIn().setHeader("subject", "Re: ${email.subject}")
            exchange.getIn().setHeader("to", email.from)
            exchange.getIn().setHeader("from", emailUser)
            exchange.getIn().body = replyBody
        }
        .to("smtp://$emailHost:$emailPort?" +
            "username=$emailUser&" +
            "password=$emailPassword&" +
            "mail.smtp.auth=false&" +  // Dla MailHog, w produkcji ustaw na true
            "mail.smtp.starttls.enable=false")  // Dla MailHog, w produkcji ustaw na true
        .log("Wysłano automatyczną odpowiedź do: \${header.to}")
    }
    
    private fun translateSentiment(sentiment: com.emailprocessor.model.Sentiment): String {
        return when (sentiment) {
            com.emailprocessor.model.Sentiment.VERY_NEGATIVE -> "bardzo negatywna"
            com.emailprocessor.model.Sentiment.NEGATIVE -> "negatywna"
            com.emailprocessor.model.Sentiment.NEUTRAL -> "neutralna"
            com.emailprocessor.model.Sentiment.POSITIVE -> "pozytywna"
            com.emailprocessor.model.Sentiment.VERY_POSITIVE -> "bardzo pozytywna"
        }
    }
}