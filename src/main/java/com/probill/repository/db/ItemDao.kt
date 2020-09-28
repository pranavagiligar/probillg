package com.probill.repository.db

import com.probill.model.Item
import com.probill.model.Table
import com.probill.model.Unit
import java.sql.ResultSet
import java.sql.Timestamp
import java.util.*

class ItemDao : BaseDao<Item>() {
    override var tableName = Table.ITEM.name

    override fun insert(item: Item): Timestamp {
        val now = Timestamp.from(Date().toInstant())
        AppDb.connection.prepareStatement(
            "INSERT INTO $tableName "
                + "($CREATED_AT, $UPDATED_AT, $NAME, $UNIT, "
                + "$GST, $S_GST, $DISCOUNT, $TOTAL_PRICE)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?)"
        )?.apply {
            this.setTimestamp(1, now)
            this.setTimestamp(2, now)
            this.setString(3, item.name)
            this.setString(4, item.unit.name)
            this.setDouble(5, item.price.gst)
            this.setDouble(6, item.price.sGst)
            this.setDouble(7, item.price.discount)
            this.setDouble(8, item.price.totalPrice)
            this.execute()
            close()
        }?.close()
        return now
    }

    override fun update(item: Item): Boolean {
        var success = false
        val now = Timestamp.from(Date().toInstant())
        AppDb.connection.prepareStatement(
            "UPDATE $tableName SET "
                + "$UPDATED_AT = ?, $NAME = ?, $UNIT = ?, "
                + "$GST = ?, $S_GST = ?, $DISCOUNT = ?, $TOTAL_PRICE = ? "
                + "WHERE $ID = ?"
        )?.apply {
            this.setTimestamp(1, now)
            this.setString(2, item.name)
            this.setString(3, item.unit.name)
            this.setDouble(4, item.price.gst)
            this.setDouble(5, item.price.sGst)
            this.setDouble(6, item.price.discount)
            this.setDouble(7, item.price.totalPrice)
            this.setLong(8, item.id)
            this.execute()
            success = this.updateCount > 0
            close()
        }?.close()
        return success
    }

    override fun readAll(): List<Item> {
        val resultSet = AppDb.connection.createStatement().executeQuery(
            "SELECT * FROM $tableName"
        )
        val list = arrayListOf<Item>()
        resultSet?.let {
            while (it.next()) {
                list.add(getItemByResultSet(it))
            }
        }
        return list
    }

    fun delete(item: Item): Boolean {
        var success = false
        val now = Timestamp.from(Date().toInstant())
        AppDb.connection.prepareStatement(
            "DELETE FROM $tableName WHERE $ID = ?"
        )?.apply {
            this.setLong(1, item.id)
            this.execute()
            success = this.updateCount > 0
            close()
        }?.close()
        return success
    }

    fun getItems(csvIds: String): List<Item>? {
        if (csvIds.isEmpty()) return null
        val resultSet = AppDb.connection.createStatement().executeQuery(
            "SELECT * FROM $tableName WHERE $ID IN ($csvIds)"
        )
        val list = arrayListOf<Item>()
        resultSet?.let {
            while (it.next()) {
                list.add(getItemByResultSet(it))
            }
        }
        return null
    }

    fun getItem(id: Long): Item? {
        AppDb.connection.prepareStatement(
            "SELECT * FROM $tableName WHERE $ID=?"
        )?.apply {
            this.setLong(1, id)
            this.executeQuery()?.let {
                var item: Item? = null
                while (it.next()) {
                    item = getItemByResultSet(it)
                    break
                }
                close()
                return item
            }
        }?.close()
        return null
    }

    private fun getItemByResultSet(resultSet: ResultSet): Item {
        return Item(
            resultSet.getString(4),
            Unit.valueOf(resultSet.getString(5)),
            getPriceFromResultSet(resultSet, 6)
        ).apply {
            fillBaseFromResultSet(this, resultSet)
        }
    }
}