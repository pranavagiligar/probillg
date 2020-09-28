package com.probill.repository.db

import com.probill.model.Setting
import com.probill.model.Table
import com.probill.model.User
import java.sql.ResultSet
import java.sql.Timestamp
import java.util.*

class SettingDao: BaseDao<Setting>() {
    override var tableName = Table.SETTING.name

    override fun insert(setting: Setting): Timestamp {
        val now = Timestamp.from(Date().toInstant())
        val s = AppDb.connection.prepareStatement(
            "INSERT INTO $tableName ($CREATED_AT, $UPDATED_AT, $USERNAME, "
                + "$BREAKUP_PER_INVOICE, $INVOICE_PER_PAGE, $ESUGAM_REQUIRED)"
                + " VALUES (?, ?, ?, ?, ?, ?)"
        )?.apply {
            this.setTimestamp(1, now)
            this.setTimestamp(2, now)
            this.setString(3, setting.user.username)
            this.setInt(4, setting.breakupPerInvoice)
            this.setInt(5, setting.invoicePerPage)
            this.setBoolean(6, setting.eSugamRequired)
        }
        s?.execute()
        s?.close()
        return now
    }

    override fun update(setting: Setting): Boolean {
        var success = false
        val now = Timestamp.from(Date().toInstant())
        AppDb.connection.prepareStatement(
            "UPDATE $tableName SET "
                + "$UPDATED_AT = ?, $USERNAME = ?, $BREAKUP_PER_INVOICE = ?, "
                + "$INVOICE_PER_PAGE = ?, $ESUGAM_REQUIRED = ? "
                + "WHERE $ID = ?"
        )?.apply {
            this.setTimestamp(1, now)
            this.setString(2, setting.user.username)
            this.setInt(3, setting.breakupPerInvoice)
            this.setInt(4, setting.invoicePerPage)
            this.setBoolean(5, setting.eSugamRequired)
            this.setLong(6, setting.id)
            this.execute()
            success = this.updateCount > 0
        }?.close()
        return success
    }

    override fun readAll(): List<Setting> {
        val resultSet = AppDb.connection.createStatement().executeQuery(
            "SELECT * FROM $tableName"
        )
        val list = arrayListOf<Setting>()
        resultSet?.let {
            while (it.next()) {
                getSettingByResultSet(it)?.let { setting -> list.add(setting) }
            }
        }
        return list
    }

    fun getSettingForUsername(username: String): Setting? {
        AppDb.connection.prepareStatement(
            "SELECT * FROM $tableName WHERE $USERNAME = ?"
        )?.apply {
            this.setString(1, username)
            this.executeQuery()?.let {
                var setting: Setting? = null
                while (it.next()) {
                    setting = getSettingByResultSet(it)
                    break
                }
                close()
                return setting
            }
        }
        return null
    }

    private fun getSettingByResultSet(resultSet: ResultSet): Setting? {
        AppDb.userDao.getByUsername(resultSet.getString(4))?.let {
            return Setting(
                it,
                resultSet.getInt(5),
                resultSet.getInt(6),
                resultSet.getBoolean(7)
            ).apply {
                fillBaseFromResultSet(this, resultSet)
            }
        } ?: kotlin.run {
            return null
        }
    }
}