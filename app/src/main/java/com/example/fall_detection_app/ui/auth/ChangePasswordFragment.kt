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

class ChangePasswordFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.change_password, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val etEmail = view.findViewById<EditText>(R.id.etEmail)
        val btnSave = view.findViewById<MaterialButton>(R.id.btnSave)

        val auth = FirebaseAuth.getInstance()

        btnSave.setOnClickListener {
            val email = etEmail.text.toString().trim()

            if (email.isEmpty()) {
                etEmail.error = "Required"
                return@setOnClickListener
            }

            btnSave.isEnabled = false
            btnSave.text = "Sending..."

            auth.sendPasswordResetEmail(email)
                .addOnSuccessListener {
                    Toast.makeText(
                        requireContext(),
                        "Password reset email sent.",
                        Toast.LENGTH_LONG
                    ).show()
                    findNavController().popBackStack()
                }
                .addOnFailureListener { e ->
                    btnSave.isEnabled = true
                    btnSave.text = "Send reset email"

                    Toast.makeText(
                        requireContext(),
                        e.message ?: "Failed to send reset email.",
                        Toast.LENGTH_LONG
                    ).show()
                }
        }

    }
}
