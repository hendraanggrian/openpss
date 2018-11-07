package com.hendraanggrian.openpss.ui.customer

import com.hendraanggrian.openpss.R
import com.hendraanggrian.openpss.content.Context
import com.hendraanggrian.openpss.control.dialog.ResultableDialog
import com.hendraanggrian.openpss.db.schemas.Customer
import com.hendraanggrian.openpss.util.clean
import com.hendraanggrian.openpss.util.isPersonName
import javafx.scene.control.Label
import javafx.scene.control.Tab
import javafx.scene.control.TabPane
import javafx.scene.control.TextField
import ktfx.beans.binding.`when`
import ktfx.beans.binding.otherwise
import ktfx.beans.binding.then
import ktfx.beans.value.eq
import ktfx.beans.value.isBlank
import ktfx.beans.value.or
import ktfx.jfoenix.jfxTabPane
import ktfx.jfoenix.jfxTextField
import ktfx.layouts.label
import ktfx.layouts.tab

class AddCustomerDialog(context: Context) : ResultableDialog<Customer>(context, R.string.add_customer) {

    private companion object {
        const val WIDTH = 300.0
    }

    private val tabPane: TabPane
    private val editor: TextField

    init {
        contentPane.run {
            minWidth = WIDTH
            maxWidth = WIDTH
        }
        tabPane = jfxTabPane {
            tab {
                bindGraphic(R.image.display_person_selected, R.image.display_person)
            }
            tab {
                bindGraphic(R.image.display_company_selected, R.image.display_company)
            }
        }
        label {
            styleClass += "bold"
            bindText(R.string.person, R.string.company)
        }
        label {
            bindText(R.string._person_requirement, R.string._company_requirement)
            isWrapText = true
        }
        editor = jfxTextField {
            promptText = getString(R.string.name)
        }
        defaultButton.disableProperty().bind(
            `when`(tabPane.selectionModel.selectedIndexProperty() eq 0)
                then (editor.textProperty().isBlank() or !editor.textProperty().isPersonName())
                otherwise editor.textProperty().isBlank()
        )
    }

    private fun Tab.bindGraphic(selectedImageId: String, unselectedImageId: String) {
        graphicProperty().bind(
            ktfx.beans.binding.`when`(selectedProperty())
                then ktfx.layouts.imageView(selectedImageId)
                otherwise ktfx.layouts.imageView(unselectedImageId)
        )
    }

    private fun Label.bindText(personTextId: String, companyTextId: String) = textProperty().bind(
        `when`(tabPane.selectionModel.selectedIndexProperty() eq 0)
            then getString(personTextId)
            otherwise getString(companyTextId)
    )

    override val nullableResult: Customer?
        get() = Customer.new(
            editor.text.clean(),
            tabPane.selectionModel.selectedIndex == 0
        )
}