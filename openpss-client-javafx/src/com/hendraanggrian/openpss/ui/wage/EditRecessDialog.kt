package com.hendraanggrian.openpss.ui.wage

import com.hendraanggrian.openpss.FxComponent
import com.hendraanggrian.openpss.PATTERN_TIME
import com.hendraanggrian.openpss.R2
import com.hendraanggrian.openpss.api.OpenPSSApi
import com.hendraanggrian.openpss.schema.Recess
import com.hendraanggrian.openpss.ui.TableDialog
import com.hendraanggrian.openpss.util.stringCell
import kotlinx.coroutines.CoroutineScope
import ktfx.collections.toMutableObservableList

class EditRecessDialog(component: FxComponent) : TableDialog<Recess>(component, R2.string.recess) {

    init {
        getString(R2.string.start)<String> {
            stringCell { start.toString(PATTERN_TIME) }
        }
        getString(R2.string.end)<String> {
            stringCell { end.toString(PATTERN_TIME) }
        }
    }

    override suspend fun CoroutineScope.refresh(): List<Recess> = OpenPSSApi.getRecesses().toMutableObservableList()

    override fun add() = AddRecessPopOver(this).show(addButton) { pair ->
        table.items.add(OpenPSSApi.addRecess(pair!!.first, pair.second))
    }

    override suspend fun CoroutineScope.delete(selected: Recess): Boolean = OpenPSSApi.deleteRecess(selected.id)
}
