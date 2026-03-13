package fr.isen.sahartayssir.waltdisney

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import fr.isen.sahartayssir.waltdisney.models.Film
import fr.isen.sahartayssir.waltdisney.models.Universe
import fr.isen.sahartayssir.waltdisney.ui.theme.WaltdisneyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WaltdisneyTheme {
                HomeScreen(
                    onLogout = {
                        FirebaseAuth.getInstance().signOut()
                        startActivity(Intent(this, LoginActivity::class.java))
                        finish()
                    },
                    onOpenProfile = { startActivity(Intent(this, ProfileActivity::class.java)) },
                    onOpenFilmDetail = { filmId ->
                        val intent = Intent(this, FilmDetailActivity::class.java).apply {
                            putExtra("filmId", filmId)
                        }
                        startActivity(intent)
                    }
                )
            }
        }
    }
}

@Composable
fun HomeScreen(onLogout: () -> Unit, onOpenProfile: () -> Unit, onOpenFilmDetail: (String) -> Unit) {
    var displayName by remember { mutableStateOf("") }
    var universes by remember { mutableStateOf(listOf<Pair<String, Universe>>()) }
    var films by remember { mutableStateOf(listOf<Pair<String, Film>>()) }
    var userStatuses by remember { mutableStateOf(mapOf<String, String>()) }
    var selectedUniverseId by remember { mutableStateOf<String?>(null) }
    var showOnlyMyStatuses by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }

    val db = FirebaseDatabase.getInstance("https://walt-disney-708e2-default-rtdb.europe-west1.firebasedatabase.app").reference
    val uid = FirebaseAuth.getInstance().currentUser?.uid

    // ÉCOUTEUR TEMPS RÉEL
    DisposableEffect(Unit) {
        val statusRef = uid?.let { db.child("userFilmStatus").child(it) }
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                userStatuses = snapshot.children.mapNotNull {
                    it.key?.let { k -> k to it.child("status").getValue(String::class.java).orEmpty() }
                }.toMap()
            }
            override fun onCancelled(error: DatabaseError) {}
        }

        statusRef?.addValueEventListener(listener)

        onDispose { statusRef?.removeEventListener(listener) }
    }

    LaunchedEffect(Unit) {
        isLoading = true
        if (uid != null) {
            db.child("users").child(uid).child("displayName").get().addOnSuccessListener {
                displayName = it.getValue(String::class.java).orEmpty()
            }
        }
        db.child("universes").get().addOnSuccessListener { snap ->
            universes = snap.children.mapNotNull { it.key?.let { k -> k to it.getValue(Universe::class.java)!! } }
            db.child("films").get().addOnSuccessListener { fSnap ->
                films = fSnap.children.mapNotNull { it.key?.let { k -> k to it.getValue(Film::class.java)!! } }
                isLoading = false
            }
        }.addOnFailureListener { isLoading = false }
    }

    val filteredFilms = when {
        showOnlyMyStatuses -> films.filter { userStatuses.containsKey(it.first) }
        selectedUniverseId == null -> emptyList()
        else -> films.filter { it.second.universeId == selectedUniverseId }
    }

    Scaffold(containerColor = Color.Black) { padding ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color.White)
            }
        } else {
            Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                Row(Modifier.fillMaxWidth().padding(24.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Column {
                        Text("Bonjour,", color = Color.Gray, fontSize = 14.sp)
                        Text(displayName, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    }
                    Row {
                        IconButton(onClick = onOpenProfile) { Icon(Icons.Default.AccountCircle, null, tint = Color.White) }
                        IconButton(onClick = onLogout) { Icon(Icons.Default.ExitToApp, null, tint = Color.Red) }
                    }
                }

                Row(Modifier.padding(horizontal = 24.dp)) {
                    ModernTab("Catalogue", !showOnlyMyStatuses) { showOnlyMyStatuses = false; selectedUniverseId = null }
                    Spacer(Modifier.width(16.dp))
                    ModernTab("Ma Liste", showOnlyMyStatuses) { showOnlyMyStatuses = true }
                }

                if (!showOnlyMyStatuses) {
                    LazyRow(contentPadding = PaddingValues(24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(universes) { (id, universe) ->
                            UniverseChip(universe.name, selectedUniverseId == id) { selectedUniverseId = id }
                        }
                    }
                } else { Spacer(Modifier.height(24.dp)) }

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(filteredFilms) { (id, film) ->
                        FilmAppleCard(film, userStatuses[id].orEmpty()) { onOpenFilmDetail(id) }
                    }
                }
            }
        }
    }
}

@Composable fun ModernTab(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Column(modifier = Modifier.clickable { onClick() }.padding(vertical = 8.dp)) {
        Text(text, color = if (isSelected) Color.White else Color.Gray, fontWeight = FontWeight.Bold)
        if (isSelected) Box(Modifier.width(20.dp).height(3.dp).background(Color.White))
    }
}

@Composable fun UniverseChip(name: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(20.dp), color = if (isSelected) Color.White else Color(0xFF1C1C1E)) {
        Text(name, color = if (isSelected) Color.Black else Color.White, modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp))
    }
}

@Composable fun FilmAppleCard(film: Film, status: String, onClick: () -> Unit) {
    Surface(onClick = onClick, shape = RoundedCornerShape(16.dp), color = Color(0xFF1C1C1E), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(50.dp).background(Color(0xFF2C2C2E), RoundedCornerShape(8.dp)), contentAlignment = Alignment.Center) {
                Text(film.title.take(1), color = Color.White, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(film.title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                if (status.isNotBlank()) Text(status.replace("_", " "), color = Color.Gray, fontSize = 12.sp)
            }
        }
    }
}