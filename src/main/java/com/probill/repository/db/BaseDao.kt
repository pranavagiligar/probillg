package com.probill.repository.db

import com.probill.model.Base
import com.probill.model.Price
import java.sql.ResultSet
import java.sql.Timestamp

abstract class BaseDao<T> {

    abstract var tableName: String
    abstract fun insert(t: T): Timestamp
    abstract fun update(t: T): Boolean
    abstract fun readAll(): List<T>

    protected fun getPriceFromResultSet(resultSet: ResultSet, offset: Int) =
        Price().apply {
            var offsetVal = offset
            this.gst = resultSet.getDouble(offsetVal++)
            this.sGst = resultSet.getDouble(offsetVal++)
            this.discount = resultSet.getDouble(offsetVal++)
            this.totalPrice = resultSet.getDouble(offsetVal)
        }

    protected fun fillBaseFromResultSet(base: Base, resultSet: ResultSet) {
        base.apply {
            id = resultSet.getLong(1)
            createdAt = resultSet.getTimestamp(2)
            updatedAt = resultSet.getTimestamp(3)
        }
    }
}