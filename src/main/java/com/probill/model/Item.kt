package com.probill.model

data class Item(
    var name: String,
    var unit: Unit = Unit.KG,
    var price: Price
) : Base() {

    var gst: Double = price.gst
    var sGst: Double = price.sGst
    var discount: Double = price.discount
    var totalPrice: Double = price.totalPrice

    override fun toString(): String {
        return "$name | $unit | ${price.csvPrice()}"
    }
}