package com.example.fall_detection_app.ui.home

import com.google.firebase.Timestamp

data class FallEvent(
    val id: String = "",
    val deviceId: String = "",
    val userId: String = "",
    val espTimestamp: Timestamp? = null,
    val serverTimestamp: Timestamp? = null,
    val status: String = ""
)