package fr.isen.sahartayssir.waltdisney

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import fr.isen.sahartayssir.waltdisney.models.Film
import fr.isen.sahartayssir.waltdisney.models.Universe
import fr.isen.sahartayssir.waltdisney.ui.theme.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val app = com.google.firebase.FirebaseApp.getInstance()
        val databaseUrl = "https://walt-disney-708e2-default-rtdb.europe-west1.firebasedatabase.app"

        Log.d("HOME_DEBUG", "MainActivity onCreate")
        Log.d("HOME_DEBUG", "currentUser = ${FirebaseAuth.getInstance().currentUser?.uid}")
        Log.d("HOME_DEBUG", "projectId = ${app.options.projectId}")
        Log.d("HOME_DEBUG", "applicationId = ${app.options.applicationId}")
        Log.d("HOME_DEBUG", "databaseUrl = ${app.options.databaseUrl}")

        val dbTest = FirebaseDatabase.getInstance(databaseUrl).reference

        dbTest.child(".info").child("connected")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    Log.d("HOME_DEBUG", ".info/connected EXPLICIT = ${snapshot.getValue(Boolean::class.java)}")
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("HOME_DEBUG", ".info/connected EXPLICIT cancelled: ${error.message}", error.toException())
                }
            })

        dbTest.child("testConnection").setValue("ok")
            .addOnSuccessListener {
                Log.d("HOME_DEBUG", "Écriture test EXPLICIT OK")
            }
            .addOnFailureListener {
                Log.e("HOME_DEBUG", "Écriture test EXPLICIT KO: ${it.message}", it)
            }

        setContent {
            WaltdisneyTheme {
                HomeScreen(
                    onLogout = {
                        FirebaseAuth.getInstance().signOut()
                        startActivity(Intent(this, LoginActivity::class.java))
                        finish()
                    },
                    onOpenProfile = { startActivity(Intent(this, ProfileActivity::class.java)) },
                    onOpenFilmDetail = { id ->
                        startActivity(Intent(this, FilmDetailActivity::class.java).apply {
                            putExtra("filmId", id)
                        })
                    }
                )
            }
        }
    }
}

@Composable
fun HomeScreen(
    onLogout: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenFilmDetail: (String) -> Unit
) {
    var displayName by remember { mutableStateOf("") }
    var profilePic by remember { mutableStateOf("") }
    var universes by remember { mutableStateOf(listOf<Pair<String, Universe>>()) }
    var films by remember { mutableStateOf(listOf<Pair<String, Film>>()) }
    var userStatuses by remember { mutableStateOf(mapOf<String, String>()) }
    var selectedUniverseId by remember { mutableStateOf<String?>(null) }
    var showOnlyMyStatuses by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    val databaseUrl = "https://walt-disney-708e2-default-rtdb.europe-west1.firebasedatabase.app"
    val db = FirebaseDatabase.getInstance(databaseUrl).reference
    val uid = FirebaseAuth.getInstance().currentUser?.uid

    DisposableEffect(Unit) {
        val ref = uid?.let { db.child("userFilmStatus").child(it) }

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                userStatuses = snapshot.children.associate { child ->
                    child.key!! to child.child("status").getValue(String::class.java).orEmpty()
                }
                Log.d("HOME_DEBUG", "userStatuses chargés = ${userStatuses.size}")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("HOME_DEBUG", "userStatuses cancelled: ${error.message}", error.toException())
            }
        }

        ref?.addValueEventListener(listener)

        onDispose {
            ref?.removeEventListener(listener)
        }
    }

    LaunchedEffect(Unit) {
        isLoading = true

        Log.d("HOME_DEBUG", "====================")
        Log.d("HOME_DEBUG", "HomeScreen lancé")
        Log.d("HOME_DEBUG", "uid = $uid")

        if (uid == null) {
            Log.e("HOME_DEBUG", "uid null")
            isLoading = false
            return@LaunchedEffect
        }

        val userDisplayNameRef = db.child("users").child(uid).child("displayName")
        Log.d("HOME_DEBUG", "Lecture displayName demarree")

        userDisplayNameRef.get()
            .addOnCompleteListener { task ->
                Log.d("HOME_DEBUG", "displayName onComplete")
                Log.d("HOME_DEBUG", "displayName isComplete = ${task.isComplete}")
                Log.d("HOME_DEBUG", "displayName isSuccessful = ${task.isSuccessful}")
                Log.d("HOME_DEBUG", "displayName exception = ${task.exception?.message}")

                if (!task.isSuccessful) {
                    Log.e("HOME_DEBUG", "Échec displayName", task.exception)
                    isLoading = false
                    return@addOnCompleteListener
                }

                val snap = task.result
                Log.d("HOME_DEBUG", "displayName exists = ${snap.exists()}")
                Log.d("HOME_DEBUG", "displayName value = ${snap.getValue(String::class.java)}")

                displayName = snap.getValue(String::class.java).orEmpty()

                val profilePicRef = db.child("users").child(uid).child("profilePic")
                Log.d("HOME_DEBUG", "Lecture profilePic demarrer")

                profilePicRef.get()
                    .addOnCompleteListener { picTask ->
                        Log.d("HOME_DEBUG", "profilePic onComplete")
                        Log.d("HOME_DEBUG", "profilePic isSuccessful = ${picTask.isSuccessful}")
                        Log.d("HOME_DEBUG", "profilePic exception = ${picTask.exception?.message}")

                        if (!picTask.isSuccessful) {
                            Log.e("HOME_DEBUG", "Échec profilePic", picTask.exception)
                            isLoading = false
                            return@addOnCompleteListener
                        }

                        val picSnap = picTask.result
                        Log.d("HOME_DEBUG", "profilePic exists = ${picSnap.exists()}")
                        Log.d("HOME_DEBUG", "profilePic value = ${picSnap.getValue(String::class.java)}")

                        profilePic = picSnap.getValue(String::class.java).orEmpty()

                        val universesRef = db.child("universes")
                        Log.d("HOME_DEBUG", "Lecture universes démarrée")

                        universesRef.get()
                            .addOnCompleteListener { uniTask ->
                                Log.d("HOME_DEBUG", "universes onComplete")
                                Log.d("HOME_DEBUG", "universes isSuccessful = ${uniTask.isSuccessful}")
                                Log.d("HOME_DEBUG", "universes exception = ${uniTask.exception?.message}")

                                if (!uniTask.isSuccessful) {
                                    Log.e("HOME_DEBUG", "Échec universes", uniTask.exception)
                                    isLoading = false
                                    return@addOnCompleteListener
                                }

                                val uniSnap = uniTask.result
                                Log.d("HOME_DEBUG", "universes childrenCount = ${uniSnap.childrenCount}")

                                try {
                                    universes = uniSnap.children.mapNotNull { child ->
                                        val key = child.key
                                        Log.d("HOME_DEBUG", "Universe child key = $key")

                                        if (key == null) return@mapNotNull null

                                        val universe = child.getValue(Universe::class.java)
                                        Log.d("HOME_DEBUG", "Universe parsed = $universe")

                                        if (universe == null) null else key to universe
                                    }

                                    Log.d("HOME_DEBUG", "universes parsés = ${universes.size}")
                                } catch (e: Exception) {
                                    Log.e("HOME_DEBUG", "Exception parsing universes", e)
                                    isLoading = false
                                    return@addOnCompleteListener
                                }

                                val filmsRef = db.child("films")
                                Log.d("HOME_DEBUG", "Lecture films demarrer")

                                filmsRef.get()
                                    .addOnCompleteListener { filmTask ->
                                        Log.d("HOME_DEBUG", "films onComplete")
                                        Log.d("HOME_DEBUG", "films isSuccessful = ${filmTask.isSuccessful}")
                                        Log.d("HOME_DEBUG", "films exception = ${filmTask.exception?.message}")

                                        if (!filmTask.isSuccessful) {
                                            Log.e("HOME_DEBUG", "Échec films", filmTask.exception)
                                            isLoading = false
                                            return@addOnCompleteListener
                                        }

                                        val filmSnap = filmTask.result
                                        Log.d("HOME_DEBUG", "films childrenCount = ${filmSnap.childrenCount}")

                                        try {
                                            films = filmSnap.children.mapNotNull { child ->
                                                val key = child.key
                                                Log.d("HOME_DEBUG", "Film child key = $key")

                                                if (key == null) return@mapNotNull null

                                                val film = child.getValue(Film::class.java)
                                                Log.d("HOME_DEBUG", "Film parsed = $film")

                                                if (film == null) null else key to film
                                            }

                                            Log.d("HOME_DEBUG", "films parsés = ${films.size}")
                                            Log.d("HOME_DEBUG", "Chargement terminé OK")
                                            isLoading = false
                                        } catch (e: Exception) {
                                            Log.e("HOME_DEBUG", "Exception parsing films", e)
                                            isLoading = false
                                        }
                                    }
                            }
                    }
            }
    }

    val filtered = films.filter {
        when {
            showOnlyMyStatuses -> userStatuses.containsKey(it.first)
            selectedUniverseId != null -> it.second.universeId == selectedUniverseId
            else -> true
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(MysticalGradient)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = MagicCyan
            )
        } else {
            Column(Modifier.fillMaxSize()) {
                HeaderMagic(displayName, profilePic, onOpenProfile, onLogout)

                Row(Modifier.padding(horizontal = 24.dp)) {
                    MagicTab("Catalogue", !showOnlyMyStatuses) {
                        showOnlyMyStatuses = false
                        selectedUniverseId = null
                    }

                    Spacer(Modifier.width(16.dp))

                    MagicTab("Ma Liste ✨", showOnlyMyStatuses) {
                        showOnlyMyStatuses = true
                    }
                }

                if (!showOnlyMyStatuses) {
                    LazyRow(
                        contentPadding = PaddingValues(24.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(universes) { (id, universe) ->
                            MagicChip(universe.name, selectedUniverseId == id) {
                                selectedUniverseId = id
                            }
                        }
                    }
                } else {
                    Spacer(Modifier.height(24.dp))
                }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(filtered) { (id, film) ->
                        FilmMagicCard(film, userStatuses[id].orEmpty()) {
                            onOpenFilmDetail(id)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HeaderMagic(name: String, pic: String, onProf: () -> Unit, onLog: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                ".",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 14.sp
            )
            Text(
                name.ifBlank { "Invité" },
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                onClick = onProf,
                shape = CircleShape,
                modifier = Modifier.size(50.dp),
                border = BorderStroke(2.dp, MagicCyan)
            ) {
                AsyncImage(
                    model = pic.ifBlank { "https://cdn-icons-png.flaticon.com/512/3135/3135715.png" },
                    contentDescription = null,
                    contentScale = ContentScale.Crop
                )
            }

            IconButton(onClick = onLog) {
                Icon(Icons.Default.ExitToApp, contentDescription = null, tint = MagicPink)
            }
        }
    }
}

@Composable
fun MagicTab(text: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        Modifier
            .clickable { onClick() }
            .padding(vertical = 8.dp)
    ) {
        Text(
            text,
            color = if (selected) Color.White else Color.White.copy(alpha = 0.4f),
            fontWeight = FontWeight.Bold
        )

        if (selected) {
            Box(
                Modifier
                    .width(20.dp)
                    .height(3.dp)
                    .background(MagicCyan, CircleShape)
            )
        }
    }
}

@Composable
fun MagicChip(name: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (selected) Color.White else GlassWhite
    ) {
        Text(
            name,
            color = if (selected) MagicDeepPurple else Color.White,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
        )
    }
}

@Composable
fun FilmMagicCard(film: Film, status: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(24.dp),
        color = GlassWhite,
        border = BorderStroke(1.dp, GlassBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(80.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                AsyncImage(
                    model = film.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(Modifier.width(16.dp))

            Column {
                Text(
                    film.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )

                if (status.isNotBlank()) {
                    Text(
                        status.uppercase().replace("_", " "),
                        color = MagicCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}