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
import com.google.firebase.firestore.FirebaseFirestore

class ChangeProfileFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.change_profile, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val etName = view.findViewById<EditText>(R.id.etName)
        val etYourNumber = view.findViewById<EditText>(R.id.etYourNumber)
        val etMonitoredNumber = view.findViewById<EditText>(R.id.etMonitoredNumber)
        val etMonitoredAddress = view.findViewById<EditText>(R.id.etMonitoredAddress)
        val btnSave = view.findViewById<MaterialButton>(R.id.btnSave)

        val uid = FirebaseAuth.getInstance().currentUser?.uid
        val db = FirebaseFirestore.getInstance()

        // Load existing data
        if (uid != null) {
            db.collection("users").document(uid).get()
                .addOnSuccessListener { doc ->
                    etName.setText(doc.getString("name") ?: "")
                    etYourNumber.setText(doc.getString("yourNumber") ?: "")
                    etMonitoredNumber.setText(doc.getString("monitoredNumber") ?: "")
                    etMonitoredAddress.setText(doc.getString("monitoredAddress") ?: "")
                }
        }

        // Save changes
        btnSave.setOnClickListener {
            val name = etName.text.toString().trim()
            val yourNumber = etYourNumber.text.toString().trim()
            val monitoredNumber = etMonitoredNumber.text.toString().trim()
            val monitoredAddress = etMonitoredAddress.text.toString().trim()

            if (name.isEmpty()) { etName.error = "Required"; return@setOnClickListener }
            if (yourNumber.isEmpty()) { etYourNumber.error = "Required"; return@setOnClickListener }
            if (monitoredNumber.isEmpty()) { etMonitoredNumber.error = "Required"; return@setOnClickListener }
            if (monitoredAddress.isEmpty()) { etMonitoredAddress.error = "Required"; return@setOnClickListener }

            btnSave.isEnabled = false

            if (uid != null) {
                db.collection("users").document(uid)
                    .set(mapOf(
                        "name" to name,
                        "yourNumber" to yourNumber,
                        "monitoredNumber" to monitoredNumber,
                        "monitoredAddress" to monitoredAddress
                    ), com.google.firebase.firestore.SetOptions.merge())
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Profile saved!", Toast.LENGTH_SHORT).show()
                        findNavController().popBackStack()
                    }
                    .addOnFailureListener { e ->
                        btnSave.isEnabled = true
                        Toast.makeText(requireContext(), "Failed: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }
        }
    }
}