package com.probill.ui

import com.probill.Constant
import com.probill.model.*
import com.probill.model.Unit
import com.probill.repository.db.AppDb
import com.probill.repository.net.api.AuthApi
import com.probill.service.ApiService
import com.probill.service.PrintService
import com.probill.utility.GeneralUtils.isNullOrEmpty
import com.probill.utility.Log
import com.probill.utility.RegexUtils
import javafx.application.Platform
import javafx.event.ActionEvent
import javafx.event.EventHandler
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.control.cell.PropertyValueFactory
import javafx.scene.input.KeyCode
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.stage.Stage
import javafx.util.Callback
import javafx.util.StringConverter
import kotlinx.coroutines.GlobalScope
import java.sql.Timestamp
import java.util.*
import kotlin.concurrent.timer

class MainWindow(var stage: Stage) : BaseUi() {

    val TAG = MainWindow::class.java.simpleName

    // New Bill Tab
    lateinit var nameField: TextField
    lateinit var phoneField: TextField
    lateinit var addressField: TextField
    lateinit var eSugamField: TextField
    lateinit var remarkFiled: TextField
    lateinit var quantityField: TextField

    lateinit var totalGstField: Label
    lateinit var totalSgstField: Label
    lateinit var totalDiscountField: Label
    lateinit var totalNetField: Label

    lateinit var gstBox: HBox
    lateinit var sgstBox: HBox
    lateinit var discountBox: HBox
    lateinit var netBox: HBox

    lateinit var unitField: Label
    lateinit var itemsList: ChoiceBox<Item>
    lateinit var table: TableView<Breakup>

    // View Tab
    lateinit var billTableView: TableView<Bill>
    lateinit var billSearchField: TextField

    // Manage Item Tab
    lateinit var itemNameField: TextField
    lateinit var itemRateField: TextField
    lateinit var itemDiscountField: TextField
    lateinit var itemGstField: TextField
    lateinit var itemSgstField: TextField
    lateinit var itemUnitField: ChoiceBox<Unit>
    lateinit var manageItemTableView: TableView<Item>

    private var itemsIndex = 0

    private var totalGst = 0.0
    private var totalSgst = 0.0
    private var totalDiscount = 0.0
    private var totalNet = 0.0

    private var user: User? = null
    private var lastBillNumber: Long = 0

    private var expiryTimer: Timer? = null
    private var pollTimer: Timer? = null

    data class Breakup(
        var item: Item,
        var id: Int,
        var name: String,
        var rate: Double,
        var qty: Float,
        var unit: String
    )

    fun initialize() {
        populateChoiceItems(AppDb.itemDao.readAll())
        initTable()
        populateTableData()
        updateTotals()
        lastBillNumber = AppDb.billDao.getLastBillId()
        AppDb.metaDao.getLastMeta()?.let {
            if (it.isLoggedIn) {
                user = AppDb.userDao.getByUsername(it.user.username)
                    ?: kotlin.run {
                        Platform.runLater {
                            stage.close()
                            openLogin()
                        }
                        return
                    }
                val expiryTime = user?.expiry?.time ?: Date().time
                expiryTimer = timer(
                    "expiry_thread",
                    false,
                    Date(expiryTime),
                    60 * 1000
                ) {
                    Platform.runLater {
                        stage.close()
                        openLogin()
                    }
                    pollTimer?.cancel()
                    this.cancel()
                }
                pollTimer = timer(
                    "god_field_thread",
                    false,
                    0,
                    user?.pollingInterval ?: (30 * 1000)
                ) {
                    ApiService.request<Any>(GlobalScope, {
                        val api = ApiService.getApiService(AuthApi::class.java)
                        val validate = api.validate(user?.username ?: "guest")
                        if (!validate.isValid) {
                            Platform.runLater {
                                stage.close()
                                openLogin()
                            }
                            expiryTimer?.cancel()
                            this.cancel()
                        }
                    }) { error ->
                        if (error.code == 401) {
                            Platform.runLater {
                                stage.close()
                                openLogin()
                            }
                            expiryTimer?.cancel()
                            this.cancel()
                        }
                        Log.e(TAG, "user validation polling failure")
                    }
                }
            } else {
                stage.close()
                openLogin()
            }
        }
    }

    private fun unitString(unit: String) = "(in %s)".format(unit)

    private fun populateChoiceItems(items: List<Item>) {
        itemsList.converter = object : StringConverter<Item>() {
            override fun toString(item: Item?): String? {
                return item?.name
            }

            override fun fromString(string: String?): Item? {
                return items.find { it.name == string }
            }
        }
        if (items.isNotEmpty()) {
            itemsList.items.addAll(items)
            itemsList.value = items.first()
            unitField.text = unitString(items.first().unit.name)
            itemsList.onAction = EventHandler {
                itemsList.value?.let {
                    unitField.text = unitString(it.unit.name)
                }
            }
        }

        itemUnitField.items.addAll(
            Unit.GRAM, Unit.KG, Unit.TON
        )
        itemUnitField.value = Unit.KG
    }

    private fun initTable() {
        // add items table
        val colId = TableColumn<Breakup, Int>("ID")
        val colName = TableColumn<Breakup, String>("NAME")
        val colRate = TableColumn<Breakup, Double>("RATE")
        val colQty = TableColumn<Breakup, Float>("QUANTITY")
        val colUnit = TableColumn<Breakup, String>("UNIT")

        colId.cellValueFactory = PropertyValueFactory("id")
        colName.cellValueFactory = PropertyValueFactory("name")
        colRate.cellValueFactory = PropertyValueFactory("rate")
        colQty.cellValueFactory = PropertyValueFactory("qty")
        colUnit.cellValueFactory = PropertyValueFactory("unit")
        table.columns.addAll(
            colId, colName, colRate, colQty, colUnit, deleteActionColumn()
        )
        table.columnResizePolicy = TableView.CONSTRAINED_RESIZE_POLICY
        table.isVisible = true

        // search bill table
        val colInvoiceId = TableColumn<Bill, Long>("ID")
        val colDate = TableColumn<Bill, Long>("Date")
        val colCustomerName = TableColumn<Bill, String>("CUSTOMER NAME")
        val colPhoneNumber = TableColumn<Bill, String>("CUSTOMER PHONE")

        colInvoiceId.cellValueFactory = PropertyValueFactory("id")
        colDate.cellValueFactory = PropertyValueFactory("createdAt")
        colCustomerName.cellValueFactory = PropertyValueFactory("name")
        colPhoneNumber.cellValueFactory = PropertyValueFactory("phone")
        billTableView.columns.addAll(
            colInvoiceId, colDate, colCustomerName, colPhoneNumber, showActionColumn()
        )
        billTableView.columnResizePolicy = TableView.CONSTRAINED_RESIZE_POLICY
        billTableView.isVisible = true

        // managed item table
        val colItemId = TableColumn<Item, Int>("ID")
        val colItemName = TableColumn<Item, String>("NAME")
        val colItemUnit = TableColumn<Item, String>("UNIT")
        val colItemGst = TableColumn<Item, Float>("GST(%)")
        val colItemSgst = TableColumn<Item, Float>("SGST(%)")
        val colItemDiscount = TableColumn<Item, Float>("DISC(%)")
        val colItemRate = TableColumn<Item, Double>("RATE")

        colItemId.cellValueFactory = PropertyValueFactory("id")
        colItemName.cellValueFactory = PropertyValueFactory("name")
        colItemUnit.cellValueFactory = PropertyValueFactory("unit")
        colItemGst.cellValueFactory = PropertyValueFactory("gst")
        colItemSgst.cellValueFactory = PropertyValueFactory("sGst")
        colItemDiscount.cellValueFactory = PropertyValueFactory("discount")
        colItemRate.cellValueFactory = PropertyValueFactory("totalPrice")
        manageItemTableView.columns.addAll(
            colItemId, colItemName, colItemUnit, colItemGst,
            colItemSgst, colItemDiscount, colItemRate, manageItemsActionColumn()
        )
        manageItemTableView.columnResizePolicy = TableView.CONSTRAINED_RESIZE_POLICY
        manageItemTableView.isVisible = true
    }

    private fun populateTableData() {
        billTableView.items.addAll(AppDb.billDao.readAll())
        billSearchField.setOnKeyReleased {
            if (it.code == KeyCode.ENTER) {
                onSearchClicked(null)
            }
        }

        manageItemTableView.items.addAll((AppDb.itemDao.readAll()))
    }

    private fun deleteActionColumn(): TableColumn<Breakup, Void> {
        val colBtn = TableColumn<Breakup, Void>("ACTION")
        colBtn.cellFactory =
            Callback<TableColumn<Breakup?, Void?>?, TableCell<Breakup?, Void?>?> {
                object : TableCell<Breakup?, Void?>() {
                    private val btn = Button("Remove")
                    override fun updateItem(item: Void?, empty: Boolean) {
                        super.updateItem(item, empty)
                        graphic = if (empty) {
                            null
                        } else {
                            btn
                        }
                    }

                    init {
                        btn.setOnAction { event: ActionEvent? ->
                            table.items.remove(table.items[index])
                            updateTotals()
                            recalculateBreakupIds()
                        }
                    }
                }
            }
        return colBtn
    }

    private fun showActionColumn(): TableColumn<Bill, Void> {
        val colBtn = TableColumn<Bill, Void>("ACTION")
        colBtn.cellFactory =
            Callback<TableColumn<Bill?, Void?>?, TableCell<Bill?, Void?>?> {
                object : TableCell<Bill?, Void?>() {
                    private lateinit var box: HBox
                    private val previewButton = Button("Preview")
                    private val printButton = Button("Print")
                    override fun updateItem(item: Void?, empty: Boolean) {
                        super.updateItem(item, empty)
                        graphic = if (empty) {
                            null
                        } else {
                            box = HBox(previewButton, printButton)
                            box.spacing = 10.0
                            box
                        }
                    }

                    init {
                        previewButton.setOnAction {
                            val bill = billTableView.items[index]
                            showPreview(bill, AppDb.breakupDao.getBreakupByBill(bill))
                        }
                        printButton.setOnAction {
                            printInit(billTableView.items[index])
                        }
                    }
                }
            }
        return colBtn
    }

    private fun manageItemsActionColumn(): TableColumn<Item, Void> {
        val colBtn = TableColumn<Item, Void>("ACTION")
        colBtn.cellFactory =
            Callback<TableColumn<Item?, Void?>?, TableCell<Item?, Void?>?> {
                object : TableCell<Item?, Void?>() {
                    private lateinit var box: HBox
                    private val modify = Button("Modify")
                    private val delete = Button("Delete")
                    override fun updateItem(item: Void?, empty: Boolean) {
                        super.updateItem(item, empty)
                        graphic = if (empty) {
                            null
                        } else {
                            box = HBox(modify, delete)
                            box.spacing = 10.0
                            box
                        }
                    }

                    init {
                        modify.setOnAction { event: ActionEvent? ->
                            val loader = FXMLLoader()
                            val stage = Stage()
                            loader.location =
                                javaClass.getResource("/update_item.fxml")
                            loader.setController(
                                UpdateItems(manageItemTableView.items[index]) {
                                    if (it) {
                                        actionOnItemModify()
                                        stage.close()
                                    }
                                }
                            )
                            val root = loader.load<Parent>()
                            root.style = "-fx-font-family: 'Verdana';"
                            val scene = Scene(root)
                            stage.scene = scene
                            stage.title = Constant.Screen.UPDATE_ITEM
                            stage.showAndWait()
                        }

                        delete.setOnAction { event: ActionEvent? ->
                            val alert = Alert(
                                Alert.AlertType.CONFIRMATION,
                                "Do you want to delete the Item "
                                    + "[${manageItemTableView.items[index].name}]",
                                ButtonType.OK, ButtonType.CANCEL
                            )
                            val result = alert.showAndWait().orElse(ButtonType.NO)
                            if (result == ButtonType.OK) {
                                actionOnItemDelete(manageItemTableView.items[index])
                            }
                        }
                    }
                }
            }
        return colBtn
    }

    private fun recalculateBreakupIds() {
        itemsIndex = 0
        table.items.map { breakup ->
            breakup.id = ++itemsIndex
            breakup
        }
    }

    fun onAddItem(action: ActionEvent) {
        val selectedItem = itemsList.value
        var selectedQty: Float = 0.0f
        if (quantityField.text.isNotEmpty()) {
            selectedQty = quantityField.text.toFloat()
        }
        if (selectedItem == null || selectedQty == 0.0f) {
            Alert(Alert.AlertType.ERROR, "Invalid input").showAndWait()
        } else {
            table.items.add(
                Breakup(
                    selectedItem,
                    ++itemsIndex,
                    selectedItem.name,
                    selectedItem.price.totalPrice,
                    selectedQty,
                    selectedItem.unit.name
                )
            )
            updateTotals()
        }
    }

    fun onSubmitClicked(action: ActionEvent) {
        if (!validateCustomerInfo()) {
            Alert(
                Alert.AlertType.ERROR,
                "Fill all input fields"
            ).showAndWait()
            return
        }
        if (table.items.isEmpty()) {
            Alert(
                Alert.AlertType.ERROR,
                "No items to create bill"
            ).showAndWait()
            return
        }
        val alert = Alert(
            Alert.AlertType.CONFIRMATION,
            "Do you want to generate bill",
            ButtonType.OK, ButtonType.CANCEL
        )
        val result = alert.showAndWait().orElse(ButtonType.NO)
        if (result == ButtonType.OK) {
            Thread {
                saveBill()?.let {
                    Platform.runLater {
                        onSearchClicked(null)
                    }
                    printInit(it)
                }
            }.start()
        }
    }

    fun onBatchItemClicked(action: ActionEvent) {
        val loader = FXMLLoader()
        val stage = Stage()
        loader.location = javaClass.getResource("/batch_entry.fxml")
        loader.setController(
            BatchEntry {
                table.items.addAll(it)
                recalculateBreakupIds()
                stage.close()
            }
        )
        val root = loader.load<Parent>()
        root.style = "-fx-font-family: 'Verdana';"
        val scene = Scene(root)
        stage.scene = scene
        stage.title = Constant.Screen.BATCH_ENTRY
        stage.isAlwaysOnTop = true
        stage.showAndWait()
    }

    fun onPreviewClicked(action: ActionEvent) {
        user?.let {
            val bill = getBillOfThePage(it)
            val breakups = table.items.map { brkUp ->
                com.probill.model.Breakup(
                    bill,
                    brkUp.item.id,
                    brkUp.qty,
                    brkUp.item.name,
                    brkUp.item.unit,
                    brkUp.item.price
                )
            }
            showPreview(bill, breakups)
        }
    }

    fun onSearchClicked(actionEvent: ActionEvent?) {
        billTableView.items.clear()
        billTableView.items.addAll(AppDb.billDao.readAll())
        if (billSearchField.text.isEmpty()) {
            return
        }
        val filteredList = billTableView.items.filter {
            it?.id == billSearchField.text.toLong()
        }
        billTableView.items.clear()
        billTableView.items.addAll(
            filteredList
        )
    }

    fun onItemAddClicked(actionEvent: ActionEvent?) {
        if (validateAddItemFields()) {
            val gst = itemGstField.text.trim().toDoubleOrNull() ?: 0.0
            val sgst = itemSgstField.text.trim().toDoubleOrNull() ?: 0.0
            val discount = itemDiscountField.text.trim().toDoubleOrNull() ?: 0.0
            val rate = itemRateField.text.trim().toDoubleOrNull() ?: 0.0
            Thread {
                AppDb.itemDao.insert(
                    Item(
                        itemNameField.text.trim(),
                        itemUnitField.value,
                        Price().apply {
                            this.gst = gst
                            this.sGst = sgst
                            this.discount = discount
                            this.totalPrice = rate
                        }
                    )
                )
                Platform.runLater {
                    actionOnItemModify()
                    resetManageItem()
                }
            }.start()
        } else {
            Alert(Alert.AlertType.ERROR, "Please enter valid data")
        }
    }

    fun onLogoutClicked(action: ActionEvent) {
        openLogin()
        expiryTimer?.cancel()
        pollTimer?.cancel()
        stage.close()
    }

    private fun actionOnItemModify() {
        val items = AppDb.itemDao.readAll()
        manageItemTableView.items.clear()
        manageItemTableView.items.addAll(AppDb.itemDao.readAll())
        itemsList.items.clear()
        populateChoiceItems(items)
    }

    private fun actionOnItemDelete(item: Item) {
        if (!AppDb.itemDao.delete(item)) {
            Alert(
                Alert.AlertType.ERROR,
                "Item has not deleted, please try again"
            ).showAndWait()
        } else {
            actionOnItemModify()
        }
    }

    private fun validateAddItemFields(): Boolean =
        !isNullOrEmpty(itemNameField.text) &&
            !isNullOrEmpty(itemUnitField.value.name) &&
            !isNullOrEmpty(itemGstField.text) &&
            !isNullOrEmpty(itemSgstField.text) &&
            !isNullOrEmpty(itemDiscountField.text) &&
            !isNullOrEmpty(itemRateField.text)

    private fun validateCustomerInfo(): Boolean =
        !isNullOrEmpty(nameField.text) &&
            !isNullOrEmpty(phoneField.text) &&
            RegexUtils.matchPhone(phoneField.text.trim()) &&
            !isNullOrEmpty(addressField.text) &&
            !isNullOrEmpty(eSugamField.text)

    private fun updateTotals() {
        totalGst = 0.0
        totalSgst = 0.0
        totalDiscount = 0.0
        totalNet = 0.0
        table.items.forEach {
            it?.let {
                calculateItemsAmount(
                    it.item.price.gst,
                    it.item.price.sGst,
                    it.item.price.discount,
                    it.item.price.totalPrice,
                    it.qty
                )
            }
        }
        gstBox.isVisible = totalGst != 0.0
        sgstBox.isVisible = totalSgst != 0.0
        discountBox.isVisible = totalDiscount != 0.0
        netBox.isVisible = totalNet != 0.0
        totalGstField.text = roundPrice(totalGst)
        totalSgstField.text = roundPrice(totalSgst)
        totalDiscountField.text = roundPrice(totalDiscount)
        totalNetField.text = roundPrice(totalNet)
    }

    private fun roundPrice(price: Double): String =
        "%.2f".format(price).toDouble().toString()

    private fun calculateItemsAmount(
        gst: Double, sGst: Double, discount: Double,
        rate: Double, qty: Float, calcTotal: Boolean = true
    ): Double {
        var total = 0.0
        total += (rate * qty)
        val d = total * (discount / 100.0)
        total -= d
        val g = total * (gst / 100.0)
        val s = total * (sGst / 100.0)
        total += g
        total += s
        if (calcTotal) {
            totalGst += g
            totalSgst += s
            totalDiscount += d
            totalNet += total
        }
        return total
    }

    private fun saveBill(): Bill? {
        val user = user ?: return null
        val now = AppDb.billDao.insert(
            getBillOfThePage(user)
        )
        AppDb.billDao
            .getBillByCreatedAt(user.username, now)?.let {
                table.items.forEach { brkUp ->
                    AppDb.breakupDao
                    AppDb.breakupDao
                        .insert(
                            com.probill.model.Breakup(
                                it,
                                brkUp.item.id,
                                brkUp.qty,
                                brkUp.item.name,
                                brkUp.item.unit,
                                brkUp.item.price
                            )
                        )
                }
                return it
            }
        return null
    }

    private fun getBillOfThePage(user: User): Bill {
        return Bill(
            user,
            nameField.text.trim(),
            phoneField.text.trim(),
            addressField.text.trim(),
            eSugamField.text.trim(),
            remarkFiled.text,
            table.items.map { brkUp -> brkUp.item },
            Price().apply {
                this.gst = totalGst
                this.sGst = totalSgst
                this.discount = totalDiscount
                this.totalPrice = totalNet
            }
        ).apply {
            this.id = lastBillNumber + 1
            this.createdAt = Timestamp.from(Date().toInstant())
        }
    }

    private fun showPreview(
        bill: Bill,
        breakups: List<com.probill.model.Breakup>
    ) {
        Platform.runLater {
            val node = Invoice.createInvoice(
                bill = bill,
                offset = 0,
                carry = 0.0,
                isLast = true,
                isPreview = true,
                breakups = breakups
            )
            val scene = Scene(node)
            val stage = Stage()
            stage.scene = scene
            stage.title = Constant.Screen.PREVIEW
            stage.showAndWait()
        }
    }

    private fun printInit(bill: Bill) {
        val papers = paperRequired(bill)
        val breakups = AppDb.breakupDao.getBreakupByBill(bill)
        Platform.runLater {
            var offset = 0
            var carry = 0.0
            while (offset < papers) {
                val box = VBox()
                carry =
                    if (offset != 0)
                        getCarryPrice(
                            breakups, offset - 1
                        ) + carry
                    else 0.0
                box.spacing = 50.0
                box.children.addAll(
                    Invoice.createInvoice(
                        bill,
                        offset,
                        carry,
                        offset == papers - 1
                    ),
                    Invoice.createInvoice(
                        bill,
                        offset,
                        carry,
                        offset == papers - 1
                    )
                )
                PrintService().print(box)
                offset++
            }
            reset()
        }
    }

    private fun getCarryPrice(
        breakups: List<com.probill.model.Breakup>,
        offset: Int
    ): Double {
        var total = 0.0
        var start = offset * Invoice.MAX_ITEM_COUNT
        var end = start + Invoice.MAX_ITEM_COUNT
        if (end > breakups.size) end = breakups.size
        while (start < end) {
            val breakup = breakups[start]
            total += calculateItemsAmount(
                breakup.price.gst,
                breakup.price.sGst,
                breakup.price.discount,
                breakup.price.totalPrice,
                breakup.quantity
            )
            start++
        }
        return total
    }

    private fun paperRequired(bill: Bill): Int {
        AppDb.breakupDao.getBreakupByBill(bill).let {
            val remainder = it.size % Invoice.MAX_ITEM_COUNT
            return (it.size / Invoice.MAX_ITEM_COUNT) +
                if (remainder > 0) 1 else 0
        }
    }

    private fun reset() {
        nameField.text = ""
        phoneField.text = ""
        phoneField.text = ""
        addressField.text = ""
        eSugamField.text = ""
        remarkFiled.text = ""
        quantityField.text = ""
        totalGstField.text = ""
        totalSgstField.text = ""
        totalDiscountField.text = ""
        totalNetField.text = ""
        gstBox.isVisible = false
        sgstBox.isVisible = false
        discountBox.isVisible = false
        netBox.isVisible = false
        unitField.text = ""
        table.items.clear()
        itemsIndex = 0
        totalGst = 0.0
        totalSgst = 0.0
        totalDiscount = 0.0
        totalNet = 0.0
        lastBillNumber = AppDb.billDao.getLastBillId()
        Log.i(TAG, "Bill UI reset done")
    }

    private fun resetManageItem() {
        itemNameField.text = ""
        itemGstField.text = ""
        itemSgstField.text = ""
        itemDiscountField.text = ""
        itemRateField.text = ""
    }
}
