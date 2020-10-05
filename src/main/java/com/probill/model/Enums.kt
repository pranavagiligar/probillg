package com.probill.model

enum class Unit {
    GRAM, KG, QTL, TON
}

enum class Table(private val table: String, clazz: Class<*>) {
    USERS("users", User::class.java),
    ITEM("item", Item::class.java),
    BILL("bill", Bill::class.java),
    BREAKUP("breakup", Breakup::class.java),
    INVENTORY("inventory", Inventory::class.java),
    SETTING("setting", Setting::class.java),
    META("meta", Meta::class.java);

    override fun toString() = table.toLowerCase()
}