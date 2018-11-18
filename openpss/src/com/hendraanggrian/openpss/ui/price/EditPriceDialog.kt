package com.hendraanggrian.openpss.ui.price

import com.hendraanggrian.openpss.App
import com.hendraanggrian.openpss.R
import com.hendraanggrian.openpss.content.Context
import com.hendraanggrian.openpss.util.stringCell
import com.hendraanggrian.openpss.db.Document
import com.hendraanggrian.openpss.db.Named
import com.hendraanggrian.openpss.db.NamedSchema
import com.hendraanggrian.openpss.db.transaction
import com.hendraanggrian.openpss.control.dialog.TableDialog
import com.hendraanggrian.openpss.control.popover.InputPopover
import com.hendraanggrian.openpss.util.isNotEmpty
import kotlinx.nosql.equal
import kotlinx.nosql.mongodb.DocumentSchema
import ktfx.jfoenix.jfxSnackbar

abstract class EditPriceDialog<D, S>(
    context: Context,
    headerId: String,
    schema: S
) : TableDialog<D, S>(context, headerId, schema)
    where D : Document<S>, D : Named, S : DocumentSchema<D>, S : NamedSchema {

    abstract fun newPrice(name: String): D

    init {
        @Suppress("LeakingThis")
        getString(R.string.name)<String> {
            minWidth = 96.0
            stringCell { name }
        }
    }

    override fun add() = InputPopover(
        this, when (this) {
            is EditPlatePriceDialog -> R.string.add_plate_price
            is EditOffsetPrintPriceDialog -> R.string.add_offset_price
            else -> R.string.add_digital_price
        }
    ).show(addButton) { name ->
        transaction @Suppress("IMPLICIT_CAST_TO_ANY") {
            when {
                schema { it.name.equal(name) }.isNotEmpty() ->
                    stack.jfxSnackbar(getString(R.string.name_taken), App.DURATION_SHORT)
                else -> {
                    val price = newPrice(name!!)
                    price.id = schema.insert(price)
                    table.items.add(price)
                }
            }
        }
    }
}