package com.probill.repository.db

import com.probill.model.Inventory
import com.probill.model.Table
import java.sql.ResultSet
import java.sql.Timestamp
import java.util.*

class InventoryDao : BaseDao<Inventory>() {
    override var tableName = Table.INVENTORY.name

    override fun insert(inventory: Inventory): Timestamp {
        val now = Timestamp.from(Date().toInstant())
        AppDb.connection.prepareStatement(
            "INSERT INTO $tableName "
                + "($CREATED_AT, $UPDATED_AT, $COUNT, $ITEM_ID)"
                + " VALUES (?, ?, ?, ?)"
        )?.apply {
            this.setTimestamp(1, now)
            this.setTimestamp(2, now)
            this.setLong(3, inventory.count)
            this.setLong(4, inventory.item.id)
            this.execute()
            close()
        }?.close()
        return now
    }

    override fun update(inventory: Inventory): Boolean {
        TODO("Not yet implemented")
    }

    override fun readAll(): List<Inventory> {
        val resultSet = AppDb.connection.createStatement().executeQuery(
            "SELECT * FROM $tableName"
        )
        val list = arrayListOf<Inventory>()
        resultSet?.let { rs ->
            while (rs.next()) {
                getInventoryByResultSet(rs)?.let { list.add(it) }
            }
        }
        return list
    }

    private fun getInventoryByResultSet(resultSet: ResultSet): Inventory? {
        val item = AppDb.itemDao
            .getItem(resultSet.getLong(5)) ?: return null
        return Inventory(
            resultSet.getLong(4),
            item
        ).apply {
            fillBaseFromResultSet(this, resultSet)
        }
    }
}