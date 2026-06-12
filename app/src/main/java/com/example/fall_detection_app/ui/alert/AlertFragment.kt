package com.example.fall_detection_app.ui.alert

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.fall_detection_app.R
import com.google.android.material.button.MaterialButton
import android.widget.TextView

class AlertFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.alert, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val tvStatus = view.findViewById<TextView>(R.id.tvStatus)
        val layoutNoFall = view.findViewById<View>(R.id.layoutNoFall)
        val layoutFallDetected = view.findViewById<View>(R.id.layoutFallDetected)
        val btnViewHistory = view.findViewById<MaterialButton>(R.id.btnViewHistory)
        val btnYes = view.findViewById<MaterialButton>(R.id.btnYes)
        val btnNo = view.findViewById<MaterialButton>(R.id.btnNo)

        // Default state — no fall
        showNoFallState(tvStatus, layoutNoFall, layoutFallDetected, btnViewHistory, btnYes, btnNo)

        btnViewHistory.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_history)
        }

        // Yes — real fall, go to emergency
        btnYes.setOnClickListener {
            findNavController().navigate(R.id.action_home_to_alert)
        }

        // No — false alarm, go back to no fall state
        btnNo.setOnClickListener {
            showNoFallState(tvStatus, layoutNoFall, layoutFallDetected, btnViewHistory, btnYes, btnNo)
        }
    }

    private fun showNoFallState(
        tvStatus: TextView,
        layoutNoFall: View,
        layoutFallDetected: View,
        btnViewHistory: MaterialButton,
        btnYes: MaterialButton,
        btnNo: MaterialButton
    ) {
        tvStatus.text = "No fall\ndetected yet"
        layoutNoFall.visibility = View.VISIBLE
        layoutFallDetected.visibility = View.GONE
        btnViewHistory.visibility = View.VISIBLE
        btnYes.visibility = View.GONE
        btnNo.visibility = View.GONE
    }

    fun showFallDetectedState(time: String) {
        val view = view ?: return
        val tvStatus = view.findViewById<TextView>(R.id.tvStatus)
        val layoutNoFall = view.findViewById<View>(R.id.layoutNoFall)
        val layoutFallDetected = view.findViewById<View>(R.id.layoutFallDetected)
        val btnViewHistory = view.findViewById<MaterialButton>(R.id.btnViewHistory)
        val btnYes = view.findViewById<MaterialButton>(R.id.btnYes)
        val btnNo = view.findViewById<MaterialButton>(R.id.btnNo)
        val tvTime = view.findViewById<TextView>(R.id.tvTime)

        tvStatus.text = "Fall detected!"
        tvTime.text = "at $time"
        layoutNoFall.visibility = View.GONE
        layoutFallDetected.visibility = View.VISIBLE
        btnViewHistory.visibility = View.GONE
        btnYes.visibility = View.VISIBLE
        btnNo.visibility = View.VISIBLE
    }
}