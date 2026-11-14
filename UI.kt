package com.learnenglishsimply.app

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private val Soft = Color(0xFFEFF8FD)

/* ---------------------------------------------------------------- root */

@Composable
fun AppRoot() {
    var user by remember { mutableStateOf(Repo.auth.currentUser) }
    DisposableEffect(Unit) {
        val l = FirebaseAuth.AuthStateListener { user = it.currentUser }
        Repo.auth.addAuthStateListener(l)
        onDispose { Repo.auth.removeAuthStateListener(l) }
    }
    val u = user
    if (u == null) AuthScreen() else ProfileGate(u)
}

@Composable
private fun ProfileGate(user: FirebaseUser) {
    var loaded by remember(user.uid) { mutableStateOf(false) }
    var profile by remember(user.uid) { mutableStateOf<Map<String, Any>?>(null) }
    var loadError by remember(user.uid) { mutableStateOf<String?>(null) }
    DisposableEffect(user.uid) {
        val reg = Repo.db.collection("users").document(user.uid).addSnapshotListener { s, e ->
            if (e != null) { loadError = e.message; loaded = true; return@addSnapshotListener }
            profile = s?.data
            loaded = true
        }
        onDispose { reg.remove() }
    }
    val p = profile
    when {
        !loaded -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
        loadError != null && p == null -> Column(
            Modifier.fillMaxSize().padding(24.dp),
            Arrangement.Center, Alignment.CenterHorizontally
        ) {
            Text("Could not load your account.\n$loadError")
            Spacer(Modifier.height(12.dp))
            Button(onClick = { Repo.auth.signOut() }) { Text("Sign out") }
        }
        p == null -> CompleteProfileScreen(user)
        else -> MainScreen(user, p)
    }
}

/* ---------------------------------------------------------------- auth */

@Composable
fun AuthScreen() {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf("") }
    var pass by remember { mutableStateOf("") }
    var creating by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val googleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        try {
            val acc = GoogleSignIn.getSignedInAccountFromIntent(res.data).getResult(ApiException::class.java)
            val cred = GoogleAuthProvider.getCredential(acc.idToken, null)
            scope.launch {
                busy = true
                try { Repo.auth.signInWithCredential(cred).await() } catch (e: Exception) { error = e.message }
                busy = false
            }
        } catch (e: ApiException) {
            error = "Google sign-in failed (code ${e.statusCode}). Check SHA-1 in Firebase."
        }
    }

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        Arrangement.Center, Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.size(84.dp).clip(RoundedCornerShape(22.dp)).background(Color(0xFF6EC6F0)),
            Alignment.Center
        ) { Text("Aa", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Bold) }
        Spacer(Modifier.height(14.dp))
        Text("Learn English Simply", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(if (creating) "Create your account" else "Welcome back", color = Color.Gray)
        Spacer(Modifier.height(22.dp))

        OutlinedTextField(
            email, { email = it.trim() }, Modifier.fillMaxWidth(), singleLine = true,
            label = { Text("Email") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(
            pass, { pass = it }, Modifier.fillMaxWidth(), singleLine = true,
            label = { Text("Password (6+ characters)") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )
        error?.let { Spacer(Modifier.height(8.dp)); Text(it, color = Color(0xFFC62828), fontSize = 13.sp) }
        Spacer(Modifier.height(14.dp))
        Button(
            onClick = {
                error = null
                if (email.isBlank() || pass.length < 6) { error = "Enter a valid email and a password of 6+ characters."; return@Button }
                scope.launch {
                    busy = true
                    try {
                        if (creating) Repo.auth.createUserWithEmailAndPassword(email, pass).await()
                        else Repo.auth.signInWithEmailAndPassword(email, pass).await()
                    } catch (e: Exception) { error = e.message }
                    busy = false
                }
            },
            enabled = !busy, modifier = Modifier.fillMaxWidth().height(50.dp)
        ) { Text(if (creating) "Create account" else "Sign in") }
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = {
                error = null
                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(ctx.getString(R.string.default_web_client_id))
                    .requestEmail().build()
                val client = GoogleSignIn.getClient(ctx, gso)
                client.signOut().addOnCompleteListener { googleLauncher.launch(client.signInIntent) }
            },
            enabled = !busy, modifier = Modifier.fillMaxWidth().height(50.dp)
        ) { Text("Continue with Google") }
        TextButton(onClick = { creating = !creating; error = null }) {
            Text(if (creating) "I already have an account" else "New here? Create an account")
        }
    }
}

@Composable
private fun CompleteProfileScreen(user: FirebaseUser) {
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(user.displayName ?: "") }
    var grade by remember { mutableStateOf("") }
    var menu by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        Arrangement.Center, Alignment.CenterHorizontally
    ) {
        Text("One last step", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text("Tell us about you", color = Color.Gray)
        Spacer(Modifier.height(20.dp))
        OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), singleLine = true, label = { Text("Student name") })
        Spacer(Modifier.height(10.dp))
        Box(Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = { menu = true }, Modifier.fillMaxWidth().height(54.dp)) {
                Text(if (grade.isEmpty()) "Choose your grade" else grade)
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                AppConfig.GRADES.forEach { g ->
                    DropdownMenuItem(text = { Text(g) }, onClick = { grade = g; menu = false })
                }
            }
        }
        error?.let { Spacer(Modifier.height(8.dp)); Text(it, color = Color(0xFFC62828), fontSize = 13.sp) }
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = {
                if (name.isBlank() || grade.isEmpty()) { error = "Please enter your name and grade."; return@Button }
                scope.launch {
                    busy = true
                    try { Repo.saveProfile(user.uid, name.trim(), user.email ?: "", grade) }
                    catch (e: Exception) { error = e.message }
                    busy = false
                }
            },
            enabled = !busy, modifier = Modifier.fillMaxWidth().height(50.dp)
        ) { Text("Start learning") }
        TextButton(onClick = { Repo.auth.signOut() }) { Text("Sign out") }
    }
}

/* ---------------------------------------------------------------- main */

private enum class Tab(val label: String, val icon: ImageVector) {
    Videos("Videos", Icons.Filled.PlayArrow),
    Reviews("Reviews", Icons.Filled.Star),
    Chat("Group Chat", Icons.Filled.Email),
    Account("Personal Account", Icons.Filled.Person)
}

@Composable
private fun MainScreen(user: FirebaseUser, profile: Map<String, Any>) {
    val ctx = LocalContext.current
    var tab by remember { mutableStateOf(Tab.Videos) }
    var showReward by remember { mutableStateOf(false) }
    val name = profile["name"] as? String ?: ""

    LaunchedEffect(name) {
        Repo.prefs(ctx).edit().putString("name", name).apply()
        ReminderWorker.schedule(ctx)
    }
    LaunchedEffect(user.uid) {
        try { if (Repo.claimDailyReward(user.uid)) showReward = true } catch (_: Exception) { }
    }

    if (showReward) DailyRewardDialog(name) { showReward = false }

    Scaffold(
        containerColor = Color.White,
        bottomBar = {
            NavigationBar(containerColor = Soft) {
                Tab.values().forEach { t ->
                    NavigationBarItem(
                        selected = tab == t, onClick = { tab = t },
                        icon = { Icon(t.icon, t.label) },
                        label = { Text(t.label, fontSize = 10.sp, maxLines = 1) }
                    )
                }
            }
        }
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when (tab) {
                Tab.Videos -> VideosTab()
                Tab.Reviews -> ReviewsTab(user, name)
                Tab.Chat -> ChatTab(user, name)
                Tab.Account -> AccountTab(user, profile)
            }
        }
    }
}

@Composable
private fun DailyRewardDialog(name: String, onClose: () -> Unit) {
    val t = rememberInfiniteTransition(label = "fire")
    val s by t.animateFloat(
        0.85f, 1.25f, infiniteRepeatable(tween(650), RepeatMode.Reverse), label = "s"
    )
    val first = name.substringBefore(' ').ifBlank { "Champion" }
    AlertDialog(
        onDismissRequest = onClose,
        confirmButton = { Button(onClick = onClose) { Text("Let's learn!") } },
        title = { Text("Welcome back, $first!", textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
        text = {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🔥", fontSize = 72.sp, modifier = Modifier.scale(s))
                Spacer(Modifier.height(8.dp))
                Text("+${AppConfig.DAILY_COINS} Motivation Coins", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(6.dp))
                Text("Every day you learn makes you stronger. Keep going!", color = Color.Gray)
            }
        }
    )
}

/* ---------------------------------------------------------------- videos */

@Composable
private fun VideosTab() {
    val ctx = LocalContext.current
    var items by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }
    DisposableEffect(Unit) {
        val reg = Repo.db.collection("videos").orderBy("order").addSnapshotListener { s, _ ->
            items = s?.documents?.mapNotNull { it.data } ?: emptyList()
            loaded = true
        }
        onDispose { reg.remove() }
    }
    Column(Modifier.fillMaxSize()) {
        Header("Lesson Videos")
        if (loaded && items.isEmpty()) Empty("No videos yet. New lessons will appear here soon.")
        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(items) { v ->
                Card(
                    Modifier.fillMaxWidth().clickable {
                        val url = v["url"] as? String ?: return@clickable
                        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    },
                    colors = CardDefaults.cardColors(containerColor = Soft)
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary), Alignment.Center) {
                            Icon(Icons.Filled.PlayArrow, null, tint = Color.White)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(v["title"] as? String ?: "Lesson", fontWeight = FontWeight.SemiBold)
                            (v["description"] as? String)?.takeIf { it.isNotBlank() }?.let { Text(it, color = Color.Gray, fontSize = 13.sp) }
                        }
                    }
                }
            }
        }
    }
}

/* ---------------------------------------------------------------- reviews */

@Composable
private fun ReviewsTab(user: FirebaseUser, name: String) {
    val scope = rememberCoroutineScope()
    var items by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var rating by remember { mutableStateOf(5) }
    var text by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    DisposableEffect(Unit) {
        val reg = Repo.db.collection("reviews").orderBy("createdAt", Query.Direction.DESCENDING).limit(100)
            .addSnapshotListener { s, _ -> items = s?.documents?.mapNotNull { it.data } ?: emptyList() }
        onDispose { reg.remove() }
    }
    Column(Modifier.fillMaxSize()) {
        Header("Reviews")
        Column(Modifier.padding(horizontal = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                (1..5).forEach { i ->
                    Text(
                        if (i <= rating) "★" else "☆", fontSize = 32.sp,
                        color = Color(0xFFF5B301),
                        modifier = Modifier.clickable { rating = i }.padding(end = 4.dp)
                    )
                }
            }
            OutlinedTextField(text, { if (it.length <= 500) text = it }, Modifier.fillMaxWidth(), label = { Text("Write your review") })
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    scope.launch {
                        busy = true
                        try {
                            Repo.db.collection("reviews").add(
                                mapOf(
                                    "uid" to user.uid, "name" to name, "rating" to rating,
                                    "text" to text.trim(), "createdAt" to FieldValue.serverTimestamp()
                                )
                            ).await()
                            text = ""
                        } catch (_: Exception) { }
                        busy = false
                    }
                },
                enabled = !busy && text.isNotBlank()
            ) { Text("Post review") }
        }
        Spacer(Modifier.height(8.dp))
        HorizontalDivider()
        if (items.isEmpty()) Empty("No reviews yet. Be the first!")
        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(items) { r ->
                val stars = ((r["rating"] as? Long) ?: 5L).toInt().coerceIn(1, 5)
                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Soft)) {
                    Column(Modifier.padding(12.dp)) {
                        Row {
                            Text(r["name"] as? String ?: "Student", fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            Text("★".repeat(stars), color = Color(0xFFF5B301))
                        }
                        Text(r["text"] as? String ?: "")
                    }
                }
            }
        }
    }
}

/* ---------------------------------------------------------------- group chat */

@Composable
private fun ChatTab(user: FirebaseUser, name: String) {
    val scope = rememberCoroutineScope()
    var items by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var text by remember { mutableStateOf("") }
    val state = rememberLazyListState()
    DisposableEffect(Unit) {
        val reg = Repo.db.collection("messages").orderBy("createdAt").limitToLast(100)
            .addSnapshotListener { s, _ -> items = s?.documents?.mapNotNull { it.data } ?: emptyList() }
        onDispose { reg.remove() }
    }
    LaunchedEffect(items.size) { if (items.isNotEmpty()) state.animateScrollToItem(items.size - 1) }

    Column(Modifier.fillMaxSize()) {
        Header("Group Chat")
        if (items.isEmpty()) Empty("Say hello to your classmates 👋")
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp),
            state = state, verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(items) { m ->
                val mine = m["uid"] == user.uid
                Row(Modifier.fillMaxWidth(), horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start) {
                    Column(
                        Modifier.clip(RoundedCornerShape(14.dp))
                            .background(if (mine) MaterialTheme.colorScheme.primary else Soft)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        if (!mine) Text(m["name"] as? String ?: "Student", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text(m["text"] as? String ?: "", color = if (mine) Color.White else Color.Black)
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(text, { if (it.length <= 500) text = it }, Modifier.weight(1f), placeholder = { Text("Message") }, maxLines = 3)
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = {
                    val t = text.trim()
                    if (t.isEmpty()) return@Button
                    text = ""
                    scope.launch {
                        try {
                            Repo.db.collection("messages").add(
                                mapOf("uid" to user.uid, "name" to name, "text" to t, "createdAt" to FieldValue.serverTimestamp())
                            ).await()
                        } catch (_: Exception) { }
                    }
                }
            ) { Text("Send") }
        }
    }
}

/* ---------------------------------------------------------------- account */

@Composable
private fun AccountTab(user: FirebaseUser, profile: Map<String, Any>) {
    val ctx = LocalContext.current
    val prefs = remember { Repo.prefs(ctx) }
    var on by remember { mutableStateOf(prefs.getBoolean("reminder_on", true)) }
    var hour by remember { mutableStateOf(prefs.getInt("reminder_hour", 18)) }
    var menu by remember { mutableStateOf(false) }
    val coins = (profile["coins"] as? Long) ?: 0L

    Column(Modifier.fillMaxSize()) {
        Header("Personal Account")
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Soft)) {
                Column(Modifier.padding(16.dp)) {
                    Text(profile["name"] as? String ?: "", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text(profile["email"] as? String ?: "", color = Color.Gray)
                    Text("Grade: ${profile["grade"] ?: ""}")
                    Spacer(Modifier.height(8.dp))
                    Text("🪙 $coins Motivation Coins", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                }
            }
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Soft)) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Daily reminder", Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                        Switch(checked = on, onCheckedChange = {
                            on = it; prefs.edit().putBoolean("reminder_on", it).apply(); ReminderWorker.schedule(ctx)
                        })
                    }
                    if (on) {
                        Box {
                            OutlinedButton(onClick = { menu = true }) { Text("Remind me at ${"%02d".format(hour)}:00") }
                            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                                (6..23).forEach { h ->
                                    DropdownMenuItem(text = { Text("%02d:00".format(h)) }, onClick = {
                                        hour = h; menu = false
                                        prefs.edit().putInt("reminder_hour", h).apply(); ReminderWorker.schedule(ctx)
                                    })
                                }
                            }
                        }
                    }
                }
            }
            OutlinedButton(
                onClick = {
                    ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/${AppConfig.SUPPORT_WHATSAPP}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                },
                Modifier.fillMaxWidth()
            ) { Text("Support on WhatsApp") }
            OutlinedButton(
                onClick = {
                    ctx.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${AppConfig.ADMIN_EMAIL}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                },
                Modifier.fillMaxWidth()
            ) { Text("Support by Gmail") }
            Button(onClick = { Repo.auth.signOut() }, Modifier.fillMaxWidth()) { Text("Sign out") }
        }
    }
}

/* ---------------------------------------------------------------- small helpers */

@Composable
private fun Header(title: String) {
    Text(title, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(16.dp))
}

@Composable
private fun Empty(msg: String) {
    Text(msg, color = Color.Gray, modifier = Modifier.padding(16.dp))
}
