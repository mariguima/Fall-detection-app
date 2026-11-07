package com.example.fall_detection_app.ui.alert

import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.fall_detection_app.AppConfig
import com.example.fall_detection_app.R
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.resume

class AlertFragment : Fragment() {

    private var currentEventId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.alert, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val ivFallImage = view.findViewById<ImageView>(R.id.ivFallImage)
        val tvTime = view.findViewById<TextView>(R.id.tvTime)
        val btnYes = view.findViewById<MaterialButton>(R.id.btnYes)
        val btnNo = view.findViewById<MaterialButton>(R.id.btnNo)

        // get arguments passed from HomeFragment
        val eventId = arguments?.getString("eventId") ?: ""
        val eventTime = arguments?.getString("eventTime") ?: ""

        currentEventId = eventId
        tvTime.text = "at $eventTime"
        loadFallImage(eventId, ivFallImage)

        // Yes — real fall
        btnYes.setOnClickListener {
            val id = currentEventId ?: return@setOnClickListener
            btnYes.isEnabled = false
            btnNo.isEnabled = false
            btnYes.text = "Confirming..."
            confirmFall(
                eventId = id,
                isRealFall = true,
                onSuccess = {
                    openNextWarningOrHome(id)
                },
                onError = {
                    btnYes.isEnabled = true
                    btnNo.isEnabled = true
                    btnYes.text = "Yes"
                }
            )
        }

        // No — false alarm
        btnNo.setOnClickListener {
            val id = currentEventId ?: return@setOnClickListener
            btnYes.isEnabled = false
            btnNo.isEnabled = false
            btnNo.text = "Confirming..."
            confirmFall(
                eventId = id,
                isRealFall = false,
                onSuccess = {
                    openNextWarningOrHome(id)
                },
                onError = {
                    btnYes.isEnabled = true
                    btnNo.isEnabled = true
                    btnNo.text = "No"
                }
            )
        }
    }

    private fun loadFallImage(eventId: String, imageView: ImageView) {
        imageView.setImageDrawable(null)

        if (eventId.isBlank()) {
            return
        }

        FirebaseFirestore.getInstance()
            .collection("events")
            .document(eventId)
            .get(Source.SERVER)
            .addOnSuccessListener { doc ->
                val imageUrl = doc.getString("frameImageUrl")
                val storageBucket = doc.getString("frameStorageBucket")
                val storagePath = doc.getString("frameStoragePath")

                if (!imageUrl.isNullOrBlank()) {
                    loadFallImageFromUrl(
                        eventId = eventId,
                        imageUrl = imageUrl,
                        imageView = imageView,
                        onError = {
                            loadFallImageFromStorage(
                                eventId = eventId,
                                storageBucket = storageBucket,
                                storagePath = storagePath,
                                imageView = imageView
                            )
                        }
                    )
                } else {
                    loadFallImageFromStorage(
                        eventId = eventId,
                        storageBucket = storageBucket,
                        storagePath = storagePath,
                        imageView = imageView
                    )
                }
            }
            .addOnFailureListener { error ->
                Log.e("ALERT", "Could not fetch event frame details: ${error.message}")
            }
    }

    private fun loadFallImageFromUrl(
        eventId: String,
        imageUrl: String,
        imageView: ImageView,
        onError: () -> Unit
    ) {
        lifecycleScope.launch {
            val bitmap = withContext(Dispatchers.IO) {
                runCatching {
                    val connection = URL(imageUrl).openConnection() as HttpURLConnection
                    connection.connectTimeout = 10000
                    connection.readTimeout = 10000
                    connection.inputStream.use { stream ->
                        BitmapFactory.decodeStream(stream)
                    }
                }.getOrNull()
            }

            if (bitmap == null) {
                Log.e("ALERT", "Could not load fall frame URL for event $eventId")
                onError()
                return@launch
            }

            if (currentEventId == eventId) {
                imageView.setImageBitmap(bitmap)
            }
        }
    }

    private fun loadFallImageFromStorage(
        eventId: String,
        storageBucket: String?,
        storagePath: String?,
        imageView: ImageView
    ) {
        if (storageBucket.isNullOrBlank() || storagePath.isNullOrBlank()) {
            Log.d("ALERT", "No Firebase Storage frame fields for event $eventId")
            return
        }

        val storage = FirebaseStorage.getInstance("gs://$storageBucket")
        storage.reference.child(storagePath)
            .getBytes(MAX_FRAME_IMAGE_BYTES)
            .addOnSuccessListener { bytes ->
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

                if (bitmap == null) {
                    Log.e("ALERT", "Could not decode Storage frame for event $eventId")
                    return@addOnSuccessListener
                }

                if (currentEventId == eventId) {
                    imageView.setImageBitmap(bitmap)
                }
            }
            .addOnFailureListener { error ->
                Log.e("ALERT", "Could not load Storage frame for event $eventId: ${error.message}")
            }
    }

    private fun confirmFall(
        eventId: String,
        isRealFall: Boolean,
        onSuccess: () -> Unit,
        onError: () -> Unit
    ) {
        val status = if (isRealFall) "fall" else "no_fall"

        lifecycleScope.launch {
            try {
                val token = getFirebaseToken() ?: run {
                    Toast.makeText(requireContext(),
                        "Authentication error", Toast.LENGTH_SHORT).show()
                    onError()
                    return@launch
                }

                val response = withContext(Dispatchers.IO) {
                    val url = URL("${AppConfig.SERVER_BASE_URL}/api/v1/events/$eventId/confirm")
                    val connection = url.openConnection() as HttpURLConnection
                    connection.requestMethod = "PATCH"
                    connection.setRequestProperty("Authorization", "Bearer $token")
                    connection.setRequestProperty("Content-Type", "application/json")
                    connection.doOutput = true
                    connection.connectTimeout = 10000
                    connection.readTimeout = 10000

                    val body = JSONObject().put("status", status).toString()
                    connection.outputStream.write(body.toByteArray())
                    connection.outputStream.flush()

                    val responseCode = connection.responseCode
                    val responseBody = (if (responseCode in 200..299) {
                        connection.inputStream
                    } else {
                        connection.errorStream
                    })?.bufferedReader()?.use { it.readText() }.orEmpty()
                    Pair(responseCode, responseBody)
                }

                val (code, responseBody) = response
                Log.d("ALERT", "Confirmation API response: HTTP $code ${responseBody.take(200)}")

                withContext(Dispatchers.Main) {
                    if (code in 200..299) {
                        if (!isAdded) return@withContext
                        Toast.makeText(
                            requireContext(),
                            if (isRealFall) "Fall confirmed" else "Marked as false alarm",
                            Toast.LENGTH_SHORT
                        ).show()
                        onSuccess()
                    } else {
                        val detail = responseBody.take(100).takeIf { it.isNotBlank() }
                        Toast.makeText(
                            requireContext(),
                            detail?.let { "Could not save confirmation ($code): $it" }
                                ?: "Could not save confirmation ($code)",
                            Toast.LENGTH_LONG
                        ).show()
                        onError()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (!isAdded) return@withContext
                    Log.e("ALERT", "Confirmation request failed", e)
                    Toast.makeText(
                        requireContext(),
                        "Connection error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    onError()
                }
            }
        }
    }

    private fun openNextWarningOrHome(confirmedEventId: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid

        if (uid == null) {
            findNavController().navigate(R.id.action_alert_to_home)
            return
        }

        FirebaseFirestore.getInstance()
            .collection("events")
            .whereEqualTo("userId", uid)
            .whereEqualTo("status", "warning")
            .get(Source.SERVER)
            .addOnSuccessListener { snapshots ->
                val nextWarning = snapshots.documents
                    .filter { doc -> doc.id != confirmedEventId }
                    .sortedBy { doc ->
                        doc.getTimestamp("serverTimestamp")?.seconds
                            ?: doc.getTimestamp("espTimestamp")?.seconds
                            ?: Long.MAX_VALUE
                    }
                    .firstOrNull()

                if (nextWarning == null) {
                    findNavController().navigate(R.id.action_alert_to_home)
                    return@addOnSuccessListener
                }

                val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault())
                    .format(
                        nextWarning.getTimestamp("serverTimestamp")?.toDate()
                            ?: nextWarning.getTimestamp("espTimestamp")?.toDate()
                            ?: Date()
                    )

                val bundle = Bundle().apply {
                    putString("eventId", nextWarning.id)
                    putString("eventTime", timeStr)
                }
                findNavController().navigate(R.id.alertFragment, bundle)
            }
            .addOnFailureListener {
                findNavController().navigate(R.id.action_alert_to_home)
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

    companion object {
        private const val MAX_FRAME_IMAGE_BYTES = 5L * 1024L * 1024L
    }
}
