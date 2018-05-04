package com.hendraanggrian.openpss.ui.invoice

import com.hendraanggrian.openpss.R
import com.hendraanggrian.openpss.controls.ViewInvoiceDialog
import com.hendraanggrian.openpss.db.SessionWrapper
import com.hendraanggrian.openpss.db.schemas.Customer
import com.hendraanggrian.openpss.db.schemas.Customers
import com.hendraanggrian.openpss.db.schemas.Employee.Role.MANAGER
import com.hendraanggrian.openpss.db.schemas.Employees
import com.hendraanggrian.openpss.db.schemas.Invoice
import com.hendraanggrian.openpss.db.schemas.Invoices
import com.hendraanggrian.openpss.db.schemas.Invoices.customerId
import com.hendraanggrian.openpss.db.schemas.Payment
import com.hendraanggrian.openpss.db.schemas.Payments
import com.hendraanggrian.openpss.db.schemas.Payments.invoiceId
import com.hendraanggrian.openpss.db.transaction
import com.hendraanggrian.openpss.io.properties.SettingsFile.INVOICE_PAGINATION_ITEMS
import com.hendraanggrian.openpss.layouts.DateBox
import com.hendraanggrian.openpss.ui.Refreshable
import com.hendraanggrian.openpss.ui.SegmentedController
import com.hendraanggrian.openpss.ui.Selectable
import com.hendraanggrian.openpss.ui.Selectable2
import com.hendraanggrian.openpss.util.PATTERN_DATETIME_EXTENDED
import com.hendraanggrian.openpss.util.controller
import com.hendraanggrian.openpss.util.currencyCell
import com.hendraanggrian.openpss.util.doneCell
import com.hendraanggrian.openpss.util.getResource
import com.hendraanggrian.openpss.util.getStyle
import com.hendraanggrian.openpss.util.matches
import com.hendraanggrian.openpss.util.pane
import com.hendraanggrian.openpss.util.stringCell
import com.hendraanggrian.openpss.util.yesNoAlert
import javafx.beans.property.SimpleObjectProperty
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Node
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.MenuButton
import javafx.scene.control.MenuItem
import javafx.scene.control.Pagination
import javafx.scene.control.RadioButton
import javafx.scene.control.RadioMenuItem
import javafx.scene.control.SelectionModel
import javafx.scene.control.SplitMenuButton
import javafx.scene.control.SplitPane
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView
import javafx.scene.control.TableView.CONSTRAINED_RESIZE_POLICY
import javafx.scene.image.ImageView
import javafx.scene.layout.Pane
import javafx.stage.Modality.APPLICATION_MODAL
import javafx.util.Callback
import kotlinx.nosql.equal
import kotlinx.nosql.update
import ktfx.application.later
import ktfx.beans.binding.bindingOf
import ktfx.beans.binding.stringBindingOf
import ktfx.beans.binding.times
import ktfx.beans.property.toReadOnlyProperty
import ktfx.beans.value.or
import ktfx.collections.emptyObservableList
import ktfx.collections.toMutableObservableList
import ktfx.collections.toObservableList
import ktfx.coroutines.onAction
import ktfx.coroutines.onMouseClicked
import ktfx.layouts.button
import ktfx.layouts.columns
import ktfx.layouts.separator
import ktfx.layouts.styledScene
import ktfx.layouts.tableView
import ktfx.layouts.tooltip
import ktfx.scene.input.isDoubleClick
import ktfx.stage.stage
import java.net.URL
import java.util.ResourceBundle
import kotlin.math.ceil

class InvoiceController : SegmentedController(), Refreshable, Selectable<Invoice>, Selectable2<Payment> {

    @FXML lateinit var customerButton: SplitMenuButton
    @FXML lateinit var customerButtonItem: MenuItem
    @FXML lateinit var paymentButton: MenuButton
    @FXML lateinit var anyPaymentItem: RadioMenuItem
    @FXML lateinit var unpaidPaymentItem: RadioMenuItem
    @FXML lateinit var paidPaymentItem: RadioMenuItem
    @FXML lateinit var allDateRadio: RadioButton
    @FXML lateinit var pickDateRadio: RadioButton
    @FXML lateinit var dateBox: DateBox
    @FXML lateinit var splitPane: SplitPane
    @FXML lateinit var paymentPane: Pane
    @FXML lateinit var invoicePagination: Pagination
    @FXML lateinit var paymentTable: TableView<Payment>
    @FXML lateinit var paymentDateTimeColumn: TableColumn<Payment, String>
    @FXML lateinit var paymentEmployeeColumn: TableColumn<Payment, String>
    @FXML lateinit var paymentValueColumn: TableColumn<Payment, String>
    @FXML lateinit var paymentMethodColumn: TableColumn<Payment, String>
    @FXML lateinit var addPaymentItem: MenuItem
    @FXML lateinit var deletePaymentItem: MenuItem
    @FXML lateinit var coverLabel: Label

    private lateinit var refreshButton: Button
    private lateinit var addButton: Button
    private lateinit var editButton: Button
    private lateinit var deleteButton: Button
    private lateinit var viewInvoiceButton: Button
    override val leftSegment: List<Node>
        get() = listOf(refreshButton, separator(), addButton, editButton, deleteButton, separator(), viewInvoiceButton)

    private lateinit var platePriceButton: Button
    private lateinit var offsetPriceButton: Button
    override val rightSegment: List<Node> get() = listOf(platePriceButton, offsetPriceButton)

    private val customerProperty = SimpleObjectProperty<Customer>()
    private lateinit var invoiceTable: TableView<Invoice>

    override fun initialize(location: URL, resources: ResourceBundle) {
        super.initialize(location, resources)
        refreshButton = button(graphic = ImageView(R.image.btn_refresh)) {
            tooltip(getString(R.string.refresh))
            onAction { refresh() }
        }
        addButton = button(graphic = ImageView(R.image.btn_add)) {
            tooltip(getString(R.string.add_invoice))
            onAction { addInvoice() }
        }
        editButton = button(graphic = ImageView(R.image.btn_edit)) {
            tooltip(getString(R.string.edit_invoice))
            onAction { editInvoice() }
        }
        deleteButton = button(graphic = ImageView(R.image.btn_delete)) {
            tooltip(getString(R.string.delete_invoice))
            onAction { deleteInvoice() }
        }
        viewInvoiceButton = button(graphic = ImageView(R.image.btn_invoice)) {
            tooltip(getString(R.string.view_invoice))
            onAction { viewInvoice() }
        }
        platePriceButton = button(getString(R.string.plate_price)) { onAction { platePrice() } }
        offsetPriceButton = button(getString(R.string.offset_price)) { onAction { offsetPrice() } }
        paymentPane.minHeightProperty().bind(splitPane.heightProperty() * 0.2)

        customerButton.textProperty().bind(stringBindingOf(customerProperty) {
            customerProperty.value?.toString() ?: getString(R.string.search_customer)
        })
        customerButtonItem.disableProperty().bind(customerProperty.isNull)
        paymentButton.textProperty().bind(stringBindingOf(
            anyPaymentItem.selectedProperty(),
            unpaidPaymentItem.selectedProperty(),
            paidPaymentItem.selectedProperty()) {
            getString(when {
                unpaidPaymentItem.isSelected -> R.string.unpaid
                paidPaymentItem.isSelected -> R.string.paid
                else -> R.string.any
            })
        })
        pickDateRadio.graphic.disableProperty().bind(!pickDateRadio.selectedProperty())

        paymentDateTimeColumn.stringCell { dateTime.toString(PATTERN_DATETIME_EXTENDED) }
        paymentEmployeeColumn.stringCell { transaction { Employees[employeeId].single() } }
        paymentValueColumn.currencyCell { value }
        paymentMethodColumn.stringCell { typedMethod.toString(this@InvoiceController) }
    }

    override fun refresh() = later {
        invoicePagination.pageFactoryProperty().bind(bindingOf(customerProperty,
            anyPaymentItem.selectedProperty(), unpaidPaymentItem.selectedProperty(), paidPaymentItem.selectedProperty(),
            allDateRadio.selectedProperty(), pickDateRadio.selectedProperty(), dateBox.valueProperty) {
            Callback<Int, Node> { page ->
                invoiceTable = tableView {
                    columnResizePolicy = CONSTRAINED_RESIZE_POLICY
                    columns {
                        getString(R.string.id)<String> { stringCell { no } }
                        getString(R.string.date)<String> { stringCell { dateTime.toString(PATTERN_DATETIME_EXTENDED) } }
                        getString(R.string.employee)<String> {
                            stringCell { transaction { Employees[employeeId].single() } }
                        }
                        getString(R.string.customer)<String> {
                            stringCell { transaction { Customers[customerId].single() } }
                        }
                        getString(R.string.total)<String> { currencyCell { total } }
                        getString(R.string.print)<Boolean> { doneCell { printed } }
                        getString(R.string.paid)<Boolean> { doneCell { paid } }
                        getString(R.string.done)<Boolean> { doneCell { done } }
                    }
                    onMouseClicked { if (it.isDoubleClick() && selected != null) viewInvoice() }
                    later {
                        transaction {
                            val invoices = Invoices.buildQuery {
                                if (customerProperty.value != null) and(customerId.equal(customerProperty.value.id))
                                if (unpaidPaymentItem.isSelected) and(it.paid.equal(false))
                                if (paidPaymentItem.isSelected) and(it.paid.equal(true))
                                if (pickDateRadio.isSelected) and(it.dateTime.matches(dateBox.value))
                            }
                            invoicePagination.pageCount =
                                ceil(invoices.count() / INVOICE_PAGINATION_ITEMS.toDouble()).toInt()
                            items = invoices
                                .skip(INVOICE_PAGINATION_ITEMS * page)
                                .take(INVOICE_PAGINATION_ITEMS).toMutableObservableList()
                            val fullAccess = login.isAtLeast(MANAGER).toReadOnlyProperty()
                            editButton.disableProperty().bind(!selectedBinding or !fullAccess)
                            deleteButton.disableProperty().bind(!selectedBinding or !fullAccess)
                            viewInvoiceButton.disableProperty().bind(!selectedBinding)
                            addPaymentItem.disableProperty().bind(!selectedBinding)
                            deletePaymentItem.disableProperty().bind(!selectedBinding2 or !fullAccess)
                        }
                    }
                }
                paymentTable.itemsProperty().bind(bindingOf(invoiceTable.selectionModel.selectedItemProperty()) {
                    if (selected == null) emptyObservableList()
                    else transaction { Payments { invoiceId.equal(selected!!.id) }.toObservableList() }
                })
                coverLabel.visibleProperty().bind(invoiceTable.selectionModel.selectedItemProperty().isNull)
                invoiceTable
            }
        })
    }

    override val selectionModel: SelectionModel<Invoice> get() = invoiceTable.selectionModel

    override val selectionModel2: SelectionModel<Payment> get() = paymentTable.selectionModel

    fun addInvoice() = InvoiceDialog(this, employee = login).showAndWait().ifPresent {
        transaction {
            it.id = Invoices.insert(it)
            invoiceTable.items.add(it)
            invoiceTable.selectionModel.selectFirst()
        }
    }

    private fun editInvoice() = InvoiceDialog(this@InvoiceController, selected!!)
        .showAndWait()
        .ifPresent {
            transaction {
                Invoices[it]
                    .projection { plates + offsets + others + note + paid }
                    .update(it.plates, it.offsets, it.others, it.note, it.calculateDue() <= 0.0)
                reload(it)
            }
        }

    private fun deleteInvoice() = yesNoAlert {
        transaction {
            Invoices -= selected!!
            Payments { invoiceId.equal(selected!!.id) }.remove()
        }
        invoiceTable.items.remove(selected)
    }

    @FXML fun viewInvoice() = ViewInvoiceDialog(this, selected!!).show()

    @FXML fun addPayment() = AddPaymentDialog(this, login, selected!!).showAndWait().ifPresent {
        transaction {
            Payments += it
            updatePaymentStatus()
            reload(selected!!)
        }
    }

    @FXML fun deletePayment() = yesNoAlert {
        transaction {
            Payments -= selected2!!
            updatePaymentStatus()
            reload(selected!!)
        }
    }

    @FXML fun selectCustomer() = SearchCustomerDialog(this).showAndWait().ifPresent { customerProperty.set(it) }

    @FXML fun clearCustomer() = customerProperty.set(null)

    private fun platePrice() = stage(getString(R.string.plate_price)) {
        initModality(APPLICATION_MODAL)
        val loader = FXMLLoader(getResource(R.layout.controller_price_plate), resources)
        scene = styledScene(getStyle(R.style.openpss), loader.pane)
        isResizable = false
        loader.controller.login = login
    }.showAndWait()

    private fun offsetPrice() = stage(getString(R.string.offset_price)) {
        initModality(APPLICATION_MODAL)
        val loader = FXMLLoader(getResource(R.layout.controller_price_offset), resources)
        scene = styledScene(getStyle(R.style.openpss), loader.pane)
        isResizable = false
        loader.controller.login = login
    }.showAndWait()

    private fun SessionWrapper.updatePaymentStatus() = Invoices[selected!!]
        .projection { Invoices.paid }
        .update(selected!!.calculateDue() <= 0.0)

    private fun SessionWrapper.reload(invoice: Invoice) = invoiceTable.run {
        items.indexOf(invoice).let { index ->
            items[index] = Invoices[invoice].single()
            selectionModel.select(index)
        }
    }
}