package com.example.last

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.last.databinding.ActivityMainBinding
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set up Firebase Storage for another project
            //initializeStorageFirebase()
        //setupTFLiteModel()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.donorCard.setOnClickListener {
            val intent = Intent(this, Donorlogin::class.java)
            startActivity(intent)
        }

        binding.recipientCard.setOnClickListener {
            val intent = Intent(this, Recipientlogin::class.java)
            startActivity(intent)
        }

        binding.hospitalCard.setOnClickListener {
            val intent = Intent(this, Hospitallogin::class.java)
            startActivity(intent)
        }
    }

    // Initialize Firebase for the storage project
//    private fun initializeStorageFirebase() {
//        val storageOptions = FirebaseOptions.Builder()
//            .setProjectId("socialmedia-b9148") // Replace with your Firebase Storage project's ID
//            .setApplicationId("1:924036427672:android:75e24d5ebe6dd3f35cc5ed") // Replace with your Firebase app's application ID
//            .setApiKey("AIzaSyBLVNr5M0sHOTtGpqBvn8ula-knHx0vxvc") // Replace with your Firebase app's API key
//            .setStorageBucket("socialmedia-b9148.appspot.com") // Replace with your Firebase Storage bucket URL
//            .build()
//
//        // Initialize Firebase with these options, giving it a different name
//        FirebaseApp.initializeApp(this, storageOptions, "storageApp")
//    }
}

