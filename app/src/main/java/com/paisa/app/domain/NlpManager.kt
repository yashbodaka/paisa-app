package com.paisa.app.domain

import android.content.Context
import com.google.mlkit.nl.entityextraction.*
import kotlinx.coroutines.tasks.await
import android.util.Log

/**
 * Handles on-device NLP using Google ML Kit.
 * Detects amounts, dates, and names even in conversational text.
 */
class NlpManager(private val context: Context) {

    private val options = EntityExtractorOptions.Builder(EntityExtractorOptions.ENGLISH)
        .build()
    
    private val client = EntityExtraction.getClient(options)

    suspend fun extract(text: String): List<EntityAnnotation> {
        return try {
            // Ensure model is present (offline)
            client.downloadModelIfNeeded().await()
            
            val params = EntityExtractionParams.Builder(text).build()
            client.annotate(params).await()
        } catch (e: Exception) {
            Log.e("NlpManager", "Extraction failed", e)
            emptyList()
        }
    }

    /**
     * Finds the first money amount in the text using ML Kit.
     * Returns value in paise if found.
     */
    suspend fun findAmountPaise(text: String): Long? {
        val annotations = extract(text)
        for (annotation in annotations) {
            for (entity in annotation.entities) {
                if (entity is MoneyEntity) {
                    // Convert to paise
                    val value = entity.integerPart.toLong() * 100L + entity.fractionalPart.toLong()
                    return value
                }
                if (entity is PaymentCardEntity) {
                    // Sometimes card numbers are detected, ignore
                }
            }
        }
        return null
    }

    fun close() {
        client.close()
    }
}
