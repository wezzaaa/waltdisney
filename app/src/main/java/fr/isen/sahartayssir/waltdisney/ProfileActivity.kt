package fr.isen.sahartayssir.waltdisney

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import fr.isen.sahartayssir.waltdisney.ui.theme.WaltdisneyTheme
import androidx.compose.foundation.clickable

class ProfileActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { WaltdisneyTheme { ProfileScreen { finish() } } }
    }
}

@Composable
fun ProfileScreen(onBack: () -> Unit) {
    var displayName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var count by remember { mutableStateOf(0) }

    val uid = FirebaseAuth.getInstance().currentUser?.uid
    val db = FirebaseDatabase.getInstance("https://walt-disney-708e2-default-rtdb.europe-west1.firebasedatabase.app").reference

    LaunchedEffect(Unit) {
        uid?.let {
            db.child("users").child(it).get().addOnSuccessListener { snap ->
                displayName = snap.child("displayName").getValue(String::class.java).orEmpty()
                email = snap.child("email").getValue(String::class.java).orEmpty()
            }
            db.child("userFilmStatus").child(it).get().addOnSuccessListener { snap ->
                count = snap.childrenCount.toInt()
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Color.Black).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
            Text("←", color = Color.White, fontSize = 24.sp, modifier = Modifier.padding(16.dp).clickable { onBack() })
        }

        // Avatar Placeholder
        Box(
            Modifier.size(100.dp).background(Color(0xFF1C1C1E), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(displayName.take(1).uppercase(), color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(displayName, style = MaterialTheme.typography.headlineMedium, color = Color.White)
        Text(email, color = Color.Gray, fontSize = 14.sp)

        Spacer(modifier = Modifier.height(40.dp))

        // Stats Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF1C1C1E),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(count.toString(), fontSize = 48.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                Text("FILMS MARQUÉS", color = Color.Gray, letterSpacing = 2.sp, fontSize = 12.sp)
            }
        }
    }
}