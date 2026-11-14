package com.learnenglishsimply.app

object AppConfig {
    /** Admin email that must receive every new-student notification. */
    const val ADMIN_EMAIL = "to23454mas@gmail.com"
    const val SUPPORT_WHATSAPP = "201223073828"

    /**
     * Formspree endpoint, e.g. "https://formspree.io/f/xxxxxxxx".
     * Create the form on formspree.io with ADMIN_EMAIL as the receiving email,
     * then paste the URL here. Empty = notification is skipped (student is still saved in Firestore).
     */
    const val ADMIN_FORM_URL = ""

    const val DAILY_COINS = 15

    val GRADES = listOf(
        "Primary 4", "Primary 5", "Primary 6",
        "Prep 1", "Prep 2", "Prep 3",
        "Secondary 1", "Secondary 2", "Secondary 3",
        "University / Adult"
    )
}
