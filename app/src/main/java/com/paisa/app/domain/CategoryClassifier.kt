package com.paisa.app.domain

interface CategoryClassifier {
    fun categorize(text: String): String
}
