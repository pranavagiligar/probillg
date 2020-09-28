package com.probill.model

import java.sql.Timestamp

data class User(
    val username: String,
    val password: String,
    val name: String,
    val company: String,
    val address: String,
    val phone: String,
    val gstNumber: String?,
    val session: String,
    val enabled: Boolean = false,
    val expiry: Timestamp = Timestamp(0),
    val pollingInterval: Long = 30 * 60 * 1000
) : Base()