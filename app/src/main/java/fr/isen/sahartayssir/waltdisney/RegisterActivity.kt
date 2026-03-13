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
import fr.isen.sahartayssir.waltdisney.ui.theme.WaltdisneyTheme
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(32.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Inscription", style = MaterialTheme.typography.displayLarge, color = Color.White)
        Spacer(modifier = Modifier.height(40.dp))

        ModernInput(value = displayName, label = "Nom d'utilisateur", onValueChange = { displayName = it })
        Spacer(modifier = Modifier.height(16.dp))
        ModernInput(value = email, label = "Email", onValueChange = { email = it })
        Spacer(modifier = Modifier.height(16.dp))
        ModernInput(value = password, label = "Mot de passe", isPassword = true, onValueChange = { password = it })

        Spacer(modifier = Modifier.height(40.dp))

        Button(
            onClick = {
                if (displayName.length < 2 || !Patterns.EMAIL_ADDRESS.matcher(email).matches() || password.length < 6) {
                    Toast.makeText(context, "Vérifiez vos informations", Toast.LENGTH_SHORT).show()
                    return@Button
                }

                val auth = FirebaseAuth.getInstance()
                val db = FirebaseDatabase.getInstance("https://walt-disney-708e2-default-rtdb.europe-west1.firebasedatabase.app").reference

                auth.createUserWithEmailAndPassword(email, password).addOnSuccessListener {
                    val uid = auth.currentUser?.uid ?: return@addOnSuccessListener
                    val userData = mapOf("email" to email, "displayName" to displayName)
                    db.child("users").child(uid).setValue(userData).addOnSuccessListener { onRegisterSuccess() }
                }.addOnFailureListener { Toast.makeText(context, it.message, Toast.LENGTH_LONG).show() }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White)
        ) {
            Text("Créer le compte", color = Color.Black, fontWeight = FontWeight.Bold)
        }

        TextButton(onClick = onGoToLogin, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text("Déjà inscrit ? Connexion", color = Color.Gray)
        }
    }
}

@Composable
fun ModernInput(value: String, label: String, isPassword: Boolean = false, onValueChange: (String) -> Unit) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = Color.Gray) },
        visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        modifier = Modifier.fillMaxWidth(),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color(0xFF1C1C1E),
            unfocusedContainerColor = Color(0xFF1C1C1E),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        ),
        shape = RoundedCornerShape(12.dp)
    )
}