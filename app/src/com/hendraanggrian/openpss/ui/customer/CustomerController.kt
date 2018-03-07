package com.hendraanggrian.openpss.ui.customer

import com.hendraanggrian.openpss.R
import com.hendraanggrian.openpss.collections.isNotEmpty
import com.hendraanggrian.openpss.db.schema.Customer
import com.hendraanggrian.openpss.db.schema.Customers
import com.hendraanggrian.openpss.db.transaction
import com.hendraanggrian.openpss.scene.control.CountBox
import com.hendraanggrian.openpss.time.PATTERN_DATE
import com.hendraanggrian.openpss.ui.AddUserDialog
import com.hendraanggrian.openpss.ui.Controller
import com.hendraanggrian.openpss.ui.Refreshable
import com.hendraanggrian.openpss.util.getResourceString
import com.hendraanggrian.openpss.util.tidy
import javafx.fxml.FXML
import javafx.scene.Node
import javafx.scene.control.ButtonType.OK
import javafx.scene.control.ChoiceBox
import javafx.scene.control.Label
import javafx.scene.control.ListView
import javafx.scene.control.Pagination
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView
import javafx.scene.control.TextField
import javafx.scene.image.ImageView
import javafx.scene.text.Font.loadFont
import javafx.util.Callback
import kotlinfx.application.later
import kotlinfx.beans.binding.bindingOf
import kotlinfx.beans.binding.booleanBindingOf
import kotlinfx.beans.binding.or
import kotlinfx.beans.binding.stringBindingOf
import kotlinfx.beans.property.toProperty
import kotlinfx.collections.mutableObservableListOf
import kotlinfx.collections.toMutableObservableList
import kotlinfx.collections.toObservableList
import kotlinfx.coroutines.onAction
import kotlinfx.layouts.button
import kotlinfx.layouts.choiceBox
import kotlinfx.layouts.contextMenu
import kotlinfx.layouts.gridPane
import kotlinfx.layouts.label
import kotlinfx.layouts.listView
import kotlinfx.layouts.menuItem
import kotlinfx.layouts.textField
import kotlinfx.scene.control.cancelButton
import kotlinfx.scene.control.confirmAlert
import kotlinfx.scene.control.dialog
import kotlinfx.scene.control.errorAlert
import kotlinfx.scene.control.inputDialog
import kotlinfx.scene.control.okButton
import kotlinfx.scene.layout.gaps
import kotlinfx.scene.layout.maxSize
import kotlinx.nosql.equal
import kotlinx.nosql.id
import kotlinx.nosql.mongodb.MongoDBSession
import kotlinx.nosql.update
import kotlin.math.ceil

class CustomerController : Controller(), Refreshable {

    @FXML lateinit var customerField: TextField
    @FXML lateinit var countBox: CountBox
    @FXML lateinit var customerPagination: Pagination
    @FXML lateinit var nameLabel: Label
    @FXML lateinit var sinceLabel: Label
    @FXML lateinit var noteLabel: Label
    @FXML lateinit var contactTable: TableView<Customer.Contact>
    @FXML lateinit var typeColumn: TableColumn<Customer.Contact, String>
    @FXML lateinit var contactColumn: TableColumn<Customer.Contact, String>
    @FXML lateinit var coverLabel: Label

    private lateinit var customerList: ListView<Customer>
    private val noteLabelGraphic = button(graphic = ImageView(R.image.btn_edit)) {
        maxSize = 24
        onAction {
            inputDialog(customer!!.note) {
                title = getString(R.string.edit_customer)
                headerText = getString(R.string.edit_customer)
                graphic = ImageView(R.image.ic_user)
                contentText = getString(R.string.note)
            }.showAndWait().ifPresent { note ->
                transaction {
                    Customers.find { id.equal(customer!!.id) }.projection { this.note }.update(note)
                    reloadCustomer(customer!!)
                }
            }
        }
    }

    override fun initialize() {
        refresh()

        countBox.desc = getString(R.string.items)
        nameLabel.font = loadFont(getResourceString(R.font.opensans_bold), 24.0)
        sinceLabel.font = loadFont(getResourceString(R.font.opensans_regular), 12.0)
        noteLabel.graphicProperty().bind(bindingOf<Node>(noteLabel.hoverProperty()) { if (noteLabel.isHover) noteLabelGraphic else null })
        contactTable.contextMenu {
            menuItem(getString(R.string.add)) {
                onAction {
                    dialog<Customer.Contact>(getString(R.string.add_contact), ImageView(R.image.ic_address)) {
                        lateinit var typeBox: ChoiceBox<String>
                        lateinit var contactField: TextField
                        dialogPane.content = gridPane {
                            gaps = 8
                            label(getString(R.string.type)) col 0 row 0
                            typeBox = choiceBox(Customer.listAllTypes()) col 1 row 0
                            label(getString(R.string.contact)) col 0 row 1
                            contactField = textField { promptText = getString(R.string.contact) } col 1 row 1
                        }
                        cancelButton()
                        okButton {
                            disableProperty().bind(typeBox.valueProperty().isNull or contactField.textProperty().isEmpty)
                        }
                        setResultConverter { if (it == OK) Customer.Contact(typeBox.value, contactField.text) else null }
                    }.showAndWait().ifPresent { contact ->
                        transaction {
                            Customers.find { id.equal(customer!!.id) }.projection { contacts }.update(customer!!.contacts + contact)
                            reloadCustomer(customer!!)
                        }
                    }
                }
            }
            menuItem(getString(R.string.delete)) {
                later { disableProperty().bind(booleanBindingOf(contactTable.selectionModel.selectedItemProperty()) { contact == null || !isFullAccess }) }
                onAction {
                    confirmAlert(getString(R.string.delete_contact)).showAndWait().ifPresent {
                        transaction {
                            Customers.find { id.equal(customer!!.id) }.projection { contacts }.update(customer!!.contacts - contact!!)
                            reloadCustomer(customer!!)
                        }
                    }
                }
            }
        }
        typeColumn.setCellValueFactory { it.value.type.toProperty() }
        contactColumn.setCellValueFactory { it.value.value.toProperty() }
    }

    override fun refresh() = customerPagination.pageFactoryProperty().bind(bindingOf(customerField.textProperty(), countBox.countProperty) {
        Callback<Int, Node> { page ->
            customerList = listView {
                later {
                    transaction {
                        val customers = if (customerField.text.isBlank()) Customers.find() else Customers.find { name.matches(customerField.text.toPattern()) }
                        customerPagination.pageCount = ceil(customers.count() / countBox.count.toDouble()).toInt()
                        items = customers.skip(countBox.count * page).take(countBox.count).toMutableObservableList()
                    }
                }
            }
            nameLabel.textProperty().bind(stringBindingOf(customerList.selectionModel.selectedItemProperty()) {
                customer?.name ?: ""
            })
            sinceLabel.textProperty().bind(stringBindingOf(customerList.selectionModel.selectedItemProperty()) {
                customer?.since?.toString(PATTERN_DATE) ?: ""
            })
            noteLabel.textProperty().bind(stringBindingOf(customerList.selectionModel.selectedItemProperty()) {
                customer?.note ?: ""
            })
            contactTable.itemsProperty().bind(bindingOf(customerList.selectionModel.selectedItemProperty()) {
                customer?.contacts?.toObservableList() ?: mutableObservableListOf()
            })
            coverLabel.visibleProperty().bind(customerList.selectionModel.selectedItemProperty().isNull)
            customerList
        }
    })

    @FXML
    fun add() = AddUserDialog(this, getString(R.string.add_customer)).showAndWait().ifPresent { name ->
        transaction {
            if (Customers.find { this.name.equal(name) }.isNotEmpty()) errorAlert(getString(R.string.name_taken)).showAndWait() else {
                val customer = Customer(name.tidy())
                customer.id = Customers.insert(customer)
                customerList.items.add(0, customer)
                customerList.selectionModel.select(0)
            }
        }
    }

    private val customer: Customer? get() = customerList.selectionModel.selectedItem

    private val contact: Customer.Contact? get() = contactTable.selectionModel.selectedItem

    private fun MongoDBSession.reloadCustomer(customer: Customer) = customerList.items.indexOf(customer).let { index ->
        customerList.items[customerList.items.indexOf(customer)] = Customers.find { id.equal(customer.id) }.single()
        customerList.selectionModel.select(index)
    }
}