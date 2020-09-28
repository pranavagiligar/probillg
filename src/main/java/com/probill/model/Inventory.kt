package com.probill.model

data class Inventory(
        var count: Long,
        var item: Item
) : Base()