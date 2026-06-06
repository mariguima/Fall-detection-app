package com.example.fall_detection_app.ui.permissions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.fall_detection_app.R
import com.google.android.material.button.MaterialButton
import android.util.Log

class PermissionsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.permissions, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btn = view.findViewById<MaterialButton>(R.id.btnContinue)
        Log.d("PERMISSIONS", "Button found: $btn")

        btn.setOnClickListener {
            Log.d("PERMISSIONS", "Button clicked!")
            findNavController().navigate(R.id.action_permissions_to_signUp)
        }
    }
}