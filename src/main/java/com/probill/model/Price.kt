package com.probill.model

class Price {
    var gst: Double = 0.0
    var sGst: Double = 0.0
    var discount: Double = 0.0
    var totalPrice: Double = 0.0

    fun csvPrice() = "gst=$gst, sgst=$sGst, dis=$discount, total=$totalPrice"
}