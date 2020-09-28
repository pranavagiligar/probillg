package com.probill.ui

import com.probill.model.Bill
import com.probill.model.Price
import com.probill.repository.db.AppDb
import com.probill.utility.DateTimeUtils
import javafx.fxml.FXMLLoader
import javafx.fxml.Initializable
import javafx.scene.Parent
import javafx.scene.control.Control
import javafx.scene.control.TableCell
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView
import javafx.scene.control.cell.PropertyValueFactory
import javafx.scene.layout.BorderPane
import javafx.scene.text.Text
import javafx.util.Callback
import java.net.URL
import java.util.*

class Invoice(
    private val bill: Bill,
    private var offset: Int,
    private val carry: Double,
    private val isLast: Boolean,
    private val isPreview: Boolean = false
) : Initializable {

    companion object {
        // TODO: Make it Configurable by settings
        const val MAX_ITEM_COUNT = 4

        fun createInvoice(
            bill: Bill, offset: Int,
            carry: Double, isLast: Boolean,
            isPreview: Boolean = false,
            breakups: List<com.probill.model.Breakup>? = null
        ): Parent {
            val loader = FXMLLoader()
            loader.location = javaClass.getResource("/invoice.fxml")
            loader.setController(
                Invoice(bill, offset, carry, isLast, isPreview).apply {
                    this.breakups = breakups
                }
            )
            val root = loader.load<Parent>()
            root.style = "-fx-font-family: 'Verdana';"
            return root
        }
    }

    lateinit var pageNumberText: Text
    lateinit var companyName: Text
    lateinit var address: Text
    lateinit var phone: Text
    lateinit var gstNumber: Text
    lateinit var customerName: Text
    lateinit var customerAddress: Text
    lateinit var customerPhone: Text

    lateinit var invoiceNumber: Text
    lateinit var invoiceDate: Text
    lateinit var eSugamNumber: Text
    lateinit var purchaseOrder: Text     // TODO: implement
    lateinit var purchaseOrderDate: Text // TODO: implement
    lateinit var remark: Text

    lateinit var carryPane: BorderPane
    lateinit var carryForwardPrice: Text
    lateinit var tableView: TableView<Breakup>
    lateinit var pageTotal: Text
    lateinit var carryForwardLabel: Text
    lateinit var cGstText: Text
    lateinit var sGstText: Text
    lateinit var discount: Text
    lateinit var netAmount: Text

    lateinit var amountInWords: Text // TODO: implement
    lateinit var disclaimer: Text    // TODO: implement

    private var totalGst = 0.0
    private var totalSgst = 0.0
    private var totalDiscount = 0.0
    private var totalAmount = 0.0

    var breakups: List<com.probill.model.Breakup>? = null

    data class Breakup(
        var serial: Int,
        var description: String,
        var quantity: Float,
        var rate: Double,
        var unit: String,
        var gstInfo: String,
        var amount: Double
    )

    override fun initialize(location: URL?, resources: ResourceBundle?) {
        pageNumberText.text = (offset + 1).toString()
        val infoFormat = "%-8s" // Max Length is from Company
        companyName.text = "$infoFormat: ${bill.createdBy.company}".format("Company")
        address.text = "$infoFormat: ${bill.createdBy.address}".format("Address")
        phone.text = "$infoFormat: ${bill.createdBy.phone}".format("Phone")
        gstNumber.text = "$infoFormat: ${bill.createdBy.gstNumber}".format("GST No")
        customerName.text = "$infoFormat: ${bill.name}".format("Customer name")
        customerAddress.text = "$infoFormat: ${bill.address}".format("Customer address")
        customerPhone.text = "$infoFormat: ${bill.phone}".format("Customer phone")

        invoiceNumber.text = "Invoice ID: ${bill.id}"
        invoiceDate.text = "Date: ${DateTimeUtils.time(bill.createdAt)}"
        eSugamNumber.text = "E-Sugam Number: ${bill.eSugamNumber}"
        remark.text = "Remark: ${bill.remark}"

        carryForwardPrice.text = "Brought forward: ${roundPrice(carry)}"
        carryPane.isVisible = carry != 0.0
        var pageTotalAmount = 0.0
        val breakups = calculateBreakup()

        breakups.forEach {
            pageTotalAmount += it.amount
        }
        pageTotal.text = "${pageTotal.text}${roundPrice(pageTotalAmount)}"

        initTable(breakups)
        if (isLast) {
            carryForwardLabel.isVisible = false
            pageTotal.isVisible = false
            val priceFormat = "%-11s" // Max Length is from New Amount text
            cGstText.text = "$priceFormat: ${roundPrice(totalGst)}".format("CGST")
            sGstText.text = "$priceFormat: ${roundPrice(totalSgst)}".format("SGST")
            discount.text = "$priceFormat: ${roundPrice(totalDiscount)}".format("Discount")
            netAmount.text = "$priceFormat: ${roundPrice(totalAmount)}".format("Net Amount")
        } else {
            cGstText.isVisible = false
            sGstText.isVisible = false
            discount.isVisible = false
            netAmount.isVisible = false
        }
        if (isPreview) pageNumberText.isVisible = false
    }

    private fun initTable(breakups: List<Breakup>) {
        tableView.columnResizePolicy = TableView.CONSTRAINED_RESIZE_POLICY
//        tableView.visibleRowCountProperty().value = breakups.size

        val colId = TableColumn<Breakup, Int>("SN")
        val colDesc = TableColumn<Breakup, String>("DESC")
        val colQty = TableColumn<Breakup, Float>("QTY")
        val colRate = TableColumn<Breakup, Double>("RATE")
        val colUnit = TableColumn<Breakup, String>("UOM")
        val colVariable = TableColumn<Breakup, String>("VAR")
        val colAmount = TableColumn<Breakup, Double>("AMT")

        colId.cellValueFactory = PropertyValueFactory("serial")
        colDesc.cellValueFactory = PropertyValueFactory("description")
        colQty.cellValueFactory = PropertyValueFactory("quantity")
        colRate.cellValueFactory = PropertyValueFactory("rate")
        colUnit.cellValueFactory = PropertyValueFactory("unit")
        colVariable.cellValueFactory = PropertyValueFactory("gstInfo")
        colAmount.cellValueFactory = PropertyValueFactory("amount")

        alignTableColumn(colId)
        alignTableColumn(colUnit)
        alignTableColumn(colDesc, "CENTER-LEFT")
        alignTableColumn(colQty, "CENTER-RIGHT")
        alignTableColumn(colRate, "CENTER-RIGHT")
        alignTableColumn(colAmount, "CENTER-RIGHT")
        colDesc.setCellFactory { wrapColumnText(it) }
        colVariable.setCellFactory { wrapColumnText(it) }

        tableView.columns.addAll(
            colId, colDesc, colQty, colRate, colUnit, colVariable, colAmount
        )

        tableView.items.addAll(breakups)
    }

    private fun wrapColumnText(it: TableColumn<Breakup, String>): TableCell<Breakup, String> {
        val cell = TableCell<Breakup, String>()
        val text = Text()
        cell.graphic = text
        cell.prefHeight = Control.USE_COMPUTED_SIZE
        text.wrappingWidthProperty().bind(it.widthProperty())
        text.textProperty().bind(cell.itemProperty())
        return cell
    }

    private fun <S, T> alignTableColumn(value: TableColumn<S, T>, alignment: String = "CENTER") {
        value.cellFactory = Callback<TableColumn<S?, T?>?, TableCell<S?, T?>?>(
            fun(_: TableColumn<S?, T?>?): TableCell<S?, T?> {
                val cell = object : TableCell<S?, T?>() {
                    override fun updateItem(item: T?, empty: Boolean) {
                        super.updateItem(item, empty)
                        text = if (empty) null else string
                        graphic = null
                    }

                    private val string: String?
                        get() = if (item == null) "" else item.toString()

                }
                cell.style = "-fx-alignment: $alignment;"
                return cell
            })
    }

    private fun calculateBreakup(): List<Breakup> {
        val breakups = arrayListOf<Breakup>()
        var breakup = AppDb.breakupDao.getBreakupByBill(bill)
        this.breakups?.let {
            if (breakup.isEmpty()) {
                breakup = it
            }
        }
        breakup.forEach {
            calculateItemsAmount(
                it.price.gst,
                it.price.sGst,
                it.price.discount,
                it.price.totalPrice,
                it.quantity
            )
        }
        var start = offset * MAX_ITEM_COUNT
        var end = start + MAX_ITEM_COUNT
        if (end > breakup.size) end = breakup.size
        if (isPreview) {
            start = 0
            end = breakup.size
        }
        while (start < end) {
            val it = breakup[start]
            breakups.add(
                Breakup(
                    serial = start + 1,
                    description = it.name,
                    quantity = it.quantity,
                    rate = it.price.totalPrice,
                    unit = it.unit.toString(),
                    gstInfo = getGstString(it.price),
                    amount = roundPrice(
                        calculateItemAmount(
                            it.price.gst,
                            it.price.sGst,
                            it.price.discount,
                            it.price.totalPrice,
                            it.quantity
                        )
                    )
                )
            )
            start++
        }
        return breakups
    }

    private fun getGstString(price: Price) =
        "CGST @ ${price.gst}%, SGST @ ${price.sGst}%, Discount=${price.discount}"

    private fun roundPrice(price: Double): Double =
        "%.2f".format(price).toDouble()

    private fun calculateItemsAmount(
        gst: Double, sGst: Double, discount: Double,
        rate: Double, qty: Float
    ): Double {
        var total = 0.0
        total += (rate * qty)
        val d = total * (discount / 100.0)
        total -= d
        val g = total * (gst / 100.0)
        val s = total * (sGst / 100.0)
        total += g
        total += s
        totalGst += g
        totalSgst += s
        totalDiscount += d
        totalAmount += total
        return total
    }

    private fun calculateItemAmount(
        gst: Double, sGst: Double, discount: Double, rate: Double, qty: Float
    ): Double {
        var total = 0.0
        total += (rate * qty)
        val d = total * (discount / 100.0)
        total -= d
        val g = total * (gst / 100.0)
        val s = total * (sGst / 100.0)
        total += g
        total += s
        return total
    }
}