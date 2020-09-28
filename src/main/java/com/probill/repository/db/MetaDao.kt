package com.probill.repository.db

import com.probill.model.Meta
import com.probill.model.Table
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Timestamp
import java.util.*

class MetaDao : BaseDao<Meta>() {
    override var tableName = Table.META.name

    override fun insert(meta: Meta): Timestamp {
        val now = Timestamp.from(Date().toInstant())
        val s = AppDb.connection.prepareStatement(
            "INSERT INTO $tableName ($CREATED_AT, $UPDATED_AT, $USERNAME, "
                + "$IS_LOGGED_IN)"
                + " VALUES (?, ?, ?, ?)"
        )?.apply {
            this.setTimestamp(1, now)
            this.setTimestamp(2, now)
            this.setString(3, meta.user.username)
            this.setBoolean(4, meta.isLoggedIn)
        }
        s?.execute()
        s?.close()
        return now
    }

    override fun update(meta: Meta): Boolean {
        var success = false
        val now = Timestamp.from(Date().toInstant())
        AppDb.connection.prepareStatement(
            "UPDATE $tableName SET "
                + "$UPDATED_AT = ?, $IS_LOGGED_IN = ? "
                + "WHERE $USERNAME = ?"
        )?.apply {
            this.setTimestamp(1, now)
            this.setBoolean(2, meta.isLoggedIn)
            this.setString(3, meta.user.username)
            this.execute()
            success = this.updateCount > 0
        }?.close()
        return success
    }

    override fun readAll(): List<Meta> {
        val resultSet = AppDb.connection.createStatement().executeQuery(
            "SELECT * FROM $tableName"
        )
        val list = arrayListOf<Meta>()
        resultSet?.let {
            while (it.next()) {
                getMetaByResultSet(it)?.let { meta -> list.add(meta) }
            }
        }
        return list
    }

    fun getLastMeta(): Meta? {
        val resultSet = AppDb.connection.createStatement().executeQuery(
            "SELECT * FROM $tableName ORDER BY $ID DESC FETCH FIRST 1 ROWS ONLY"
        )
        if (resultSet.fetchSize <= 0) return null
        resultSet.next()
        resultSet?.let {
            return try {
                getMetaByResultSet(it)
            } catch (e: SQLException) {
                // TODO: fix the exception to optimise the code
                e.printStackTrace()
                null
            }
        }
        return null
    }

    private fun getMetaByResultSet(resultSet: ResultSet): Meta? {
        AppDb.userDao.getByUsername(resultSet.getString(4))?.let {
            return Meta(
                it,
                resultSet.getBoolean(5)
            ).apply {
                fillBaseFromResultSet(this, resultSet)
            }
        } ?: kotlin.run {
            return null
        }
    }
}