package com.probill.repository.db

import com.probill.Constant
import com.probill.model.Table
import com.probill.utility.Log
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

class AppDb {

    fun createTables() {
        val tables = allTables()
        Log.i(TAG, "Creating database tables")
        DDL.queries().forEach { ddl ->
            tables.find { it.equals(ddl.table.toString(), true) }?.let {
                Log.i(TAG, "Table '${ddl.table}' is already exists")
            } ?: kotlin.run {
                connection.createStatement()?.apply {
                    execute(ddl.toString())
                    close()
                }?.close()
                Log.i(TAG, "Table '${ddl.table}' is created successfully")
            }
        }
    }

    fun dropDb() {
        if (allTables().size != DDL.queries().size) return
        Log.d(TAG, "Deleting all database tables")
        connection.createStatement()?.apply {
            // Order of execution matters
            val tables = arrayOf(
                    Table.INVENTORY.name,
                    Table.BREAKUP.name,
                    Table.BILL.name,
                    Table.ITEM.name,
                    Table.SETTING.name,
                    Table.META.name,
                    Table.USERS.name
            )
            tables.forEach {
                addBatch("DROP TABLE $it")
            }
            executeBatch()
            close()
        }?.close()
    }

    private fun allTables(): List<String> {
        val list = arrayListOf<String>()
        connection.metaData?.let {
            val rs = it.getTables(null, null, null, arrayOf("TABLE"))
            rs?.let { result ->
                while (result.next()) {
                    list.add(result.getString("TABLE_NAME"))
                }
            }
        }
        return list
    }

    companion object {
        val TAG: String = AppDb::class.java.simpleName
        val connection: Connection
            get() {
                return DriverManager.getConnection(
                    "jdbc:derby:${Constant.DATABASE_NAME};create=true"
                )
            }

        fun stop() {
            if (!connection.isClosed) {
                connection.close()
            }
            try {
                DriverManager.getConnection("jdbc:derby:;shutdown=true")
            } catch (e: SQLException) {
                Log.i(TAG, e.message ?: "Database shutdown")
            }
        }

        val userDao = UserDao()
        val itemDao = ItemDao()
        val billDao = BillDao()
        val breakupDao = BreakupDao()
        val inventoryDao = InventoryDao()
        val settingDao = SettingDao()
        val metaDao = MetaDao()
    }
}