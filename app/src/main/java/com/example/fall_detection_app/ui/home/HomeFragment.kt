package com.example.fall_detection_app.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.fall_detection_app.R
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class HomeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.home, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val drawerLayout = view.findViewById<DrawerLayout>(R.id.drawerLayout)
        val tvHello = view.findViewById<TextView>(R.id.tvHello)

        // open drawer when hamburger is clicked
        view.findViewById<View>(R.id.btnMenu).setOnClickListener {
            drawerLayout.openDrawer(view.findViewById(R.id.navDrawer))
        }

        // load user name from Firestore
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid != null) {
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener { doc ->
                    val name = doc.getString("name") ?: "User"
                    tvHello.text = "Hello,\n$name"
                }
        }

        // change profile
        view.findViewById<TextView>(R.id.tvChangeProfile).setOnClickListener {
            findNavController().navigate(R.id.action_home_to_changeProfile)
        }

        //change password
        view.findViewById<TextView>(R.id.tvChangePassword).setOnClickListener {
            findNavController().navigate(R.id.action_home_to_changePassword)
        }

        //connect to device
        view.findViewById<TextView>(R.id.tvConnectDevice).setOnClickListener {
            findNavController().navigate(R.id.action_home_to_espConnect)
        }

        // disconnect button in drawer
        view.findViewById<MaterialButton>(R.id.btnLogout).setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            findNavController().navigate(
                R.id.loginFragment, null,
                androidx.navigation.NavOptions.Builder()
                    .setPopUpTo(R.id.nav_graph, true)
                    .build()
            )
        }
    }
}