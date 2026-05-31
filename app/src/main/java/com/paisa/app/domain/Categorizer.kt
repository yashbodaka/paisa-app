package com.paisa.app.domain

import android.content.Context
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.text.textembedder.TextEmbedder
import com.google.mediapipe.tasks.text.textembedder.TextEmbedder.TextEmbedderOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.sqrt

class NlpCategorizer(private val context: Context) : CategoryClassifier {

    private var textEmbedder: TextEmbedder? = null
    
    // Semantic Centroids (Reference Vectors)
    private val categoryVectors = mutableMapOf<String, FloatArray>()

    private val categoryDescriptions = mapOf(
        "food" to "meals, food, restaurants, dinner, lunch, breakfast, khana, nasta, snacks, zomato, swiggy, biryani, pizza, tea, coffee, drinks, eating out",
        "groceries" to "groceries, grocery, milk, vegetables, fruits, eggs, bread, butter, blinkit, zepto, instamart, rashan, doodh, sabji, supermarket",
        "shopping" to "shopping, clothes, shoes, fashion, electronics, mobile, accessories, amazon, flipkart, myntra, mall, market, purchase, goods",
        "travel" to "travel, auto, taxi, uber, ola, petrol, diesel, fuel, cng, bus, train, metro, rickshaw, rapido, transport, commute, vehicle maintenance",
        "rent" to "rent, room rent, house rent, flat rent, landlord, kiraya, accommodation, housing",
        "health" to "health, medicine, doctor, hospital, clinic, pharmacy, medical, dawa, gym, workout, fitness, dental, checkup",
        "bills" to "utility bills, electricity, water, gas, recharge, mobile bill, wifi, internet, rent, house maintenance, insurance, subscriptions",
        "education" to "education, books, course, class, college, school, fees, tuition, kitab, exam, learning",
        "entertainment" to "movie, cinema, ott, netflix, games, party, outing, club, drinks, hobby, fun, concert",
        "investment" to "investment, stocks, mutual funds, sip, gold, crypto, savings, fixed deposit, shares, trading",
        "personal" to "personal care, salon, barber, grooming, gifts, cosmetics, haircut, spa"
    )

    suspend fun init() = withContext(Dispatchers.IO) {
        try {
            val baseOptions = BaseOptions.builder()
                .setModelAssetPath("universal_sentence_encoder.tflite")
                .build()

            val options = TextEmbedderOptions.builder()
                .setBaseOptions(baseOptions)
                .build()

            textEmbedder = TextEmbedder.createFromOptions(context, options)
            
            // Pre-calculate category anchors using their descriptions
            calculateCategoryVectors()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun calculateCategoryVectors() {
        categoryDescriptions.forEach { (category, description) ->
            val vector = getEmbedding(description)
            if (vector != null) {
                categoryVectors[category] = vector
            }
        }
    }

    private fun getEmbedding(text: String): FloatArray? {
        val embedder = textEmbedder ?: return null
        return try {
            val result = embedder.embed(text)
            result.embeddingResult().embeddings()[0].floatEmbedding()
        } catch (e: Exception) {
            null
        }
    }

    override fun categorize(text: String): String {
        val inputVector = getEmbedding(text) ?: return "other"
        
        var bestCategory = "other"
        var maxSimilarity = -1f

        categoryVectors.forEach { (category, vector) ->
            val similarity = cosineSimilarity(inputVector, vector)
            if (similarity > maxSimilarity) {
                maxSimilarity = similarity
                bestCategory = category
            }
        }

        return if (maxSimilarity > 0.32f) bestCategory else "other"
    }

    private fun cosineSimilarity(v1: FloatArray, v2: FloatArray): Float {
        var dotProduct = 0f
        var normA = 0f
        var normB = 0f
        for (i in v1.indices) {
            dotProduct += v1[i] * v2[i]
            normA += v1[i] * v1[i]
            normB += v2[i] * v2[i]
        }
        val denominator = sqrt(normA.toDouble()) * sqrt(normB.toDouble())
        return if (denominator == 0.0) 0f else (dotProduct / denominator).toFloat()
    }
    
    fun close() {
        textEmbedder?.close()
    }
}
