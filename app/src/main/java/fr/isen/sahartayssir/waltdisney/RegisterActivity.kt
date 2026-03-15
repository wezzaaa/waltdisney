package fr.isen.sahartayssir.waltdisney

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import fr.isen.sahartayssir.waltdisney.ui.theme.*
import androidx.compose.ui.text.input.PasswordVisualTransformation

class RegisterActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WaltdisneyTheme {
                RegisterScreen(
                    onRegisterSuccess = {
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    },
                    onGoToLogin = {
                        startActivity(Intent(this, LoginActivity::class.java))
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
fun RegisterScreen(onRegisterSuccess: () -> Unit, onGoToLogin: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var displayName by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MysticalGradient)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "Inscription",
                style = MaterialTheme.typography.displayLarge,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                "Rejoignez la communauté Disney ✨",
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.padding(bottom = 32.dp)
            )

            MagicInput(value = displayName, label = "Nom d'utilisateur") { displayName = it }
            Spacer(modifier = Modifier.height(16.dp))
            MagicInput(value = email, label = "Email") { email = it }
            Spacer(modifier = Modifier.height(16.dp))
            MagicInput(value = password, label = "Mot de passe", isPassword = true) { password = it }

            Spacer(modifier = Modifier.height(40.dp))

            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    color = MagicPink
                )
            } else {
                Button(
                    onClick = {
                        if (displayName.length < 2 || !Patterns.EMAIL_ADDRESS.matcher(email).matches() || password.length < 6) {
                            Toast.makeText(context, "Données invalides (MDP: 6 caractères min.)", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        isLoading = true
                        val auth = FirebaseAuth.getInstance()
                        val databaseUrl = "https://walt-disney-708e2-default-rtdb.europe-west1.firebasedatabase.app"
                        val db = FirebaseDatabase.getInstance(databaseUrl).reference

                        auth.createUserWithEmailAndPassword(email, password).addOnSuccessListener {
                            val uid = auth.currentUser?.uid ?: return@addOnSuccessListener
                            val userData = mapOf(
                                "email" to email,
                                "displayName" to displayName,
                                "profilePic" to "" // Initialement vide
                            )
                            db.child("users").child(uid).setValue(userData).addOnSuccessListener {
                                isLoading = false
                                onRegisterSuccess()
                            }
                        }.addOnFailureListener {
                            isLoading = false
                            Toast.makeText(context, it.message, Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MagicPink,
                        contentColor = Color.White
                    )
                ) {
                    Text("Créer mon compte", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }

            TextButton(
                onClick = onGoToLogin,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp)
            ) {
                Text("Déjà un compte ? Connectez-vous", color = Color.White.copy(alpha = 0.6f))
            }
        }
    }
}