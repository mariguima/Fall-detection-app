package com.example.fall_detection_app.ui.home

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fall_detection_app.R
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class HomeFragment : Fragment() {

    private var firestoreListener: ListenerRegistration? = null
    private lateinit var fallAdapter: FallHistoryAdapter
    private val fallEvents = mutableListOf<FallEvent>()
    private val notifiedWarnings = mutableSetOf<String>()

    // keep view references accessible across functions
    private var rvFallHistory: RecyclerView? = null
    private var emptyState: View? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.home, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val drawerLayout = view.findViewById<DrawerLayout>(R.id.drawerLayout)
        val tvHello = view.findViewById<TextView>(R.id.tvHello)
        rvFallHistory = view.findViewById(R.id.rvFallHistory)
        emptyState = view.findViewById(R.id.emptyState)

        // set up RecyclerView
        fallAdapter = FallHistoryAdapter(fallEvents)
        rvFallHistory?.layoutManager = LinearLayoutManager(requireContext())
        rvFallHistory?.adapter = fallAdapter

        // show empty state immediately while waiting for Firestore
        showEmptyState(true)

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

            // start listening to events
            listenToEvents(uid)
        }

        // menu items
        view.findViewById<TextView>(R.id.tvChangeProfile).setOnClickListener {
            drawerLayout.closeDrawers()
            findNavController().navigate(R.id.action_home_to_changeProfile)
        }

        view.findViewById<TextView>(R.id.tvChangePassword).setOnClickListener {
            drawerLayout.closeDrawers()
            findNavController().navigate(R.id.action_home_to_changePassword)
        }

        view.findViewById<TextView>(R.id.tvConnectDevice).setOnClickListener {
            drawerLayout.closeDrawers()
            findNavController().navigate(R.id.action_home_to_espConnect)
        }

        // disconnect button
        view.findViewById<MaterialButton>(R.id.btnLogout).setOnClickListener {
            firestoreListener?.remove()
            FirebaseAuth.getInstance().signOut()
            findNavController().navigate(
                R.id.loginFragment, null,
                androidx.navigation.NavOptions.Builder()
                    .setPopUpTo(R.id.nav_graph, true)
                    .build()
            )
        }
    }

    private fun showEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            emptyState?.visibility = View.VISIBLE
            rvFallHistory?.visibility = View.GONE
        } else {
            emptyState?.visibility = View.GONE
            rvFallHistory?.visibility = View.VISIBLE
        }
    }

    private fun listenToEvents(uid: String) {
        val db = FirebaseFirestore.getInstance()

        firestoreListener = db.collection("events")
            .whereEqualTo("userId", uid)
            .orderBy("espTimestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e("HOME", "Firestore error: ${error.message}")
                    return@addSnapshotListener
                }

                val events = snapshots?.documents?.map { doc ->
                    FallEvent(
                        id = doc.id,
                        deviceId = doc.getString("deviceId") ?: "",
                        userId = doc.getString("userId") ?: "",
                        espTimestamp = doc.getTimestamp("espTimestamp"),
                        serverTimestamp = doc.getTimestamp("serverTimestamp"),
                        status = doc.getString("status") ?: ""
                    )
                } ?: emptyList()

                fallAdapter.updateEvents(events)
                showEmptyState(events.isEmpty())

                // notify for new warnings only
                events.filter { it.status == "warning" && it.id !in notifiedWarnings }
                    .forEach { event ->
                        showFallNotification(event)
                        notifiedWarnings.add(event.id)
                    }
            }
    }

    private fun showFallNotification(event: FallEvent) {
        val channelId = "fall_alerts"
        val notificationManager = requireContext()
            .getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Fall Alerts",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = android.app.Notification.Builder(requireContext(), channelId)
            .setSmallIcon(R.drawable.ic_shield)
            .setContentTitle("Fall detected!")
            .setContentText("A possible fall was detected. Please confirm.")
            .setAutoCancel(true)
            .build()

        notificationManager.notify(event.id.hashCode(), notification)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        firestoreListener?.remove()
        rvFallHistory = null
        emptyState = null
    }
}