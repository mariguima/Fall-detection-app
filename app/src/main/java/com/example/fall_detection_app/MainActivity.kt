package com.example.fall_detection_app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onResume() {
        super.onResume()
        val auth = FirebaseAuth.getInstance()
        val navController = findNavController(R.id.nav_host_fragment)

        if (auth.currentUser != null) {
            // user is logged in — go to home skipping onboarding
            if (navController.currentDestination?.id == R.id.splashFragment) {
                navController.navigate(
                    R.id.homeFragment, null,
                    androidx.navigation.NavOptions.Builder()
                        .setPopUpTo(R.id.nav_graph, true)
                        .build()
                )
            }
        } else {
            // no user — go to splash screen
            if (navController.currentDestination?.id != R.id.splashFragment) {
                navController.navigate(
                    R.id.splashFragment, null,
                    androidx.navigation.NavOptions.Builder()
                        .setPopUpTo(R.id.nav_graph, true)
                        .build()
                )
            }
        }
    }
}