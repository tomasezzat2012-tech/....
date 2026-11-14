package com.learnenglishsimply.app

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Repo {
    val auth: FirebaseAuth get() = FirebaseAuth.getInstance()
    val db: FirebaseFirestore get() = FirebaseFirestore.getInstance()

    fun today(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    fun now(): String = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())

    /** Creates users/{uid} and notifies the admin with the 4 required fields. */
    suspend fun saveProfile(uid: String, name: String, email: String, grade: String) {
        val date = now()
        db.collection("users").document(uid).set(
            mapOf(
                "name" to name, "email" to email, "grade" to grade,
                "registeredAt" to date, "coins" to 0, "lastRewardDate" to ""
            )
        ).await()
        notifyAdmin(name, email, grade, date)
    }

    private suspend fun notifyAdmin(name: String, email: String, grade: String, date: String) =
        withContext(Dispatchers.IO) {
            if (AppConfig.ADMIN_FORM_URL.isBlank()) return@withContext
            try {
                val body = JSONObject()
                    .put("_subject", "New student registration - Learn English Simply")
                    .put("Student Name", name)
                    .put("Student Email", email)
                    .put("Student Grade", grade)
                    .put("Registration Date", date)
                    .toString()
                val c = URL(AppConfig.ADMIN_FORM_URL).openConnection() as HttpURLConnection
                c.requestMethod = "POST"
                c.connectTimeout = 10000
                c.readTimeout = 10000
                c.doOutput = true
                c.setRequestProperty("Content-Type", "application/json")
                c.setRequestProperty("Accept", "application/json")
                c.outputStream.use { it.write(body.toByteArray()) }
                c.responseCode
                c.disconnect()
            } catch (_: Exception) { /* never block sign-up because of the notification */ }
        }

    /** Gives DAILY_COINS once per day. Returns true only the first time in a day. */
    suspend fun claimDailyReward(uid: String): Boolean {
        val ref = db.collection("users").document(uid)
        val today = today()
        return db.runTransaction { tr ->
            val snap = tr.get(ref)
            if (!snap.exists() || snap.getString("lastRewardDate") == today) {
                false
            } else {
                val coins = (snap.getLong("coins") ?: 0L) + AppConfig.DAILY_COINS
                tr.update(ref, mapOf("coins" to coins, "lastRewardDate" to today))
                true
            }
        }.await()
    }

    fun prefs(c: Context) = c.getSharedPreferences("les", Context.MODE_PRIVATE)
}
