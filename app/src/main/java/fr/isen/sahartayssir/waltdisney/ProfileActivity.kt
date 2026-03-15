package fr.isen.sahartayssir.waltdisney

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import fr.isen.sahartayssir.waltdisney.ui.theme.*

class ProfileActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { WaltdisneyTheme { ProfileScreen { finish() } } }
    }
}

@Composable
fun ProfileScreen(onBack: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var pic by remember { mutableStateOf("") }
    var count by remember { mutableStateOf(0) }

    val databaseUrl = "https://walt-disney-708e2-default-rtdb.europe-west1.firebasedatabase.app"
    val uid = FirebaseAuth.getInstance().currentUser?.uid
    val db = FirebaseDatabase.getInstance(databaseUrl).reference

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { db.child("users").child(uid!!).child("profilePic").setValue(it.toString()).addOnSuccessListener { pic = it.toString() } }
    }

    LaunchedEffect(Unit) {
        uid?.let {
            db.child("users").child(it).get().addOnSuccessListener { snap ->
                name = snap.child("displayName").getValue(String::class.java).orEmpty()
                email = snap.child("email").getValue(String::class.java).orEmpty()
                pic = snap.child("profilePic").getValue(String::class.java).orEmpty()
            }
            db.child("userFilmStatus").child(it).get().addOnSuccessListener { count = it.childrenCount.toInt() }
        }
    }

    Box(Modifier.fillMaxSize().background(MysticalGradient)) {
        Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "←", color = Color.White, fontSize = 24.sp, modifier = Modifier.align(Alignment.Start).clickable { onBack() })
            Spacer(Modifier.height(40.dp))
            Box(contentAlignment = Alignment.BottomEnd) {
                Surface(modifier = Modifier.size(150.dp), shape = CircleShape, border = BorderStroke(4.dp, MagicCyan)) {
                    AsyncImage(model = if (pic.isBlank()) "https://cdn-icons-png.flaticon.com/512/3135/3135715.png" else pic, contentDescription = null, contentScale = ContentScale.Crop)
                }
                FloatingActionButton(onClick = { launcher.launch("image/*") }, containerColor = MagicPink, modifier = Modifier.size(45.dp), shape = CircleShape) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White)
                }
            }
            Spacer(Modifier.height(24.dp))
            Text(name, color = Color.White, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text(email, color = Color.White.copy(alpha = 0.5f))
            Spacer(Modifier.height(40.dp))
            Surface(modifier = Modifier.fillMaxWidth(), color = GlassWhite, shape = RoundedCornerShape(24.dp), border = BorderStroke(1.dp, GlassBorder)) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(count.toString(), fontSize = 60.sp, fontWeight = FontWeight.Black, color = MagicCyan)
                    Text("FILMS DANS MA BIBLIOTHÈQUE ✨", color = Color.White, letterSpacing = 2.sp, fontSize = 12.sp)
                }
            }
        }
    }
}