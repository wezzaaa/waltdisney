package fr.isen.sahartayssir.waltdisney

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.database.FirebaseDatabase
import fr.isen.sahartayssir.waltdisney.models.Universe

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            UniverseScreen()
        }
    }
}

@Composable
fun UniverseScreen() {
    var universes by remember { mutableStateOf(listOf<Pair<String, Universe>>()) }

    LaunchedEffect(Unit) {
        val db = FirebaseDatabase
            .getInstance("https://walt-disney-708e2-default-rtdb.europe-west1.firebasedatabase.app")
            .reference

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
                Log.d("FIREBASE_TEST", "Universes loaded: ${tempList.size}")
            }
            .addOnFailureListener { error ->
                Log.e("FIREBASE_TEST", "Read failed", error)
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
                    text = "Universes",
                    style = MaterialTheme.typography.headlineMedium
                )
            }

            items(universes) { (_, universe) ->
                Card {
                    Text(
                        text = universe.name,
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}