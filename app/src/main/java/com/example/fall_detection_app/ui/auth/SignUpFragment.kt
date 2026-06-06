package com.example.fall_detection_app.ui.auth

import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.fall_detection_app.R
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SignUpFragment : Fragment() {

    private lateinit var auth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.signup, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()

        val etEmail = view.findViewById<EditText>(R.id.etEmail)
        val etPassword = view.findViewById<EditText>(R.id.etPassword)
        val etYourNumber = view.findViewById<EditText>(R.id.etYourNumber)
        val etMonitoredNumber = view.findViewById<EditText>(R.id.etMonitoredNumber)
        val etMonitoredAddress = view.findViewById<EditText>(R.id.etMonitoredAddress)
        val btnFinish = view.findViewById<MaterialButton>(R.id.btnFinish)
        val tvGoToLogin = view.findViewById<TextView>(R.id.tvGoToLogin)

        val fullText = "Already signed up? Log in"
        val spannable = SpannableString(fullText)
        val pink = ContextCompat.getColor(requireContext(), R.color.accent_pink)
        spannable.setSpan(
            ForegroundColorSpan(pink),
            fullText.indexOf("Log in"),
            fullText.length,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        tvGoToLogin.text = spannable

        tvGoToLogin.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            findNavController().navigate(R.id.action_signUpFragment_to_loginFragment)
        }

        btnFinish.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString()
            val yourNumber = etYourNumber.text.toString().trim()
            val monitoredNumber = etMonitoredNumber.text.toString().trim()
            val monitoredAddress = etMonitoredAddress.text.toString().trim()

            if (email.isEmpty()) { etEmail.error = "Required"; return@setOnClickListener }
            if (password.length < 6) { etPassword.error = "Min 6 characters"; return@setOnClickListener }
            if (yourNumber.isEmpty()) { etYourNumber.error = "Required"; return@setOnClickListener }
            if (monitoredNumber.isEmpty()) { etMonitoredNumber.error = "Required"; return@setOnClickListener }
            if (monitoredAddress.isEmpty()) { etMonitoredAddress.error = "Required"; return@setOnClickListener }

            btnFinish.isEnabled = false

            auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener { result ->
                    val userId = result.user!!.uid
                    val db = FirebaseFirestore.getInstance()
                    db.collection("users").document(userId)
                        .set(mapOf(
                            "yourNumber" to yourNumber,
                            "monitoredNumber" to monitoredNumber,
                            "monitoredAddress" to monitoredAddress
                        ))
                        .addOnSuccessListener {
                            findNavController().navigate(R.id.action_signUpFragment_to_homeFragment)
                        }
                        .addOnFailureListener { e ->
                            btnFinish.isEnabled = true
                            Toast.makeText(requireContext(), "Profile save failed: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                }
                .addOnFailureListener { e ->
                    btnFinish.isEnabled = true
                    Toast.makeText(requireContext(), e.message, Toast.LENGTH_LONG).show()
                }
        }
    }
}