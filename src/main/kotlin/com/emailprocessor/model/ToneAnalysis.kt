package com.emailprocessor.model

/**
 * Model analizy tonu wiadomości email
 */
data class ToneAnalysis(
    val sentiment: Sentiment,
    val emotions: Map<Emotion, Float>,
    val urgency: Urgency,
    val formality: Formality,
    val topTopics: List<String>,
    val summaryText: String
) {
    companion object {
        fun fromJson(json: String): ToneAnalysis {
            // W pełnej implementacji użylibyśmy biblioteki JSON
            // dla uproszczonej wersji zwracamy przykładowe dane
            return ToneAnalysis(
                sentiment = Sentiment.NEUTRAL,
                emotions = mapOf(
                    Emotion.NEUTRAL to 0.8f,
                    Emotion.HAPPINESS to 0.2f
                ),
                urgency = Urgency.NORMAL,
                formality = Formality.NEUTRAL,
                topTopics = listOf("zapytanie", "informacja"),
                summaryText = "Wiadomość zawiera ogólne zapytanie o informacje."
            )
        }
    }
    
    fun toJson(): String {
        // biblioteka JSON
        return """{"sentiment":"$sentiment","urgency":"$urgency"}"""
    }
}

/**
 * Ogólny sentyment wiadomości
 */
enum class Sentiment {
    VERY_NEGATIVE,
    NEGATIVE,
    NEUTRAL,
    POSITIVE,
    VERY_POSITIVE
}

/**
 * Emocje wykryte w wiadomości
 */
enum class Emotion {
    ANGER,
    FEAR,
    HAPPINESS,
    SADNESS,
    SURPRISE,
    DISGUST,
    NEUTRAL
}

/**
 * Pilność wiadomości
 */
enum class Urgency {
    LOW,
    NORMAL,
    HIGH,
    CRITICAL
}

/**
 * Poziom formalności wiadomości
 */
enum class Formality {
    VERY_INFORMAL,
    INFORMAL,
    NEUTRAL,
    FORMAL,
    VERY_FORMAL
}