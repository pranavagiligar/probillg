package com.probill.ui

import com.probill.model.Item
import com.probill.model.Unit
import com.probill.repository.db.AppDb
import com.probill.service.ApiService
import com.probill.utility.GeneralUtils
import javafx.event.ActionEvent
import javafx.fxml.Initializable
import javafx.scene.control.ChoiceBox
import javafx.scene.control.TextField
import java.net.URL
import java.util.*
import com.probill.utility.GeneralUtils.isNullOrEmpty
import javafx.scene.control.Alert

class UpdateItems(val item: Item, val callback: (success: Boolean) -> kotlin.Unit = {}): Initializable {
    lateinit var itemNameField: TextField
    lateinit var itemRateField: TextField
    lateinit var itemDiscountField: TextField
    lateinit var itemGstField: TextField
    lateinit var itemSgstField: TextField
    lateinit var itemUnitField: ChoiceBox<Unit>

    override fun initialize(location: URL?, resources: ResourceBundle?) {
        itemNameField.text = item.name
        itemRateField.text = item.price.totalPrice.toString()
        itemDiscountField.text = item.price.discount.toString()
        itemGstField.text = item.price.gst.toString()
        itemSgstField.text = item.price.sGst.toString()

        itemUnitField.items.addAll(
            Unit.GRAM, Unit.KG, Unit.TON
        )
        itemUnitField.value = item.unit

        itemNameField.isEditable = false
    }

    fun onUpdateClicked(actionEvent: ActionEvent?) {
        if (validateAddItemFields()) {
            val gst = itemGstField.text.trim().toDoubleOrNull() ?: 0.0
            val sgst = itemSgstField.text.trim().toDoubleOrNull() ?: 0.0
            val discount = itemDiscountField.text.trim().toDoubleOrNull() ?: 0.0
            val rate = itemRateField.text.trim().toDoubleOrNull() ?: 0.0
            val success = AppDb.itemDao.update(
                item.apply {
                    this.name = itemNameField.text.trim()
                    this.unit = itemUnitField.value
                    item.price.gst = gst
                    item.price.sGst = sgst
                    item.price.discount = discount
                    item.price.totalPrice = rate
                }
            )
            if (success) {
                Alert(
                    Alert.AlertType.INFORMATION,
                    "Update done"
                ).showAndWait()
            } else {
                Alert(
                    Alert.AlertType.ERROR,
                    "Update failed, Please try again"
                ).showAndWait()
            }
            callback(success)
        }
    }

    private fun validateAddItemFields(): Boolean =
            !isNullOrEmpty(itemUnitField.value.name) &&
            !isNullOrEmpty(itemGstField.text) &&
            !isNullOrEmpty(itemSgstField.text) &&
            !isNullOrEmpty(itemDiscountField.text) &&
            !isNullOrEmpty(itemRateField.text)
}