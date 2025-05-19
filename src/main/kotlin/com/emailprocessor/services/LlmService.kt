package com.emailprocessor.services

import com.emailprocessor.model.Emotion
import com.emailprocessor.model.Formality
import com.emailprocessor.model.Sentiment
import com.emailprocessor.model.ToneAnalysis
import com.emailprocessor.model.Urgency
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue

/**
 * Serwis do komunikacji z API modelu językowego (LLM)
 */
class LlmService(
    private val apiUrl: String,
    private val model: String
) {
    private val client = OkHttpClient()
    private val mapper = jacksonObjectMapper()
    
    /**
     * Analizuje ton wiadomości email
     */
    fun analyzeTone(content: String): ToneAnalysis {
        if (content.isBlank()) {
            return createDefaultAnalysis()
        }
        
        try {
            // Tworzenie promptu dla modelu LLM
            val prompt = createAnalysisPrompt(content)
            
            // Wysłanie zapytania do API modelu
            val response = callLlmApi(prompt)
            
            // Parsowanie odpowiedzi
            return parseAnalysisResponse(response)
        } catch (e: Exception) {
            println("Błąd podczas analizy tonu: ${e.message}")
            e.printStackTrace()
            return createDefaultAnalysis()
        }
    }
    
    /**
     * Tworzy prompt dla modelu LLM do analizy tonu
     */
    private fun createAnalysisPrompt(content: String): String {
        return """
        Przeanalizuj poniższą wiadomość email i podaj:
        1. Ogólny sentyment (VERY_NEGATIVE, NEGATIVE, NEUTRAL, POSITIVE, VERY_POSITIVE)
        2. Główne emocje (ANGER, FEAR, HAPPINESS, SADNESS, SURPRISE, DISGUST, NEUTRAL) z wartościami od 0 do 1
        3. Pilność (LOW, NORMAL, HIGH, CRITICAL)
        4. Formalność (VERY_INFORMAL, INFORMAL, NEUTRAL, FORMAL, VERY_FORMAL)
        5. Główne tematy (lista słów kluczowych)
        6. Krótkie podsumowanie treści
        
        Odpowiedź podaj w formacie JSON.
        
        Wiadomość:
        $content
        """.trimIndent()
    }
    
    /**
     * Wywołuje API modelu językowego
     */
    private fun callLlmApi(prompt: String): String {
        // W produkcyjnej wersji ten kod byłby bardziej rozbudowany
        // i obsługiwałby różne typy API modeli językowych
        
        val jsonBody = """
        {
            "model": "$model",
            "prompt": "$prompt",
            "temperature": 0.7,
            "max_tokens": 1000
        }
        """.trimIndent()
        
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = jsonBody.toRequestBody(mediaType)
        
        val request = Request.Builder()
            .url("$apiUrl/api/generate")
            .post(requestBody)
            .build()
            
        // W tej uproszczonej wersji zwracamy zaślepkę zamiast faktycznego wywołania API
        // W pełnej implementacji użylibyśmy:
        // client.newCall(request).execute().use { response -> return response.body?.string() ?: "" }
        
        return """
        {
            "sentiment": "NEUTRAL",
            "emotions": {
                "NEUTRAL": 0.8,
                "HAPPINESS": 0.2
            },
            "urgency": "NORMAL",
            "formality": "NEUTRAL",
            "topTopics": ["zapytanie", "informacja"],
            "summaryText": "Wiadomość zawiera ogólne zapytanie o informacje."
        }
        """.trimIndent()
    }
    
    /**
     * Parsuje odpowiedź API do modelu ToneAnalysis
     */
    private fun parseAnalysisResponse(response: String): ToneAnalysis {
        try {
            // W pełnej implementacji użylibyśmy Jacksona do parsowania JSON
            // Dla uproszczenia, zwracamy obiekt bezpośrednio
            
            // val analysisMap: Map<String, Any> = mapper.readValue(response)
            
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
        } catch (e: Exception) {
            println("Błąd podczas parsowania odpowiedzi: ${e.message}")
            return createDefaultAnalysis()
        }
    }
    
    /**
     * Tworzy domyślną analizę w przypadku błędów
     */
    private fun createDefaultAnalysis(): ToneAnalysis {
        return ToneAnalysis(
            sentiment = Sentiment.NEUTRAL,
            emotions = mapOf(Emotion.NEUTRAL to 1.0f),
            urgency = Urgency.NORMAL,
            formality = Formality.NEUTRAL,
            topTopics = emptyList(),
            summaryText = "Nie można przeanalizować treści wiadomości."
        )
    }
}