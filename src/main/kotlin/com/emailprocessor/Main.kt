package com.emailprocessor

import com.emailprocessor.routes.EmailProcessingRoute
import com.emailprocessor.routes.EmailSendingRoute
import com.emailprocessor.services.EmailService
import com.emailprocessor.services.LlmService
import org.apache.camel.main.Main
import java.sql.DriverManager
import java.util.Properties
import java.io.FileInputStream
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

fun main() {
    // Inicjalizacja i uruchomienie aplikacji
    val app = EmailLlmProcessor()
    app.init()
    app.start()
}

class EmailLlmProcessor {
    private val camelMain = Main()
    private val config = loadConfig()
    
    fun init() {
        println("Inicjalizacja Email LLM Processor...")
        
        // Inicjalizacja bazy danych
        initDatabase()
        
        // Inicjalizacja serwisów
        val llmService = LlmService(
            apiUrl = config.getProperty("LLM_API_URL", "http://localhost:11434"),
            model = config.getProperty("LLM_MODEL", "llama2")
        )
        
        val emailService = EmailService(llmService)
        
        // Rejestracja tras Camel
        camelMain.configure().addRoutesBuilder(EmailProcessingRoute(
            emailHost = config.getProperty("EMAIL_HOST", "localhost"),
            emailPort = config.getProperty("EMAIL_PORT", "1025").toInt(),
            emailUser = config.getProperty("EMAIL_USER", "test@example.com"),
            emailPassword = config.getProperty("EMAIL_PASSWORD", "password"),
            emailService = emailService,
            dbPath = config.getProperty("DATABASE_PATH", "data/emails.db")
        ))
        
        camelMain.configure().addRoutesBuilder(EmailSendingRoute(
            emailHost = config.getProperty("EMAIL_HOST", "localhost"),
            emailPort = config.getProperty("EMAIL_PORT", "1025").toInt(),
            emailUser = config.getProperty("EMAIL_USER", "test@example.com"),
            emailPassword = config.getProperty("EMAIL_PASSWORD", "password")
        ))
        
        println("Inicjalizacja zakończona.")
    }
    
    fun start() {
        println("Uruchamianie Email LLM Processor...")
        camelMain.start()
    }
    
    private fun loadConfig(): Properties {
        val config = Properties()
        
        // Próba załadowania zmiennych środowiskowych z pliku .env jeśli istnieje
        val envFile = File(".env")
        if (envFile.exists()) {
            FileInputStream(envFile).use { fis ->
                config.load(fis)
            }
        }
        
        // Nadpisanie zmiennymi środowiskowymi z systemu
        System.getenv().forEach { (key, value) ->
            config.setProperty(key, value)
        }
        
        return config
    }
    
    private fun initDatabase() {
        val dbPath = config.getProperty("DATABASE_PATH", "data/emails.db")
        val dbDir = File(dbPath).parentFile
        
        if (!dbDir.exists()) {
            println("Tworzenie katalogu dla bazy danych: ${dbDir.absolutePath}")
            dbDir.mkdirs()
        }
        
        println("Inicjalizacja bazy danych SQLite: $dbPath")
        
        // Połączenie z bazą danych SQLite i utworzenie tabel
        Class.forName("org.sqlite.JDBC")
        DriverManager.getConnection("jdbc:sqlite:$dbPath").use { conn ->
            conn.createStatement().use { statement ->
                // Tabela dla wiadomości email
                statement.execute("""
                    CREATE TABLE IF NOT EXISTS emails (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        from_address TEXT NOT NULL,
                        to_address TEXT NOT NULL,
                        subject TEXT,
                        content TEXT,
                        received_date TIMESTAMP,
                        processed_date TIMESTAMP,
                        tone_analysis TEXT,
                        status TEXT
                    )
                """)
            }
        }
    }
}