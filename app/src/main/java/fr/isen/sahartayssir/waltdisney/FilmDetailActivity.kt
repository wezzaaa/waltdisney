package fr.isen.sahartayssir.waltdisney

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import fr.isen.sahartayssir.waltdisney.models.Film
import fr.isen.sahartayssir.waltdisney.ui.theme.WaltdisneyTheme

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
    val context = LocalContext.current
    var film by remember { mutableStateOf(Film()) }
    var currentStatus by remember { mutableStateOf("") }
    var owners by remember { mutableStateOf(listOf<String>()) }
    var isLoading by remember { mutableStateOf(true) }

    val uid = FirebaseAuth.getInstance().currentUser?.uid
    val db = FirebaseDatabase.getInstance("https://walt-disney-708e2-default-rtdb.europe-west1.firebasedatabase.app").reference

    // Fonction pour charger toutes les infos (Film + Mon Statut + Liste des Propriétaires)
    fun loadDetails() {
        isLoading = true
        db.child("films").child(filmId).get().addOnSuccessListener { filmSnap ->
            film = filmSnap.getValue(Film::class.java) ?: Film()

            if (uid != null) {
                // 1. Charger mon statut
                db.child("userFilmStatus").child(uid).child(filmId).child("status").get().addOnSuccessListener { statusSnap ->
                    currentStatus = statusSnap.getValue(String::class.java).orEmpty()

                    // 2. Charger la liste des gens qui veulent s'en débarrasser
                    db.child("ownersGettingRid").child(filmId).get().addOnSuccessListener { ownerSnap ->
                        val uids = ownerSnap.children.mapNotNull { it.key }
                        if (uids.isEmpty()) {
                            owners = emptyList()
                            isLoading = false
                        } else {
                            val names = mutableListOf<String>()
                            var count = 0
                            uids.forEach { ownerUid ->
                                db.child("users").child(ownerUid).child("displayName").get().addOnSuccessListener { nameSnap ->
                                    val name = nameSnap.getValue(String::class.java) ?: "Anonyme"
                                    // On ajoute le nom (on peut filtrer "soi-même" si on veut)
                                    names.add(if(ownerUid == uid) "$name (Moi)" else name)

                                    count++
                                    if (count == uids.size) {
                                        owners = names
                                        isLoading = false
                                    }
                                }.addOnFailureListener {
                                    count++
                                    if (count == uids.size) { isLoading = false }
                                }
                            }
                        }
                    }.addOnFailureListener { isLoading = false }
                }.addOnFailureListener { isLoading = false }
            } else {
                isLoading = false
            }
        }.addOnFailureListener { isLoading = false }
    }

    LaunchedEffect(filmId) { loadDetails() }

    // Fonction pour sauvegarder le statut (avec gestion spéciale pour "Se débarrasser")
    fun saveStatus(status: String) {
        if (uid == null) return
        isLoading = true

        val updates = mutableMapOf<String, Any?>()
        updates["userFilmStatus/$uid/$filmId/status"] = status

        // Si le statut est "want_to_get_rid", on s'ajoute à la liste publique
        if (status == "want_to_get_rid") {
            updates["ownersGettingRid/$filmId/$uid"] = true
        } else {
            // Sinon, on s'enlève de la liste publique
            updates["ownersGettingRid/$filmId/$uid"] = null
        }

        db.updateChildren(updates).addOnSuccessListener {
            currentStatus = status
            loadDetails() // On recharge tout pour voir la liste mise à jour
            Toast.makeText(context, "Statut mis à jour", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener {
            isLoading = false
            Toast.makeText(context, "Erreur de sauvegarde", Toast.LENGTH_SHORT).show()
        }
    }

    fun deleteStatus() {
        if (uid == null) return
        isLoading = true
        val updates = mutableMapOf<String, Any?>()
        updates["userFilmStatus/$uid/$filmId"] = null
        updates["ownersGettingRid/$filmId/$uid"] = null

        db.updateChildren(updates).addOnSuccessListener {
            onBack() // Retour direct
        }.addOnFailureListener {
            isLoading = false
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        if (isLoading) {
            CircularProgressIndicator(Modifier.align(Alignment.Center), color = Color.White)
        } else {
            Column(Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState())) {
                Text("←", color = Color.White, fontSize = 30.sp, modifier = Modifier.clickable { onBack() })
                Spacer(Modifier.height(30.dp))

                Text(film.title, color = Color.White, style = MaterialTheme.typography.displayLarge)
                Text(film.category.uppercase(), color = Color.Gray, style = MaterialTheme.typography.labelMedium)

                Spacer(Modifier.height(32.dp))

                // Boutons de statut
                StatusButton("Déjà vu", currentStatus == "watched") { saveStatus("watched") }
                StatusButton("Envie de voir", currentStatus == "want_to_watch") { saveStatus("want_to_watch") }
                StatusButton("Possédé (DVD)", currentStatus == "own_dvd") { saveStatus("own_dvd") }
                StatusButton("Se débarrasser", currentStatus == "want_to_get_rid") { saveStatus("want_to_get_rid") }

                // AFFICHAGE DES PROPRIÉTAIRES (CONSIGNE PROJET)
                if (owners.isNotEmpty()) {
                    Spacer(Modifier.height(32.dp))
                    Surface(
                        color = Color(0xFF1C1C1E),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Disponible chez :", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Spacer(Modifier.height(8.dp))
                            Text(owners.joinToString(", "), color = Color.Gray, fontSize = 14.sp)
                        }
                    }
                }

                Spacer(Modifier.height(48.dp))
                TextButton(onClick = { deleteStatus() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Retirer de ma liste", color = Color.Red, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun StatusButton(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) Color.White else Color(0xFF1C1C1E)
    ) {
        Box(Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
            Text(text, color = if (isSelected) Color.Black else Color.White, fontWeight = FontWeight.SemiBold)
        }
    }
}