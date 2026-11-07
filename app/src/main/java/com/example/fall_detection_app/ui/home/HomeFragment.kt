package com.example.fall_detection_app.ui.home

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupWindow
import android.widget.TextView
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.fall_detection_app.R
import com.example.fall_detection_app.notifications.PushTokenManager
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source

class HomeFragment : Fragment() {

    private var firestoreListener: ListenerRegistration? = null
    private lateinit var fallAdapter: FallHistoryAdapter
    private val fallEvents = mutableListOf<FallEvent>()
    private val visibleFallEvents = mutableListOf<FallEvent>()
    private val notifiedWarnings = mutableSetOf<String>()

    // keep view references accessible across functions
    private var rvFallHistory: RecyclerView? = null
    private var emptyState: View? = null
    private var weeklyFallsView: TextView? = null
    private var falseAlarmsView: TextView? = null
    private var confirmedFallsPercentageView: TextView? = null
    private var falseAlarmsPercentageView: TextView? = null
    private var totalAlarmsView: TextView? = null
    private var pieChartView: FallAnalyticsPieChartView? = null
    private var historyEmptyTitleView: TextView? = null
    private var historyEmptySubtitleView: TextView? = null
    private var selectedAnalyticsPeriod = AnalyticsPeriod.LAST_7_DAYS
    private val selectedHistoryFilters = mutableSetOf(HistoryFilter.ALL)
    private var historyFilterPopup: PopupWindow? = null

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
        weeklyFallsView = view.findViewById(R.id.tvWeeklyFalls)
        falseAlarmsView = view.findViewById(R.id.tvFalseAlarms)
        confirmedFallsPercentageView = view.findViewById(R.id.tvConfirmedFallsPercentage)
        falseAlarmsPercentageView = view.findViewById(R.id.tvFalseAlarmsPercentage)
        totalAlarmsView = view.findViewById(R.id.tvTotalAlarms)
        pieChartView = view.findViewById(R.id.fallAnalyticsPieChart)
        historyEmptyTitleView = view.findViewById(R.id.tvEmptyTitle)
        historyEmptySubtitleView = view.findViewById(R.id.tvEmptySubtitle)

        val periodToggleGroup = view.findViewById<MaterialButtonToggleGroup>(R.id.periodToggleGroup)
        periodToggleGroup.check(
            when (selectedAnalyticsPeriod) {
                AnalyticsPeriod.LAST_7_DAYS -> R.id.btnLast7Days
                AnalyticsPeriod.LAST_MONTH -> R.id.btnLastMonth
                AnalyticsPeriod.LAST_YEAR -> R.id.btnLastYear
            }
        )
        periodToggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                selectedAnalyticsPeriod = when (checkedId) {
                    R.id.btnLastMonth -> AnalyticsPeriod.LAST_MONTH
                    R.id.btnLastYear -> AnalyticsPeriod.LAST_YEAR
                    else -> AnalyticsPeriod.LAST_7_DAYS
                }
                updateAnalytics(fallEvents)
            }
        }

        val historyFilterButton = view.findViewById<MaterialButton>(R.id.btnHistoryFilter)
        historyFilterButton.setOnClickListener {
            showHistoryFilterPopup(historyFilterButton)
        }

        // set up RecyclerView
        fallAdapter = FallHistoryAdapter(visibleFallEvents) { event ->
            val bundle = Bundle().apply {
                putString("eventId", event.id)
            }
            findNavController().navigate(R.id.action_home_to_historyImage, bundle)
        }
        rvFallHistory?.layoutManager = LinearLayoutManager(requireContext())
        rvFallHistory?.adapter = fallAdapter
        sizeScrollableHomePage(view)

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
            loadEventsFromServer(uid)
        }

        // menu items
        view.findViewById<TextView>(R.id.tvChangeProfile).setOnClickListener {
            drawerLayout.closeDrawers()
            findNavController().navigate(R.id.action_home_to_changeProfile)
        }

        view.findViewById<TextView>(R.id.tvConnectDevice).setOnClickListener {
            drawerLayout.closeDrawers()
            findNavController().navigate(R.id.action_home_to_espConnect)
        }

        // disconnect button
        view.findViewById<MaterialButton>(R.id.btnLogout).setOnClickListener {
            firestoreListener?.remove()
            PushTokenManager.removeCurrentUserToken()
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

    private fun sizeScrollableHomePage(view: View) {
        val pageScrollView = view.findViewById<HalfScreenNestedScrollView>(R.id.homeContentScrollView)
        val pageContent = view.findViewById<ViewGroup>(R.id.homePageContent)
        val historyCard = view.findViewById<View>(R.id.card)

        pageScrollView.post {
            val viewportHeight = pageScrollView.height
            if (viewportHeight <= 0) return@post

            var contentHeightWithoutHistoryCard = pageContent.paddingTop + pageContent.paddingBottom
            for (index in 0 until pageContent.childCount) {
                val child = pageContent.getChildAt(index)
                val layoutParams = child.layoutParams as? ViewGroup.MarginLayoutParams
                val verticalMargins = (layoutParams?.topMargin ?: 0) + (layoutParams?.bottomMargin ?: 0)
                contentHeightWithoutHistoryCard += verticalMargins
                if (child !== historyCard) {
                    contentHeightWithoutHistoryCard += child.height
                }
            }

            val targetHistoryHeight = (viewportHeight + viewportHeight / 2 - contentHeightWithoutHistoryCard)
                .coerceAtLeast(1)
            val historyLayoutParams = historyCard.layoutParams
            if (historyLayoutParams.height != targetHistoryHeight) {
                historyLayoutParams.height = targetHistoryHeight
                historyCard.layoutParams = historyLayoutParams
            }
        }
    }

    private fun eventsQuery(uid: String): Query {
        val db = FirebaseFirestore.getInstance()

        return db.collection("events")
            .whereEqualTo("userId", uid)
    }

    private fun loadEventsFromServer(uid: String) {
        firestoreListener?.remove()
        fallEvents.clear()
        updateHistoryList()

        eventsQuery(uid)
            .get(Source.SERVER)
            .addOnSuccessListener { snapshots ->
                val events = snapshots.documents.map { doc -> doc.toFallEvent() }

                updateEventList(events)
                notifyForNewWarnings(events)
                listenToEvents(uid)
            }
            .addOnFailureListener { error ->
                Log.e("HOME", "Firestore server refresh error: ${error.message}")
                listenToEvents(uid)
            }
    }

    private fun listenToEvents(uid: String) {
        firestoreListener?.remove()

        firestoreListener = eventsQuery(uid)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.e("HOME", "Firestore error: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshots?.metadata?.isFromCache == true) {
                    Log.d("HOME", "Ignoring cached events snapshot")
                    return@addSnapshotListener
                }

                Log.d("HOME", "Events received: ${snapshots?.documents?.size}")
                snapshots?.documents?.forEach { doc ->
                    Log.d("HOME", "Event: ${doc.id} status=${doc.getString("status")}")
                }

                val events = snapshots?.documents?.map { doc -> doc.toFallEvent() } ?: emptyList()
                updateEventList(events)
                notifyForNewWarnings(events)

            }
    }

    private fun DocumentSnapshot.toFallEvent(): FallEvent {
        return FallEvent(
            id = id,
            deviceId = getString("deviceId") ?: "",
            userId = getString("userId") ?: "",
            espTimestamp = getTimestamp("espTimestamp"),
            serverTimestamp = getTimestamp("serverTimestamp"),
            status = getString("status") ?: "",
            source = getString("source") ?: "",
            frameSequence = getNumberField("frameSequence")?.toLong(),
            fallScore = getNumberField("fall score")?.toDouble()
                ?: getNumberField("fallScore")?.toDouble()
        )
    }

    private fun DocumentSnapshot.getNumberField(field: String): Number? {
        return when (val value = get(field)) {
            is Number -> value
            is String -> value.toDoubleOrNull()
            else -> null
        }
    }

    private fun updateEventList(events: List<FallEvent>) {
        updateAnalytics(events)
        val sortedEvents = events.sortedByDescending { event ->
            event.serverTimestamp?.seconds ?: event.espTimestamp?.seconds ?: 0
        }

        fallEvents.clear()
        fallEvents.addAll(sortedEvents)
        updateHistoryList()
    }

    private fun updateAnalytics(events: List<FallEvent>) {
        val nowMillis = System.currentTimeMillis()
        val periodStartMillis = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            when (selectedAnalyticsPeriod) {
                AnalyticsPeriod.LAST_7_DAYS -> add(Calendar.DAY_OF_YEAR, -7)
                AnalyticsPeriod.LAST_MONTH -> add(Calendar.MONTH, -1)
                AnalyticsPeriod.LAST_YEAR -> add(Calendar.YEAR, -1)
            }
        }.timeInMillis

        val periodEvents = events.filter { event ->
            val eventMillis = (event.serverTimestamp ?: event.espTimestamp)?.toDate()?.time
            eventMillis != null && eventMillis in periodStartMillis..nowMillis
        }
        val confirmedFalls = periodEvents.count { it.status == "fall" }
        val reviewedAlerts = periodEvents.count {
            it.status == "fall" || it.status == "no_fall" || it.status == "fake_alarm"
        }
        val falseAlarms = periodEvents.count { it.status == "no_fall" || it.status == "fake_alarm" }
        val confirmedFallsPercentage = if (reviewedAlerts == 0) 0 else Math.round(confirmedFalls * 100f / reviewedAlerts)
        val falseAlarmsPercentage = if (reviewedAlerts == 0) 0 else 100 - confirmedFallsPercentage

        weeklyFallsView?.text = confirmedFalls.toString()
        falseAlarmsView?.text = falseAlarms.toString()
        totalAlarmsView?.text = periodEvents.size.toString()
        confirmedFallsPercentageView?.text = if (reviewedAlerts == 0) "N/A" else "$confirmedFallsPercentage%"
        falseAlarmsPercentageView?.text = if (reviewedAlerts == 0) "N/A" else "$falseAlarmsPercentage%"
        pieChartView?.setCounts(confirmedFalls, falseAlarms)
        pieChartView?.contentDescription = if (reviewedAlerts == 0) {
            "No reviewed fall alerts in this period"
        } else {
            "Confirmed falls $confirmedFallsPercentage percent; false alarms $falseAlarmsPercentage percent"
        }
    }

    private fun updateHistoryList() {
        val showAllEvents = HistoryFilter.ALL in selectedHistoryFilters
        val filteredEvents = if (showAllEvents) {
            fallEvents
        } else {
            fallEvents.filter { event ->
                (HistoryFilter.CONFIRMED_FALLS in selectedHistoryFilters && event.status == "fall") ||
                    (HistoryFilter.FALSE_ALARMS in selectedHistoryFilters &&
                        (event.status == "no_fall" || event.status == "fake_alarm"))
            }
        }

        fallAdapter.updateEvents(filteredEvents)
        when {
            showAllEvents -> {
                historyEmptyTitleView?.text = "No falls detected yet"
                historyEmptySubtitleView?.text = "Once the device picks up a fall, it'll show up here with the date and time."
            }
            selectedHistoryFilters.isEmpty() -> {
                historyEmptyTitleView?.text = "No filters selected"
                historyEmptySubtitleView?.text = "Choose one or more event types from Filter to show matching alerts."
            }
            HistoryFilter.CONFIRMED_FALLS in selectedHistoryFilters &&
                HistoryFilter.FALSE_ALARMS in selectedHistoryFilters -> {
                historyEmptyTitleView?.text = "No reviewed alerts"
                historyEmptySubtitleView?.text = "Confirmed falls and false alarms will appear here."
            }
            HistoryFilter.CONFIRMED_FALLS in selectedHistoryFilters -> {
                historyEmptyTitleView?.text = "No confirmed falls"
                historyEmptySubtitleView?.text = "Falls confirmed as real will appear here."
            }
            else -> {
                historyEmptyTitleView?.text = "No false alarms"
                historyEmptySubtitleView?.text = "Alerts marked as not a fall will appear here."
            }
        }
        showEmptyState(filteredEvents.isEmpty())
    }

    private fun showHistoryFilterPopup(anchor: View) {
        historyFilterPopup?.let { popup ->
            if (popup.isShowing) {
                popup.dismiss()
                return
            }
        }

        val popupContent = layoutInflater.inflate(R.layout.history_filter_dropdown, null, false)
        val allCheckBox = popupContent.findViewById<MaterialCheckBox>(R.id.checkFilterAll)
        val confirmedCheckBox = popupContent.findViewById<MaterialCheckBox>(R.id.checkFilterConfirmed)
        val falseAlarmsCheckBox = popupContent.findViewById<MaterialCheckBox>(R.id.checkFilterFalseAlarms)

        allCheckBox.isChecked = HistoryFilter.ALL in selectedHistoryFilters
        confirmedCheckBox.isChecked = HistoryFilter.CONFIRMED_FALLS in selectedHistoryFilters
        falseAlarmsCheckBox.isChecked = HistoryFilter.FALSE_ALARMS in selectedHistoryFilters

        var synchronizingChecks = false
        fun syncCheckboxesWithSelection() {
            synchronizingChecks = true
            allCheckBox.isChecked = HistoryFilter.ALL in selectedHistoryFilters
            confirmedCheckBox.isChecked = HistoryFilter.CONFIRMED_FALLS in selectedHistoryFilters
            falseAlarmsCheckBox.isChecked = HistoryFilter.FALSE_ALARMS in selectedHistoryFilters
            synchronizingChecks = false
        }

        allCheckBox.setOnCheckedChangeListener { _, isChecked ->
            if (synchronizingChecks) return@setOnCheckedChangeListener
            selectedHistoryFilters.clear()
            if (isChecked) selectedHistoryFilters.add(HistoryFilter.ALL)
            syncCheckboxesWithSelection()
            updateHistoryList()
        }

        fun categoryListener(filter: HistoryFilter) =
            android.widget.CompoundButton.OnCheckedChangeListener { _, isChecked ->
                if (!synchronizingChecks) {
                    selectedHistoryFilters.clear()
                    if (isChecked) {
                        selectedHistoryFilters.add(filter)
                    }
                    syncCheckboxesWithSelection()
                    updateHistoryList()
                }
            }

        confirmedCheckBox.setOnCheckedChangeListener(categoryListener(HistoryFilter.CONFIRMED_FALLS))
        falseAlarmsCheckBox.setOnCheckedChangeListener(categoryListener(HistoryFilter.FALSE_ALARMS))

        val popupWidth = (220 * resources.displayMetrics.density).toInt()
        val popup = PopupWindow(
            popupContent,
            popupWidth,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            elevation = 8 * resources.displayMetrics.density
            isOutsideTouchable = true
            setOnDismissListener {
                if (historyFilterPopup === this) historyFilterPopup = null
            }
        }
        historyFilterPopup = popup
        popup.showAsDropDown(anchor, anchor.width - popupWidth, 0)
    }

    private fun notifyForNewWarnings(events: List<FallEvent>) {
        events.filter { it.status == "warning" && it.id !in notifiedWarnings }
            .forEach { event ->
                showFallNotification(event)
                notifiedWarnings.add(event.id)

                val timeStr = SimpleDateFormat("HH:mm", Locale.getDefault())
                    .format(event.serverTimestamp?.toDate() ?: Date())

                val currentDestination = findNavController().currentDestination?.id
                if (currentDestination == R.id.homeFragment) {
                    val bundle = Bundle().apply {
                        putString("eventId", event.id)
                        putString("eventTime", timeStr)
                    }
                    findNavController().navigate(R.id.action_home_to_alert, bundle)
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
        historyFilterPopup?.dismiss()
        historyFilterPopup = null
        super.onDestroyView()
        firestoreListener?.remove()
        rvFallHistory = null
        emptyState = null
        weeklyFallsView = null
        falseAlarmsView = null
        confirmedFallsPercentageView = null
        falseAlarmsPercentageView = null
        totalAlarmsView = null
        pieChartView = null
        historyEmptyTitleView = null
        historyEmptySubtitleView = null
    }

    private enum class AnalyticsPeriod {
        LAST_7_DAYS,
        LAST_MONTH,
        LAST_YEAR
    }

    private enum class HistoryFilter {
        ALL,
        CONFIRMED_FALLS,
        FALSE_ALARMS
    }
}
