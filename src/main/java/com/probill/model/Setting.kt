package com.probill.model

data class Setting(
    val user: User,
    val invoicePerPage: Int,
    val breakupPerInvoice: Int,
    var eSugamRequired: Boolean
) : Base()