package com.probill.test

import com.probill.model.*
import com.probill.model.Unit
import com.probill.repository.db.AppDb
import com.probill.utility.Log
import java.sql.Timestamp

class MockInsertTest {
    private val TAG = MockInsertTest::class.java.simpleName

    fun start() {
        // Note: password 1234 with md5
        val user = User(
            "grpranava",
            "81dc9bdb52d04dc20036dbd8313ed05",
            "Ranganna",
            "GajananaMill",
            "Talguppa",
            "9481350855",
            "GSTIN1234",
            "12345",
            true,
            Timestamp(0),
            2
        )
        val setting = Setting(
            user,
            2,
            4,
            true
        )
        val meta = Meta(
            user,
            true
        )
        val itemRice = Item(
            "Rice",
            Unit.KG,
            Price().apply {
                this.gst = 15.0
                this.sGst = 5.0
                this.totalPrice = 80.0
            }
        ).apply { id = 1 }
        val itemPoha = Item(
            "Poha",
            Unit.TON,
            Price().apply {
                this.gst = 2.0
                this.sGst = 10.0
                this.discount = 2.0
                this.totalPrice = 45.0
            }
        ).apply { id = 2 }
        val bill = Bill(
            user,
            "Rajiv",
            "9449457575",
            "Geejgar",
            "123214214",
            "Comeback with a handbag",
            listOf(itemRice, itemPoha),
            Price().apply {
                this.totalPrice = 125.0
            }
        ).apply {
            id = 1
        }
        val breakup1 = Breakup(
            bill, itemRice.id, 50.0f, itemRice.name, itemRice.unit, itemRice.price
        )
        val breakup2 = Breakup(
            bill, itemPoha.id, 5.0f, itemPoha.name, itemPoha.unit, itemPoha.price
        )

        AppDb.apply {
            userDao.insert(user)
            settingDao.insert(setting)
            metaDao.insert(meta)
            itemDao.insert(itemRice)
            itemDao.insert(itemPoha)
            billDao.insert(bill)
            breakupDao.insert(breakup1)
            breakupDao.insert(breakup2)
            inventoryDao.insert(Inventory(600, itemRice))
        }
        Log.d(TAG, "A mock insert test done")
    }

    fun insertTheUser(): User {
        val user = User(
            "grpranava",
            "81dc9bdb52d04dc20036dbd8313ed05",
            "Ranganna",
            "GajananaMill",
            "Talguppa",
            "9481350855",
            "GSTIN1234",
            "12345",
            true,
            Timestamp(0),
            2
        )
        AppDb.userDao.insert(user)
        return user
    }
}