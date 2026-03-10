package fr.isen.sahartayssir.waltdisney

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import fr.isen.sahartayssir.waltdisney.models.Film
import fr.isen.sahartayssir.waltdisney.models.Universe

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            HomeScreen(
                onLogout = {
                    FirebaseAuth.getInstance().signOut()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                },
                onOpenProfile = {
                    startActivity(Intent(this, ProfileActivity::class.java))
                },
                onOpenFilmDetail = { filmId ->
                    val intent = Intent(this, FilmDetailActivity::class.java)
                    intent.putExtra("filmId", filmId)
                    startActivity(intent)
                }
            )
        }
    }
}

@Composable
fun HomeScreen(
    onLogout: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenFilmDetail: (String) -> Unit
) {
    val context = LocalContext.current
    var displayName by remember { mutableStateOf("") }
    var universes by remember { mutableStateOf(listOf<Pair<String, Universe>>()) }
    var films by remember { mutableStateOf(listOf<Pair<String, Film>>()) }
    var selectedUniverseId by remember { mutableStateOf<String?>(null) }
    var userStatuses by remember { mutableStateOf(mapOf<String, String>()) }
    var ownersByFilm by remember { mutableStateOf(mapOf<String, List<String>>()) }
    var showOnlyMyStatuses by remember { mutableStateOf(false) }

    val auth = FirebaseAuth.getInstance()
    val uid = auth.currentUser?.uid

    val db = FirebaseDatabase
        .getInstance("https://walt-disney-708e2-default-rtdb.europe-west1.firebasedatabase.app")
        .reference

    fun loadUserStatuses() {
        if (uid == null) return

        db.child("userFilmStatus").child(uid).get()
            .addOnSuccessListener { snapshot ->
                val tempMap = mutableMapOf<String, String>()
                for (filmSnapshot in snapshot.children) {
                    val filmId = filmSnapshot.key.orEmpty()
                    val status = filmSnapshot.child("status").getValue(String::class.java).orEmpty()
                    if (filmId.isNotBlank() && status.isNotBlank()) {
                        tempMap[filmId] = status
                    }
                }
                userStatuses = tempMap
            }
            .addOnFailureListener { error ->
                Log.e("FIREBASE_TEST", "User statuses read failed", error)
            }
    }

    fun loadOwnersGettingRid() {
        db.child("ownersGettingRid").get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.hasChildren()) {
                    ownersByFilm = emptyMap()
                    return@addOnSuccessListener
                }

                for (filmSnapshot in snapshot.children) {
                    val filmId = filmSnapshot.key.orEmpty()
                    val uidList = filmSnapshot.children.mapNotNull { it.key }

                    if (filmId.isBlank() || uidList.isEmpty()) continue

                    val names = mutableListOf<String>()
                    var processed = 0

                    for (ownerUid in uidList) {
                        db.child("users").child(ownerUid).child("displayName").get()
                            .addOnSuccessListener { nameSnapshot ->
                                val name = nameSnapshot.getValue(String::class.java).orEmpty()
                                names.add(if (name.isNotBlank()) name else ownerUid)
                                processed++

                                if (processed == uidList.size) {
                                    ownersByFilm = ownersByFilm.toMutableMap().apply {
                                        put(filmId, names)
                                    }
                                }
                            }
                            .addOnFailureListener {
                                names.add(ownerUid)
                                processed++

                                if (processed == uidList.size) {
                                    ownersByFilm = ownersByFilm.toMutableMap().apply {
                                        put(filmId, names)
                                    }
                                }
                            }
                    }
                }
            }
            .addOnFailureListener { error ->
                Log.e("FIREBASE_TEST", "Owners getting rid read failed", error)
            }
    }

    LaunchedEffect(Unit) {
        if (uid != null) {
            db.child("users").child(uid).child("displayName").get()
                .addOnSuccessListener { snapshot ->
                    displayName = snapshot.getValue(String::class.java).orEmpty()
                }
                .addOnFailureListener { error ->
                    Log.e("FIREBASE_TEST", "Display name read failed", error)
                }
        }

        db.child("universes").get()
            .addOnSuccessListener { snapshot ->
                val tempList = mutableListOf<Pair<String, Universe>>()
                for (child in snapshot.children) {
                    val universe = child.getValue(Universe::class.java)
                    if (universe != null) {
                        tempList.add(child.key.orEmpty() to universe)
                    }
                }
                universes = tempList
            }

        db.child("films").get()
            .addOnSuccessListener { snapshot ->
                val tempList = mutableListOf<Pair<String, Film>>()
                for (child in snapshot.children) {
                    val film = child.getValue(Film::class.java)
                    if (film != null) {
                        tempList.add(child.key.orEmpty() to film)
                    }
                }
                films = tempList
            }

        loadUserStatuses()
        loadOwnersGettingRid()
    }

    val filteredFilms = when {
        showOnlyMyStatuses -> films.filter { (filmId, _) -> userStatuses.containsKey(filmId) }
        selectedUniverseId == null -> emptyList()
        else -> films.filter { it.second.universeId == selectedUniverseId }
    }

    fun saveFilmStatus(filmId: String, status: String) {
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
                userStatuses = userStatuses.toMutableMap().apply {
                    this[filmId] = status
                }
                loadOwnersGettingRid()
                Toast.makeText(context, "Status saved: $status", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { error ->
                Toast.makeText(context, error.message ?: "Save failed", Toast.LENGTH_LONG).show()
            }
    }

    fun deleteFilmStatus(filmId: String) {
        if (uid == null) {
            Toast.makeText(context, "User not connected", Toast.LENGTH_SHORT).show()
            return
        }

        val updates = mutableMapOf<String, Any?>()
        updates["userFilmStatus/$uid/$filmId"] = null
        updates["ownersGettingRid/$filmId/$uid"] = null

        db.updateChildren(updates)
            .addOnSuccessListener {
                userStatuses = userStatuses.toMutableMap().apply {
                    remove(filmId)
                }
                loadOwnersGettingRid()
                Toast.makeText(context, "Status deleted", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { error ->
                Toast.makeText(context, error.message ?: "Delete failed", Toast.LENGTH_LONG).show()
            }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = if (displayName.isNotBlank()) "Welcome, $displayName" else "Welcome",
                    style = MaterialTheme.typography.headlineMedium
                )
            }

            item { Button(onClick = onOpenProfile) { Text("Profile") } }
            item { Button(onClick = onLogout) { Text("Logout") } }

            item {
                Button(
                    onClick = {
                        showOnlyMyStatuses = !showOnlyMyStatuses
                        if (showOnlyMyStatuses) selectedUniverseId = null
                    }
                ) {
                    Text(if (showOnlyMyStatuses) "Back to universes" else "My statuses")
                }
            }

            if (!showOnlyMyStatuses) {
                item {
                    Text(
                        text = "Universes",
                        style = MaterialTheme.typography.headlineMedium
                    )
                }

                items(universes) { (universeId, universe) ->
                    Card(
                        modifier = Modifier.clickable {
                            selectedUniverseId = universeId
                            showOnlyMyStatuses = false
                        }
                    ) {
                        Text(
                            text = universe.name,
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }

            if (showOnlyMyStatuses || selectedUniverseId != null) {
                item {
                    Text(
                        text = if (showOnlyMyStatuses) "My films" else "Films",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                items(filteredFilms) { (filmId, film) ->
                    val currentStatus = userStatuses[filmId].orEmpty()

                    Card {
                        Text(
                            text = film.title,
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 4.dp)
                        )

                        Text(
                            text = if (currentStatus.isNotBlank()) {
                                "Current status: $currentStatus"
                            } else {
                                "Current status: none"
                            },
                            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp)
                        )

                        Button(
                            onClick = { onOpenFilmDetail(filmId) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                        ) {
                            Text("View details")
                        }

                        Button(
                            onClick = { saveFilmStatus(filmId, "watched") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                        ) {
                            Text("Watched")
                        }

                        Button(
                            onClick = { saveFilmStatus(filmId, "want_to_watch") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                        ) {
                            Text("Want to watch")
                        }

                        Button(
                            onClick = { saveFilmStatus(filmId, "own_dvd") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                        ) {
                            Text("Own DVD")
                        }

                        Button(
                            onClick = { saveFilmStatus(filmId, "want_to_get_rid") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                        ) {
                            Text("Want to get rid")
                        }

                        Button(
                            onClick = { deleteFilmStatus(filmId) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp)
                        ) {
                            Text("Delete my status")
                        }
                    }
                }
            }
        }
    }
}