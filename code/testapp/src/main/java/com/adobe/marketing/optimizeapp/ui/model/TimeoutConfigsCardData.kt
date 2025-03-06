package com.adobe.marketing.optimizeapp.ui.model

data class TimeoutConfigsCardData(
    val value: String,
    val pref1Txt: String = "Default value",
    val pref2Txt: String = "Custom value",
    val isCustomTimeoutOpted: Boolean = false
)
