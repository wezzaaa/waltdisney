package fr.isen.sahartayssir.waltdisney

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import fr.isen.sahartayssir.waltdisney.models.Film
import fr.isen.sahartayssir.waltdisney.ui.theme.*

class FilmDetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val filmId = intent.getStringExtra("filmId").orEmpty()
        setContent {
            WaltdisneyTheme {
                FilmDetailScreen(filmId) { finish() }
            }
        }
    }
}

@Composable
fun FilmDetailScreen(filmId: String, onBack: () -> Unit) {
    var film by remember { mutableStateOf(Film()) }
    var currentStatus by remember { mutableStateOf("") }
    var owners by remember { mutableStateOf(listOf<String>()) }
    var isLoading by remember { mutableStateOf(true) }

    // ✅ FIX : URL Firebase explicite (même que MainActivity et ProfileActivity)
    val db = FirebaseDatabase.getInstance(
        "https://walt-disney-708e2-default-rtdb.europe-west1.firebasedatabase.app"
    ).reference
    val uid = FirebaseAuth.getInstance().currentUser?.uid

    fun loadDetails() {
        val userId = uid
        if (filmId.isBlank()) {
            isLoading = false
            return
        }

        isLoading = true

        db.child("films").child(filmId).get()
            .addOnSuccessListener { filmSnap ->
                film = filmSnap.getValue(Film::class.java) ?: Film()

                if (userId == null) {
                    currentStatus = ""
                    owners = emptyList()
                    isLoading = false
                    return@addOnSuccessListener
                }

                db.child("userFilmStatus").child(userId).child(filmId).child("status").get()
                    .addOnSuccessListener { statusSnap ->
                        currentStatus = statusSnap.getValue(String::class.java).orEmpty()

                        db.child("ownersGettingRid").child(filmId).get()
                            .addOnSuccessListener { ownersSnap ->
                                val ownerIds = ownersSnap.children.mapNotNull { it.key }

                                if (ownerIds.isEmpty()) {
                                    owners = emptyList()
                                    isLoading = false
                                } else {
                                    val names = mutableListOf<String>()
                                    var remaining = ownerIds.size

                                    ownerIds.forEach { ownerId ->
                                        db.child("users").child(ownerId).child("displayName").get()
                                            .addOnSuccessListener { nameSnap ->
                                                names.add(nameSnap.getValue(String::class.java) ?: "Anonyme")
                                            }
                                            .addOnCompleteListener {
                                                remaining--
                                                if (remaining == 0) {
                                                    owners = names.distinct()
                                                    isLoading = false
                                                }
                                            }
                                    }
                                }
                            }
                            .addOnFailureListener {
                                owners = emptyList()
                                isLoading = false
                            }
                    }
                    .addOnFailureListener {
                        currentStatus = ""
                        owners = emptyList()
                        isLoading = false
                    }
            }
            .addOnFailureListener {
                // ✅ FIX : s'assurer qu'on sort du loading même en cas d'échec
                isLoading = false
            }
    }

    LaunchedEffect(filmId) {
        loadDetails()
    }

    fun updateStatus(s: String?) {
        val userId = uid ?: return

        isLoading = true
        val updates = mutableMapOf<String, Any?>()

        if (s == null) {
            updates["userFilmStatus/$userId/$filmId"] = null
            updates["ownersGettingRid/$filmId/$userId"] = null
        } else {
            updates["userFilmStatus/$userId/$filmId/status"] = s
            updates["ownersGettingRid/$filmId/$userId"] =
                if (s == "want_to_get_rid") true else null
        }

        db.updateChildren(updates).addOnSuccessListener {
            if (s == null) onBack() else loadDetails()
        }.addOnFailureListener {
            isLoading = false
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(MagicDeepPurple)
    ) {
        AsyncImage(
            model = film.imageUrl,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .blur(40.dp),
            contentScale = ContentScale.Crop,
            alpha = 0.3f
        )

        if (isLoading) {
            CircularProgressIndicator(
                Modifier.align(Alignment.Center),
                color = Color.White
            )
        } else {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    "←",
                    color = Color.White,
                    fontSize = 30.sp,
                    modifier = Modifier.clickable { onBack() }
                )

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp)
                        .padding(vertical = 20.dp),
                    shape = RoundedCornerShape(32.dp),
                    border = BorderStroke(2.dp, GlassBorder)
                ) {
                    AsyncImage(
                        model = film.imageUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop
                    )
                }

                Text(
                    film.title,
                    color = Color.White,
                    style = MaterialTheme.typography.displayLarge
                )

                Text(
                    film.category.uppercase(),
                    color = MagicCyan,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(30.dp))

                val statuses = listOf(
                    "watched" to "Déjà vu",
                    "want_to_watch" to "Envie de voir",
                    "own_dvd" to "Possédé (DVD)",
                    "want_to_get_rid" to "Se débarrasser"
                )

                statuses.forEach { (key, label) ->
                    StatusMagicButton(label, currentStatus == key) {
                        updateStatus(key)
                    }
                }

                if (owners.isNotEmpty()) {
                    Text(
                        "Disponible chez : ${owners.joinToString(", ")}",
                        color = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }

                Spacer(Modifier.height(40.dp))

                TextButton(
                    onClick = { updateStatus(null) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Retirer de ma liste", color = MagicPink)
                }
            }
        }
    }
}

@Composable
fun StatusMagicButton(t: String, sel: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        color = if (sel) Color.White else GlassWhite,
        border = BorderStroke(1.dp, if (sel) Color.White else GlassBorder)
    ) {
        Box(
            Modifier.padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                t,
                color = if (sel) MagicDeepPurple else Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}