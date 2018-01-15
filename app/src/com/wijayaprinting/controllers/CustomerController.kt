package com.wijayaprinting.controllers

import com.wijayaprinting.App.Companion.FULL_ACCESS
import com.wijayaprinting.PATTERN_DATE
import com.wijayaprinting.R
import com.wijayaprinting.collections.isNotEmpty
import com.wijayaprinting.collections.minus
import com.wijayaprinting.collections.plus
import com.wijayaprinting.nosql.Customer
import com.wijayaprinting.nosql.Customers
import com.wijayaprinting.nosql.transaction
import com.wijayaprinting.util.gap
import com.wijayaprinting.util.size
import javafx.fxml.FXML
import javafx.scene.Node
import javafx.scene.control.*
import javafx.scene.control.ButtonType.CANCEL
import javafx.scene.control.ButtonType.OK
import javafx.scene.image.ImageView
import javafx.scene.text.Font.loadFont
import javafx.util.Callback
import kotfx.*
import kotlinx.nosql.equal
import kotlinx.nosql.id
import kotlinx.nosql.mongodb.MongoDBSession
import kotlinx.nosql.update
import kotlin.math.ceil

class CustomerController : Controller() {

    companion object {
        private const val ITEMS_PER_PAGE = 20
    }

    @FXML lateinit var customerField: TextField
    @FXML lateinit var customerPagination: Pagination
    @FXML lateinit var nameLabel: Label
    @FXML lateinit var sinceLabel: Label
    @FXML lateinit var noteLabel: Label
    @FXML lateinit var contactTable: TableView<Customer.Contact>
    @FXML lateinit var typeColumn: TableColumn<Customer.Contact, String>
    @FXML lateinit var contactColumn: TableColumn<Customer.Contact, String>
    @FXML lateinit var coverLabel: Label

    private lateinit var customerList: ListView<Customer>
    private val nameLabelGraphic = button(graphic = ImageView(R.png.btn_edit)) {
        size(24)
        setOnAction {
            inputDialog(customer!!.name) {
                title = getString(R.string.edit_customer)
                headerText = getString(R.string.edit_customer)
                graphic = ImageView(R.png.ic_user)
                contentText = getString(R.string.name)
            }.showAndWait().ifPresent { name ->
                transaction {
                    if (Customers.find { this.name.equal(name) }.isNotEmpty) errorAlert(getString(R.string.customer_name_taken)).showAndWait() else {
                        Customers.find { id.equal(customer!!.id) }.projection { this.name }.update(name)
                        reload(customer!!)
                        customerField.text = name
                    }
                }
            }
        }
    }
    private val noteLabelGraphic = button(graphic = ImageView(R.png.btn_edit)) {
        size(24)
        setOnAction {
            inputDialog(customer!!.note) {
                title = getString(R.string.edit_customer)
                headerText = getString(R.string.edit_customer)
                graphic = ImageView(R.png.ic_user)
                contentText = getString(R.string.note)
            }.showAndWait().ifPresent { note ->
                transaction {
                    Customers.find { id.equal(customer!!.id) }.projection { this.note }.update(note)
                    reload(customer!!)
                    customerField.text = customer!!.name
                }
            }
        }
    }

    @FXML
    fun initialize() {
        customerPagination.pageFactoryProperty() bind bindingOf(customerField.textProperty()) {
            Callback<Int, Node> { page ->
                customerList = listView {
                    runFX {
                        transaction {
                            val customers = if (customerField.text.isBlank()) Customers.find() else Customers.find { name.matches(customerField.text.toPattern()) }
                            customerPagination.pageCount = ceil(customers.count() / ITEMS_PER_PAGE.toDouble()).toInt()
                            items = customers.skip(ITEMS_PER_PAGE * page).take(ITEMS_PER_PAGE).toMutableObservableList()
                        }
                    }
                }
                nameLabel.textProperty() rebind stringBindingOf(customerList.selectionModel.selectedItemProperty()) { customer?.name ?: "" }
                sinceLabel.textProperty() rebind stringBindingOf(customerList.selectionModel.selectedItemProperty()) { customer?.since?.toString(PATTERN_DATE) ?: "" }
                noteLabel.textProperty() rebind stringBindingOf(customerList.selectionModel.selectedItemProperty()) { customer?.note ?: "" }
                contactTable.itemsProperty() rebind bindingOf(customerList.selectionModel.selectedItemProperty()) { customer?.contacts?.toObservableList() ?: mutableObservableListOf() }
                coverLabel.visibleProperty() rebind customerList.selectionModel.selectedItemProperty().isNull
                customerList
            }
        }

        nameLabel.font = loadFont(latoBold, 24.0)
        nameLabel.graphicProperty() bind bindingOf<Node>(nameLabel.hoverProperty()) { if (nameLabel.isHover && FULL_ACCESS) nameLabelGraphic else null }
        sinceLabel.font = loadFont(latoRegular, 12.0)
        noteLabel.graphicProperty() bind bindingOf<Node>(noteLabel.hoverProperty()) { if (noteLabel.isHover) noteLabelGraphic else null }
        contactTable.contextMenu = contextMenu {
            menuItem(getString(R.string.add)) {
                setOnAction {
                    dialog<Customer.Contact>(getString(R.string.add_contact), getString(R.string.add_contact), ImageView(R.png.ic_address)) {
                        lateinit var typeBox: ChoiceBox<String>
                        lateinit var contactField: TextField
                        content = gridPane {
                            gap(8)
                            label(getString(R.string.type)) col 0 row 0
                            typeBox = choiceBox(Customer.listAllTypes()) col 1 row 0
                            label(getString(R.string.contact)) col 0 row 1
                            contactField = textField { promptText = getString(R.string.contact) } col 1 row 1
                        }
                        button(CANCEL)
                        button(OK).disableProperty() bind (typeBox.selectionModel.selectedItemProperty().isNull or contactField.textProperty().isEmpty)
                        setResultConverter { if (it == OK) Customer.Contact(typeBox.value, contactField.text) else null }
                    }.showAndWait().ifPresent { contact ->
                        transaction {
                            Customers.find { id.equal(customer!!.id) }.projection { contacts }.update(customer!!.contacts + contact)
                            reload(customer!!)
                        }
                    }
                }
            }
            menuItem(getString(R.string.delete)) {
                disableProperty() bind booleanBindingOf(contactTable.selectionModel.selectedItemProperty()) { contact == null || !FULL_ACCESS }
                setOnAction {
                    confirmAlert(getString(R.string.delete_contact)).showAndWait().ifPresent {
                        transaction {
                            Customers.find { id.equal(customer!!.id) }.projection { contacts }.update(customer!!.contacts - contact!!)
                            reload(customer!!)
                        }
                    }
                }
            }
        }
        typeColumn.setCellValueFactory { it.value.type.asProperty() }
        contactColumn.setCellValueFactory { it.value.value.asProperty() }
    }

    @FXML
    fun clearOnAction() {
        customerField.text = ""
    }

    @FXML
    fun addOnAction() = inputDialog {
        title = getString(R.string.add_customer)
        headerText = getString(R.string.add_customer)
        graphic = ImageView(R.png.ic_user)
        contentText = getString(R.string.name)
    }.showAndWait().ifPresent { name ->
        transaction {
            if (Customers.find { this.name.equal(name) }.isNotEmpty) errorAlert(getString(R.string.customer_name_taken)).showAndWait() else {
                val customer = Customer(name)
                customer.id = Customers.insert(Customer(name))
                customerField.text = name
            }
        }
    }

    private val customer: Customer? get() = customerList.selectionModel.selectedItem

    private val contact: Customer.Contact? get() = contactTable.selectionModel.selectedItem

    private fun MongoDBSession.reload(customer: Customer) {
        customerList.items[customerList.items.indexOf(customer)] = Customers.find { id.equal(customer.id) }.single()
    }
}