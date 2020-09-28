package com.probill.model

data class Bill(
    var createdBy: User,
    var name: String,
    var phone: String? = null,
    var address: String? = null,
    var eSugamNumber: String? = null,
    var remark: String? = null,
    var items: List<Item>? = null,
    var price: Price
) : Base() {

    fun csvItems(): String? = items?.joinToString {
        return@joinToString it.id.toString()
    }
}