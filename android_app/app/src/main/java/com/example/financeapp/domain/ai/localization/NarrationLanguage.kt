package com.example.financeapp.domain.ai.localization

/** Four narration modes. [code] is sent to the proxy; [locale] is the Android speech tag. */
enum class NarrationLanguage(val code: String, val locale: String, val label: String) {
    ENGLISH("en", "en-IN", "English"),
    HINGLISH("hi-Latn", "hi-IN", "Hinglish"),
    HINDI("hi-Deva", "hi-IN", "Hindi"),
    MARATHI("mr-Deva", "mr-IN", "Marathi"),
    ;

    fun examples(): LanguageExamples = when (this) {
        ENGLISH -> LanguageExamples(
            cashOverview = "Bank cash is ₹45,200. Card dues are separate and are not subtracted from cash.",
            income = "Salary is ₹85,000, expected on the 1st.",
            shock = "A ₹25,000 medical bill in month 3 uses the cash reserve.",
        )
        HINGLISH -> LanguageExamples(
            cashOverview = "Bank cash ₹45,200 hai. Card dues alag hain, cash mein se minus nahi hote.",
            income = "Salary ₹85,000 hai, 1st ko aani chahiye.",
            shock = "Month 3 mein ₹25,000 ka medical bill reserve use karega.",
        )
        HINDI -> LanguageExamples(
            cashOverview = "बैंक में नकद ₹45,200 है। कार्ड का बकाया अलग है, नकद में से नहीं कटा।",
            income = "वेतन ₹85,000 है, 1 तारीख को आने की उम्मीद है।",
            shock = "तीसरे महीने में ₹25,000 का मेडिकल बिल रिज़र्व से जाएगा।",
        )
        MARATHI -> LanguageExamples(
            cashOverview = "बँकेत रोख ₹45,200 आहे. कार्डची थकबाकी वेगळी आहे, रोख रकमेतून वजा नाही.",
            income = "पगार ₹85,000 आहे, 1 तारखेला अपेक्षित आहे.",
            shock = "तिसऱ्या महिन्यात ₹25,000 चे वैद्यकीय बिल रिझर्व वापरेल.",
        )
    }

    companion object {
        fun fromCode(code: String): NarrationLanguage? = entries.firstOrNull { it.code == code }
    }
}

data class LanguageExamples(val cashOverview: String, val income: String, val shock: String)
