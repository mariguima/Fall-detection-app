package com.example.fall_detection_app.ui.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.fall_detection_app.R
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth

class LoginFragment : Fragment() {

    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.login, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()

        val etEmail = view.findViewById<EditText>(R.id.etEmail)
        val etPassword = view.findViewById<EditText>(R.id.etPassword)
        val btnGetStarted = view.findViewById<MaterialButton>(R.id.btnGetStarted)

        btnGetStarted.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString()

            if (email.isEmpty()) { etEmail.error = "Required"; return@setOnClickListener }
            if (password.isEmpty()) { etPassword.error = "Required"; return@setOnClickListener }

            btnGetStarted.isEnabled = false  // prevent double-tap

            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener {
                    // Login worked — go to home screen
                    findNavController().navigate(R.id.action_loginFragment_to_homeFragment)
                }
                .addOnFailureListener { e ->
                    btnGetStarted.isEnabled = true
                    Toast.makeText(requireContext(), e.message, Toast.LENGTH_LONG).show()
                }
        }
    }
}