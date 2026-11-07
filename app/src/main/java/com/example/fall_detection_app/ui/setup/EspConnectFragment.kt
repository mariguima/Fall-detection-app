package com.example.fall_detection_app.ui.setup

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.fall_detection_app.AppConfig
import com.example.fall_detection_app.R
import com.example.fall_detection_app.databinding.EspConnectBinding
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.resume

class EspConnectFragment : Fragment() {

    private var _binding: EspConnectBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = EspConnectBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // freeze button while checking
        binding.btnNext.setOnClickListener {
            binding.btnNext.isEnabled = false
            binding.btnNext.text = "Checking..."
            checkDeviceRegistered()
        }
    }

    private fun checkDeviceRegistered() {
        lifecycleScope.launch {
            try {
                // get Firebase JWT token
                val token = getFirebaseToken() ?: run {
                    showError("Authentication error. Please log in again.")
                    return@launch
                }

                // call GET /api/v1/devices to check if ESP32 registered
                val response = withContext(Dispatchers.IO) {
                    val url = URL("${AppConfig.SERVER_BASE_URL}/api/v1/devices")
                    val connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.setRequestProperty("Authorization", "Bearer $token")
                    connection.setRequestProperty("Content-Type", "application/json")
                    connection.connectTimeout = 10000
                    connection.readTimeout = 10000

                    val responseCode = connection.responseCode
                    val body = if (responseCode == 200) {
                        connection.inputStream.bufferedReader().readText()
                    } else {
                        connection.errorStream?.bufferedReader()?.readText() ?: ""
                    }
                    Pair(responseCode, body)
                }

                val (code, body) = response

                if (code == 200) {
                    val json = JSONObject(body)
                    val devices = json.getJSONArray("devices")

                    if (devices.length() > 0) {
                        // device found — show success notification
                        showDeviceConnectedNotification()
                        // navigate to home
                        findNavController().navigate(R.id.action_espConnect_to_home)
                    } else {
                        // no device registered yet
                        showError("No device found. Make sure you completed all the steps and try again.")
                        resetButton()
                    }
                } else {
                    showError("Server error ($code). Please try again.")
                    resetButton()
                }

            } catch (e: Exception) {
                showError("Connection error: ${e.message}")
                resetButton()
            }
        }
    }

    private suspend fun getFirebaseToken(): String? {
        return suspendCancellableCoroutine { continuation ->
            val user = FirebaseAuth.getInstance().currentUser

            if (user == null) {
                continuation.resume(null)
                return@suspendCancellableCoroutine
            }

            user.getIdToken(false)
                .addOnSuccessListener { result ->
                    if (continuation.isActive) {
                        continuation.resume(result.token)
                    }
                }
                .addOnFailureListener {
                    if (continuation.isActive) {
                        continuation.resume(null)
                    }
                }
        }
    }

    private fun showDeviceConnectedNotification() {
        val channelId = "device_connected"
        val notificationManager = requireContext()
            .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Device Connection",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = android.app.Notification.Builder(requireContext(), channelId)
            .setSmallIcon(R.drawable.ic_shield)
            .setContentTitle("Device connected!")
            .setContentText("Your fall detection device is now linked to your account.")
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1001, notification)
    }

    private fun showError(message: String) {
        activity?.runOnUiThread {
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        }
    }

    private fun resetButton() {
        activity?.runOnUiThread {
            binding.btnNext.isEnabled = true
            binding.btnNext.text = "Next"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
