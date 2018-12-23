package com.hendraanggrian.openpss.ui.price

import com.hendraanggrian.openpss.R
import com.hendraanggrian.openpss.content.FxComponent
import com.hendraanggrian.openpss.data.OffsetPrice
import javafx.beans.value.ObservableValue
import kotlinx.coroutines.CoroutineScope
import ktfx.coroutines.onEditCommit
import ktfx.listeners.textFieldCellFactory
import ktfx.readOnlyDoublePropertyOf
import ktfx.readOnlyIntPropertyOf

@Suppress("UNCHECKED_CAST")
class EditOffsetPriceDialog(
    component: FxComponent
) : EditPriceDialog<OffsetPrice>(component, R.string.offset_print_price) {

    init {
        getString(R.string.min_qty)<Int> {
            minWidth = 128.0
            style = "-fx-alignment: center-right;"
            setCellValueFactory { readOnlyIntPropertyOf(it.value.minQty) as ObservableValue<Int> }
            textFieldCellFactory {
                fromString { it.toIntOrNull() ?: 0 }
            }
            onEditCommit { cell ->
                val offset = cell.rowValue
                if (api.editOffsetPrice(offset.id, cell.newValue, offset.minPrice, offset.excessPrice)) {
                    cell.rowValue.minQty = cell.newValue
                }
            }
        }

        getString(R.string.min_price)<Double> {
            minWidth = 128.0
            style = "-fx-alignment: center-right;"
            setCellValueFactory { readOnlyDoublePropertyOf(it.value.minPrice) as ObservableValue<Double> }
            textFieldCellFactory {
                fromString { it.toDoubleOrNull() ?: 0.0 }
            }
            onEditCommit { cell ->
                val offset = cell.rowValue
                if (api.editOffsetPrice(offset.id, offset.minQty, cell.newValue, offset.excessPrice)) {
                    cell.rowValue.minPrice = cell.newValue
                }
            }
        }

        getString(R.string.excess_price)<Double> {
            minWidth = 128.0
            style = "-fx-alignment: center-right;"
            setCellValueFactory { readOnlyDoublePropertyOf(it.value.excessPrice) as ObservableValue<Double> }
            textFieldCellFactory {
                fromString { it.toDoubleOrNull() ?: 0.0 }
            }
            onEditCommit { cell ->
                val offset = cell.rowValue
                if (api.editOffsetPrice(offset.id, offset.minQty, offset.minPrice, cell.newValue)) {
                    cell.rowValue.excessPrice = cell.newValue
                }
            }
        }
    }

    override suspend fun CoroutineScope.refresh(): List<OffsetPrice> = api.getOffsetPrices()

    override suspend fun CoroutineScope.add(name: String): OffsetPrice? = api.addOffsetPrice(name)

    override suspend fun CoroutineScope.delete(selected: OffsetPrice): Boolean = api.deleteOffsetPrice(selected.id)
}