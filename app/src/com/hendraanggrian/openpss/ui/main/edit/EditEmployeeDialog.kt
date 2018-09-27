package com.hendraanggrian.openpss.ui.main.edit

import com.hendraanggrian.openpss.R
import com.hendraanggrian.openpss.control.dialog.TableDialog
import com.hendraanggrian.openpss.control.doneCell
import com.hendraanggrian.openpss.control.popover.InputUserPopover
import com.hendraanggrian.openpss.control.stringCell
import com.hendraanggrian.openpss.db.schemas.Employee
import com.hendraanggrian.openpss.db.schemas.Employees
import com.hendraanggrian.openpss.db.transaction
import com.hendraanggrian.openpss.i18n.Resourced
import com.hendraanggrian.openpss.util.getStyle
import javafx.scene.Node
import kotlinx.coroutines.experimental.Dispatchers
import kotlinx.coroutines.experimental.GlobalScope
import kotlinx.coroutines.experimental.delay
import kotlinx.coroutines.experimental.javafx.JavaFx
import kotlinx.coroutines.experimental.launch
import kotlinx.nosql.notEqual
import kotlinx.nosql.update
import ktfx.beans.property.toProperty
import ktfx.beans.value.or
import ktfx.collections.toMutableObservableList
import ktfx.coroutines.onAction
import ktfx.layouts.LayoutManager
import ktfx.layouts.button
import ktfx.scene.control.styledInfoAlert

class EditEmployeeDialog(
    resourced: Resourced,
    private val employee: Employee
) : TableDialog<Employee, Employees>(Employees, resourced, employee, R.string.employee, R.image.header_employee) {

    private companion object {
        // temporary fix
        const val DELAY = 200L
    }

    override fun LayoutManager<Node>.onCreateActions() {
        button(getString(R.string.toggle_admin)) {
            bindDisable()
            onAction {
                transaction { Employees[selected!!].projection { admin }.update(!selected!!.admin) }
                refresh()
            }
        }
        button(getString(R.string.reset_password)) {
            bindDisable()
            onAction {
                transaction { Employees[selected!!].projection { password }.update(Employee.DEFAULT_PASSWORD) }
                styledInfoAlert(
                    getStyle(R.style.openpss),
                    getString(R.string.change_password_popup_will_appear_when_is_logged_back_in, employee.name)
                ).show()
            }
        }
    }

    init {
        getString(R.string.name)<String> {
            stringCell { name }
        }
        getString(R.string.admin)<Boolean> {
            doneCell { admin }
        }
        GlobalScope.launch(Dispatchers.JavaFx) {
            delay(DELAY)
            transaction { employee.isAdmin() }.toProperty().let {
                addButton.disableProperty().bind(!it)
                deleteButton.disableProperty().bind(!selectedBinding or !it)
            }
        }
    }

    override fun refresh() {
        table.items = transaction { Employees { it.name.notEqual(Employee.BACKDOOR.name) }.toMutableObservableList() }
    }

    override fun add() = InputUserPopover(this, R.string.add_employee, false).showAt(addButton) {
        val employee = Employee.new(it)
        employee.id = transaction { Employees.insert(employee) }
        table.items.add(employee)
        select(employee)
    }

    private fun Node.bindDisable() {
        GlobalScope.launch(Dispatchers.JavaFx) {
            delay(DELAY)
            disableProperty().bind(!selectedBinding or !transaction { employee.isAdmin() }.toProperty())
        }
    }
}