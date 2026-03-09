package fr.isen.sahartayssir.waltdisney

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import com.google.firebase.database.FirebaseDatabase
import fr.isen.sahartayssir.waltdisney.models.Universe

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = FirebaseDatabase
            .getInstance("https://walt-disney-708e2-default-rtdb.europe-west1.firebasedatabase.app")
            .reference

        db.child("universes").get()
            .addOnSuccessListener { snapshot ->
                Log.d("FIREBASE_TEST", "Read success")

                for (child in snapshot.children) {
                    val universe = child.getValue(Universe::class.java)
                    Log.d("FIREBASE_TEST", "key=${child.key}, name=${universe?.name}")
                }
            }
            .addOnFailureListener { error ->
                Log.e("FIREBASE_TEST", "Read failed", error)
            }
    }
}