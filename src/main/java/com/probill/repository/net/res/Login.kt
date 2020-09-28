package com.probill.repository.net.res

data class Login(
    val username: String,
    val password: String,
    val name: String,
    val company: String,
    val address: String,
    val phone: String,
    val gstNumber: String?,
    val session: String,
    val enabled: Boolean = false,
    val expiry: Long,
    val pollingInterval: Long,
    val invoicePerPage: Int,
    val breakupPerInvoice: Int,
    val eSugamRequired: Boolean
)