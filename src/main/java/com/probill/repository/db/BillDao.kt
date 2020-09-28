package com.probill.repository.db

import com.probill.model.Bill
import com.probill.model.Table
import com.probill.model.User
import java.sql.ResultSet
import java.sql.Timestamp
import java.util.*

class BillDao : BaseDao<Bill>() {

    override var tableName = Table.BILL.name

    override fun insert(bill: Bill): Timestamp {
        val now = Timestamp.from(Date().toInstant())
        AppDb.connection.prepareStatement(
            "INSERT INTO $tableName "
                + "($ID, $CREATED_AT, $UPDATED_AT, $CREATED_BY, $NAME," +
                " $PHONE, $ADDRESS, $ESUGAM, $REMARK, $ITEMS,"
                + " $GST, $S_GST, $DISCOUNT, $TOTAL_PRICE)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
        )?.apply {
            this.setLong(1, bill.id)
            this.setTimestamp(2, now)
            this.setTimestamp(3, now)
            this.setString(
                4,
                AppDb.metaDao.getLastMeta()?.user?.username
                    ?: return Timestamp(0)
            )
            this.setString(5, bill.name)
            this.setString(6, bill.phone)
            this.setString(7, bill.address)
            this.setString(8, bill.eSugamNumber)
            this.setString(9, bill.remark)
            this.setString(10, bill.csvItems())
            this.setDouble(11, bill.price.gst)
            this.setDouble(12, bill.price.sGst)
            this.setDouble(13, bill.price.discount)
            this.setDouble(14, bill.price.totalPrice)
            this.execute()
            close()
        }?.close()
        return now
    }

    @Deprecated(
        message = "Bill update prohibited",
        level = DeprecationLevel.HIDDEN
    )
    override fun update(bill: Bill) = false

    override fun readAll(): List<Bill> {
        val resultSet = AppDb.connection.createStatement().executeQuery(
            "SELECT * FROM $tableName"
        )
        val list = arrayListOf<Bill>()
        resultSet?.let {
            while (it.next()) {
                val user = AppDb.userDao
                    .getByUsername(it.getString(4)) ?: return list
                list.add(getBillFromResultSet(user, it))
            }
        }
        return list
    }

    fun getById(id: Long): Bill? {
        AppDb.connection.prepareStatement(
            "SELECT * FROM $tableName WHERE $ID=?"
        )?.apply {
            this.setLong(1, id)
            this.executeQuery()?.let {
                var bill: Bill? = null
                while (it.next()) {
                    val user = AppDb.userDao
                        .getByUsername(it.getString(4)) ?: return null
                    bill = getBillFromResultSet(user, it)
                    break
                }
                close()
                return bill
            }
        }?.close()
        return null
    }

    fun getLastBillId(): Long {
        val resultSet = AppDb.connection.createStatement().executeQuery(
            "SELECT $ID FROM $tableName ORDER BY $ID DESC "
                + "FETCH FIRST 1 ROWS ONLY"
        )
        resultSet?.let {
            while (it.next()) {
                return it.getLong(1)
            }
        }
        return 0
    }

    fun getBillByCreatedAt(username: String, timestamp: Timestamp): Bill? {
        AppDb.connection.prepareStatement(
            "SELECT * FROM $tableName WHERE $CREATED_BY=? AND $CREATED_AT=?"
        )?.apply {
            this.setString(1, username)
            this.setTimestamp(2, timestamp)
            this.executeQuery()?.let {
                var bill: Bill? = null
                while (it.next()) {
                    val user = AppDb.userDao
                        .getByUsername(it.getString(4)) ?: return null
                    bill = getBillFromResultSet(user, it)
                    break
                }
                close()
                return bill
            }
        }?.close()
        return null
    }

    private fun getBillFromResultSet(user: User, resultSet: ResultSet): Bill {
        return Bill(
            user,
            resultSet.getString(5),
            resultSet.getString(6),
            resultSet.getString(7),
            resultSet.getString(8),
            resultSet.getString(9),
            AppDb.itemDao.getItems(resultSet.getString(10)),
            getPriceFromResultSet(resultSet, 11)
        ).apply {
            fillBaseFromResultSet(this, resultSet)
        }
    }
}