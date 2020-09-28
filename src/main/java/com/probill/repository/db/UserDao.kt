package com.probill.repository.db

import com.probill.model.Table
import com.probill.model.User
import java.sql.ResultSet
import java.sql.Timestamp
import java.util.*

class UserDao : BaseDao<User>() {
    override var tableName = Table.USERS.name

    override fun insert(user: User): Timestamp {
        val now = Timestamp.from(Date().toInstant())
        val s = AppDb.connection.prepareStatement(
            "INSERT INTO $tableName ($CREATED_AT, $UPDATED_AT, $USERNAME, "
                + "$PASSWORD, $NAME, $COMPANY, $ADDRESS, "
                + "$PHONE, $GST_NUMBER, $SESSION, $ENABLED, "
                + "$EXPIRY, $POLLING_INTERVAL)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
        )?.apply {
            this.setTimestamp(1, now)
            this.setTimestamp(2, now)
            this.setString(3, user.username)
            this.setString(4, user.password)
            this.setString(5, user.name)
            this.setString(6, user.company)
            this.setString(7, user.address)
            this.setString(8, user.phone)
            this.setString(9, user.gstNumber)
            this.setString(10, user.session)
            this.setBoolean(11, user.enabled)
            this.setTimestamp(12, user.expiry)
            this.setLong(13, user.pollingInterval)
        }
        s?.execute()
        s?.close()
        return now
    }

    override fun update(user: User): Boolean {
        var success = false
        val now = Timestamp.from(Date().toInstant())
        AppDb.connection.prepareStatement(
            "UPDATE $tableName SET "
                + "$UPDATED_AT = ?, $NAME = ?, "
                + "$COMPANY = ?, $ADDRESS = ?, $PHONE = ?, $GST_NUMBER = ?, "
                + "$SESSION = ?, $ENABLED = ?, $EXPIRY = ?, $POLLING_INTERVAL = ? "
                + "WHERE $USERNAME = ?"
        )?.apply {
            this.setTimestamp(1, now)
            this.setString(2, user.name)
            this.setString(3, user.company)
            this.setString(4, user.address)
            this.setString(5, user.phone)
            this.setString(6, user.gstNumber)
            this.setString(7, user.session)
            this.setBoolean(8, user.enabled)
            this.setTimestamp(9, user.expiry)
            this.setLong(10, user.pollingInterval)
            this.setString(11, user.username)
            this.execute()
            success = this.updateCount > 0
        }?.close()
        return success
    }

    override fun readAll(): List<User> {
        val resultSet = AppDb.connection.createStatement().executeQuery(
            "SELECT * FROM $tableName"
        )
        val list = arrayListOf<User>()
        resultSet?.let {
            while (it.next()) {
                list.add(getUserByResultSet(it))
            }
        }
        return list
    }

    fun getByUsername(username: String): User? {
        AppDb.connection.prepareStatement(
            "SELECT * FROM $tableName WHERE $USERNAME=?"
        )?.apply {
            this.setString(1, username)
            this.executeQuery()?.let {
                var user: User? = null
                while (it.next()) {
                    user = getUserByResultSet(it)
                    break
                }
                close()
                return user
            }
        }
        return null
    }

    fun updatePassword(user: User): Boolean {
        var success = false
        val now = Timestamp.from(Date().toInstant())
        AppDb.connection.prepareStatement(
            "UPDATE $tableName SET "
                + "$UPDATED_AT = ?, $PASSWORD = ? WHERE $USERNAME = ?"
        )?.apply {
            this.setTimestamp(1, now)
            this.setString(2, user.password)
            this.setString(3, user.username)
            this.execute()
            success = this.updateCount > 0
        }?.close()
        return success
    }

    private fun getUserByResultSet(resultSet: ResultSet): User {
        return User(
            resultSet.getString(4),
            resultSet.getString(5),
            resultSet.getString(6),
            resultSet.getString(7),
            resultSet.getString(8),
            resultSet.getString(9),
            resultSet.getString(10),
            resultSet.getString(11),
            resultSet.getBoolean(12),
            resultSet.getTimestamp(13),
            resultSet.getLong(14)
        ).apply {
            fillBaseFromResultSet(this, resultSet)
        }
    }
}