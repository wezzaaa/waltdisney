package fr.isen.sahartayssir.waltdisney

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import fr.isen.sahartayssir.waltdisney.models.Film

class FilmDetailActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val filmId = intent.getStringExtra("filmId").orEmpty()

        setContent {
            FilmDetailScreen(
                filmId = filmId,
                onBack = { finish() }
            )
        }
    }
}

@Composable
fun FilmDetailScreen(
    filmId: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    var film by remember { mutableStateOf(Film()) }
    var currentStatus by remember { mutableStateOf("") }
    var owners by remember { mutableStateOf(listOf<String>()) }

    val uid = FirebaseAuth.getInstance().currentUser?.uid

    val db = FirebaseDatabase
        .getInstance("https://walt-disney-708e2-default-rtdb.europe-west1.firebasedatabase.app")
        .reference

    fun loadFilmDetails() {
        if (filmId.isBlank()) return

        db.child("films").child(filmId).get()
            .addOnSuccessListener { snapshot ->
                val loadedFilm = snapshot.getValue(Film::class.java)
                if (loadedFilm != null) {
                    film = loadedFilm
                }
            }
            .addOnFailureListener { error ->
                Log.e("FIREBASE_TEST", "Film detail read failed", error)
            }

        if (uid != null) {
            db.child("userFilmStatus").child(uid).child(filmId).child("status").get()
                .addOnSuccessListener { snapshot ->
                    currentStatus = snapshot.getValue(String::class.java).orEmpty()
                }
        }

        db.child("ownersGettingRid").child(filmId).get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.hasChildren()) {
                    owners = emptyList()
                    return@addOnSuccessListener
                }

                val uidList = snapshot.children.mapNotNull { it.key }
                val names = mutableListOf<String>()
                var processed = 0

                for (ownerUid in uidList) {
                    db.child("users").child(ownerUid).child("displayName").get()
                        .addOnSuccessListener { nameSnapshot ->
                            val name = nameSnapshot.getValue(String::class.java).orEmpty()
                            names.add(if (name.isNotBlank()) name else ownerUid)
                            processed++
                            if (processed == uidList.size) {
                                owners = names
                            }
                        }
                        .addOnFailureListener {
                            names.add(ownerUid)
                            processed++
                            if (processed == uidList.size) {
                                owners = names
                            }
                        }
                }
            }
    }

    fun saveFilmStatus(status: String) {
        if (uid == null) {
            Toast.makeText(context, "User not connected", Toast.LENGTH_SHORT).show()
            return
        }

        val updates = mutableMapOf<String, Any?>()
        updates["userFilmStatus/$uid/$filmId/status"] = status

        if (status == "want_to_get_rid") {
            updates["ownersGettingRid/$filmId/$uid"] = true
        } else {
            updates["ownersGettingRid/$filmId/$uid"] = null
        }

        db.updateChildren(updates)
            .addOnSuccessListener {
                currentStatus = status
                loadFilmDetails()
                Toast.makeText(context, "Status saved: $status", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { error ->
                Toast.makeText(context, error.message ?: "Save failed", Toast.LENGTH_LONG).show()
            }
    }

    LaunchedEffect(filmId) {
        loadFilmDetails()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Back")
        }

        Text(
            text = "Film Detail",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(text = "Title: ${film.title}", style = MaterialTheme.typography.bodyLarge)
        Text(text = "Universe: ${film.universeId}", style = MaterialTheme.typography.bodyLarge)
        Text(text = "Category: ${film.category}", style = MaterialTheme.typography.bodyLarge)
        Text(text = "Release date: ${film.releaseDate}", style = MaterialTheme.typography.bodyLarge)
        Text(
            text = if (currentStatus.isNotBlank()) "Current status: $currentStatus" else "Current status: none",
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = if (owners.isNotEmpty()) {
                "Users who want to get rid of it: ${owners.joinToString(", ")}"
            } else {
                "Users who want to get rid of it: none"
            },
            style = MaterialTheme.typography.bodyLarge
        )

        Button(
            onClick = { saveFilmStatus("watched") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Watched")
        }

        Button(
            onClick = { saveFilmStatus("want_to_watch") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Want to watch")
        }

        Button(
            onClick = { saveFilmStatus("own_dvd") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Own DVD")
        }

        Button(
            onClick = { saveFilmStatus("want_to_get_rid") },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Want to get rid")
        }
    }
}