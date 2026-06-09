package com.example.fall_detection_app.ui.onboarding.slides

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.util.Log
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.example.fall_detection_app.R
import com.example.fall_detection_app.ui.onboarding.OnboardingViewModel

class RequirementsFragment : Fragment() {

    interface PermissionCallback {
        fun onPermissionsGranted()
        fun onPermissionsDenied()
    }

    private var callback: PermissionCallback? = null

    // shares the same ViewModel instance as OnboardingFragment
    private val viewModel: OnboardingViewModel by activityViewModels()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        checkPermissionsAndUpdateButton()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.slide_requirements, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        callback = parentFragment as? PermissionCallback

        // observe trigger from OnboardingFragment
        viewModel.requestPermissionsTrigger.observe(viewLifecycleOwner) { shouldRequest ->
            if (shouldRequest == true) {
                requestNeededPermissions()
                viewModel.requestPermissionsTrigger.value = false
            }
        }
    }

    private fun requestNeededPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (!isEmulator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!isGranted(Manifest.permission.BLUETOOTH_CONNECT))
                    permissionsToRequest.add(Manifest.permission.BLUETOOTH_CONNECT)
                if (!isGranted(Manifest.permission.BLUETOOTH_SCAN))
                    permissionsToRequest.add(Manifest.permission.BLUETOOTH_SCAN)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!isGranted(Manifest.permission.POST_NOTIFICATIONS)) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
                Log.d("PERMISSIONS", "Notifications NOT granted — will request")
            } else {
                Log.d("PERMISSIONS", "Notifications already granted — skipping")
            }
        } else {
            Log.d("PERMISSIONS", "Android version below 13 — no notification permission needed")
        }

        Log.d("PERMISSIONS", "Permissions to request: $permissionsToRequest")

        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            checkPermissionsAndUpdateButton()
        }
    }

    private fun checkPermissionsAndUpdateButton() {
        val bluetoothGranted = if (isEmulator()) true
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            isGranted(Manifest.permission.BLUETOOTH_CONNECT) &&
                    isGranted(Manifest.permission.BLUETOOTH_SCAN)
        } else true

        val notificationsGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            isGranted(Manifest.permission.POST_NOTIFICATIONS)
        } else true

        if (bluetoothGranted && notificationsGranted) {
            callback?.onPermissionsGranted()
        } else {
            callback?.onPermissionsDenied()
        }
    }

    private fun isGranted(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            requireContext(), permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun isEmulator(): Boolean {
        return (Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || Build.BRAND.startsWith("generic"))
    }
}