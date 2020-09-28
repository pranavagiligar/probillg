package com.probill.repository.db

import com.probill.model.Bill
import com.probill.model.Breakup
import com.probill.model.Table
import com.probill.model.Unit
import java.sql.ResultSet
import java.sql.Timestamp
import java.util.*

class BreakupDao : BaseDao<Breakup>() {
    override var tableName = Table.BREAKUP.toString()

    override fun insert(breakup: Breakup): Timestamp {
        val now = Timestamp.from(Date().toInstant())
        AppDb.connection.prepareStatement(
            "INSERT INTO $tableName "
                + "($CREATED_AT, $UPDATED_AT, $BILL_ID, $ITEM_ID, $QUANTITY, "
                + "$NAME, $UNIT, $GST, $S_GST, $DISCOUNT, $TOTAL_PRICE)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
        )?.apply {
            this.setTimestamp(1, now)
            this.setTimestamp(2, now)
            this.setLong(3, breakup.bill.id)
            this.setLong(4, breakup.itemId)
            this.setFloat(5, breakup.quantity)
            this.setString(6, breakup.name)
            this.setString(7, breakup.unit.name)
            this.setDouble(8, breakup.price.gst)
            this.setDouble(9, breakup.price.sGst)
            this.setDouble(10, breakup.price.discount)
            this.setDouble(11, breakup.price.totalPrice)
            this.execute()
            close()
        }?.close()
        return now
    }

    @Deprecated(message = "Breakup update prohibited", level = DeprecationLevel.HIDDEN)
    override fun update(breakup: Breakup) = false

    override fun readAll(): List<Breakup> {
        val resultSet = AppDb.connection.createStatement().executeQuery(
            "SELECT * FROM $tableName"
        )
        val list = arrayListOf<Breakup>()
        resultSet?.let { rs ->
            while (rs.next()) {
                getBreakupByResultSet(rs)?.let { list.add(it) }
            }
        }
        return list
    }

    fun getBreakupByBill(bill: Bill): List<Breakup> {
        val list = arrayListOf<Breakup>()
        AppDb.connection.prepareStatement(
            "SELECT * FROM $tableName WHERE $BILL_ID=?"
        )?.apply {
            this.setLong(1, bill.id)
            this.executeQuery()?.let {
                while (it.next()) {
                    getBreakupByResultSet(it)?.let { breakup ->
                        list.add(breakup)
                    }
                }
                close()
                return list
            }
        }?.close()
        return list
    }

    private fun getBreakupByResultSet(resultSet: ResultSet): Breakup? {
        val itemId = resultSet.getLong(5)
        val bill = AppDb.billDao
            .getById(resultSet.getLong(4)) ?: return null

        return Breakup(
            bill,
            itemId,
            resultSet.getFloat(6),
            resultSet.getString(7),
            Unit.valueOf(resultSet.getString(8)),
            getPriceFromResultSet(resultSet, 9)
        ).apply {
            fillBaseFromResultSet(this, resultSet)
        }
    }
}