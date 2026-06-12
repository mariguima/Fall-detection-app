package com.example.fall_detection_app.ui.setup

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

class WifiConnectionFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.wifi_connection, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val etNetworkName = view.findViewById<EditText>(R.id.etNetworkName)
        val etPassword = view.findViewById<EditText>(R.id.etPassword)
        val btnConnect = view.findViewById<MaterialButton>(R.id.btnConnect)

        btnConnect.setOnClickListener {
            val network = etNetworkName.text.toString().trim()
            val password = etPassword.text.toString()

            if (network.isEmpty()) {
                etNetworkName.error = "Please enter network name"
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                etPassword.error = "Please enter password"
                return@setOnClickListener
            }

            // TODO: send wifi credentials to device
            Toast.makeText(requireContext(), "Connecting...", Toast.LENGTH_SHORT).show()
            findNavController().navigate(R.id.action_wifiConnection_to_home)
        }
    }
}