package com.probill.ui

import com.probill.model.Breakup
import com.probill.model.Item
import com.probill.repository.db.AppDb
import javafx.event.ActionEvent
import javafx.fxml.Initializable
import javafx.scene.control.*
import javafx.scene.control.cell.PropertyValueFactory
import javafx.util.Callback
import java.net.URL
import java.util.*

class BatchEntry(val callback: (List<MainWindow.Breakup>) -> Unit) : Initializable {

    lateinit var tableView: TableView<Breakup>
    lateinit var applyButton: Button

    override fun initialize(location: URL?, resources: ResourceBundle?) {
        val items = AppDb.itemDao.readAll()
        var counter = 1
        tableView.items.addAll(
            items.map {
                Breakup(
                    it,
                    counter++,
                    it.name,
                    it.price.totalPrice,
                    0.0f,
                    it.unit.name,
                    false
                )
            }
        )
        initTable()
        tableView.isVisible = true
        applyButton.setOnAction {
            onApplyClicked(it)
        }
    }

    private fun initTable() {
        val colId = TableColumn<Breakup, Int>("ID")
        val colName = TableColumn<Breakup, String>("NAME")
        val colRate = TableColumn<Breakup, Double>("RATE")
        val colUnit = TableColumn<Breakup, String>("UNIT")

        colId.cellValueFactory = PropertyValueFactory("id")
        colName.cellValueFactory = PropertyValueFactory("name")
        colRate.cellValueFactory = PropertyValueFactory("rate")
        colUnit.cellValueFactory = PropertyValueFactory("unit")

        tableView.columns.addAll(
            colId, colName, colRate, colUnit, qtyColumn(), choiceColumn()
        )
        tableView.columnResizePolicy = TableView.CONSTRAINED_RESIZE_POLICY
        tableView.isVisible = true
    }

    private fun onApplyClicked(action: ActionEvent) {
        callback(tableView.items.filtered { it.enabled && it.qty > 0.0f }.map {
            MainWindow.Breakup(
                it.item,
                it.id,
                it.name,
                it.rate,
                it.qty,
                it.unit
            )
        })
    }

    private fun choiceColumn(): TableColumn<Breakup?, Void> {
        val colBtn = TableColumn<Breakup?, Void>("Select")
        colBtn.cellFactory =
            Callback<TableColumn<Breakup?, Void?>?, TableCell<Breakup?, Void?>?> {
                object : TableCell<Breakup?, Void?>() {
                    private val checkbox = CheckBox()
                    override fun updateItem(item: Void?, empty: Boolean) {
                        super.updateItem(item, empty)
                        graphic = if (empty) {
                            null
                        } else {
                            checkbox.isSelected = tableView.items[index]?.enabled ?: false
                            checkbox
                        }
                    }

                    init {
                        checkbox.setOnAction { event: ActionEvent? ->
                            tableView.items[index]?.enabled = checkbox.isSelected
                            tableView.refresh()
                        }
                    }
                }
            }
        return colBtn
    }

    private fun qtyColumn(): TableColumn<Breakup?, Void> {
        val colBtn = TableColumn<Breakup?, Void>("QTY")
        colBtn.cellFactory =
            Callback<TableColumn<Breakup?, Void?>?, TableCell<Breakup?, Void?>?> {
                object : TableCell<Breakup?, Void?>() {
                    private val textField = TextField()
                    override fun updateItem(item: Void?, empty: Boolean) {
                        super.updateItem(item, empty)
                        graphic = if (empty) {
                            null
                        } else {
                            textField.promptText = "0.0"
                            textField.isEditable = tableView.items[index]?.enabled ?: false
                            if (textField.isEditable) {
                                if (tableView.items[index]?.qty != 0.0f) {
                                    textField.text = tableView.items[index]?.qty.toString()
                                }
                            } else {
                                textField.text = ""
                            }
                            textField.setOnKeyReleased {
                                tableView.items[index]?.qty = textField.text.toFloat()
                            }
                            textField
                        }
                    }
                }
            }
        return colBtn
    }

    data class Breakup(
        var item: Item,
        var id: Int,
        var name: String,
        var rate: Double,
        var qty: Float,
        var unit: String,
        var enabled: Boolean
    )
}