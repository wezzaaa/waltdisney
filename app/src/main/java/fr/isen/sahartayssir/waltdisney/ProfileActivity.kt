package fr.isen.sahartayssir.waltdisney

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class ProfileActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ProfileScreen()
        }
    }
}

@Composable
fun ProfileScreen() {
    var displayName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var markedFilmsCount by remember { mutableStateOf(0) }

    val auth = FirebaseAuth.getInstance()
    val uid = auth.currentUser?.uid

    val db = FirebaseDatabase
        .getInstance("https://walt-disney-708e2-default-rtdb.europe-west1.firebasedatabase.app")
        .reference

    LaunchedEffect(Unit) {
        if (uid == null) return@LaunchedEffect

        db.child("users").child(uid).child("displayName").get()
            .addOnSuccessListener { snapshot ->
                displayName = snapshot.getValue(String::class.java).orEmpty()
            }
            .addOnFailureListener { error ->
                Log.e("FIREBASE_TEST", "Profile displayName read failed", error)
            }

        db.child("users").child(uid).child("email").get()
            .addOnSuccessListener { snapshot ->
                email = snapshot.getValue(String::class.java).orEmpty()
            }
            .addOnFailureListener { error ->
                Log.e("FIREBASE_TEST", "Profile email read failed", error)
            }

        db.child("userFilmStatus").child(uid).get()
            .addOnSuccessListener { snapshot ->
                markedFilmsCount = snapshot.childrenCount.toInt()
            }
            .addOnFailureListener { error ->
                Log.e("FIREBASE_TEST", "Profile statuses read failed", error)
            }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Profile",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = if (displayName.isNotBlank()) "Display name: $displayName" else "Display name: -",
            style = MaterialTheme.typography.bodyLarge
        )

        Text(
            text = if (email.isNotBlank()) "Email: $email" else "Email: -",
            style = MaterialTheme.typography.bodyLarge
        )

        Text(
            text = "Marked films: $markedFilmsCount",
            style = MaterialTheme.typography.bodyLarge
        )
    }
}