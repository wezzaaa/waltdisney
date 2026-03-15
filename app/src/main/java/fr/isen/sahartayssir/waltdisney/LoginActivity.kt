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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import fr.isen.sahartayssir.waltdisney.ui.theme.*

class LoginActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (FirebaseAuth.getInstance().currentUser != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }
        setContent { WaltdisneyTheme { LoginScreen(
            onLoginSuccess = { startActivity(Intent(this, MainActivity::class.java)); finish() },
            onGoToRegister = { startActivity(Intent(this, RegisterActivity::class.java)) }
        )}}
    }
}

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit, onGoToRegister: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize().background(MysticalGradient)) {
        Column(modifier = Modifier.fillMaxSize().padding(32.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.Start) {
            Text(text = "Connexion", style = MaterialTheme.typography.displayLarge, color = Color.White, fontWeight = FontWeight.ExtraBold)
            Text(text = "La magie n'attend que vous ✨", color = Color.White.copy(alpha = 0.7f), fontSize = 16.sp, modifier = Modifier.padding(bottom = 40.dp))
            MagicInput(value = email, label = "Email") { email = it }
            Spacer(modifier = Modifier.height(16.dp))
            MagicInput(value = password, label = "Mot de passe", isPassword = true) { password = it }
            Spacer(modifier = Modifier.height(40.dp))
            if (isLoading) { CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally), color = MagicCyan) }
            else {
                Button(onClick = {
                    if (email.isBlank() || password.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                        Toast.makeText(context, "Vérifiez vos données", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    isLoading = true
                    FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password).addOnSuccessListener { onLoginSuccess() }.addOnFailureListener { isLoading = false; Toast.makeText(context, it.message, Toast.LENGTH_LONG).show() }
                }, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = MagicDeepPurple)) {
                    Text("Entrer dans le Royaume", fontWeight = FontWeight.Bold)
                }
            }
            TextButton(onClick = onGoToRegister, modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 16.dp)) {
                Text("Nouveau ? Créer un profil magique", color = Color.White.copy(alpha = 0.8f))
            }
        }
    }
}

@Composable
fun MagicInput(value: String, label: String, isPassword: Boolean = false, onValueChange: (String) -> Unit) {
    TextField(
        value = value, onValueChange = onValueChange, label = { Text(label, color = Color.White.copy(alpha = 0.6f)) },
        visualTransformation = if (isPassword) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        modifier = Modifier.fillMaxWidth(),
        colors = TextFieldDefaults.colors(focusedContainerColor = GlassWhite, unfocusedContainerColor = GlassWhite, focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedIndicatorColor = MagicCyan, unfocusedIndicatorColor = Color.Transparent),
        shape = RoundedCornerShape(16.dp)
    )
}