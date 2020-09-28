package com.probill.model

class Breakup(
    var bill: Bill,
    var itemId: Long,
    var quantity: Float,
    var name: String,
    var unit: Unit,
    var price: Price
) : Base()