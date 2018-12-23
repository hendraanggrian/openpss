package com.hendraanggrian.openpss.popup.popover

import com.hendraanggrian.openpss.R
import com.hendraanggrian.openpss.content.FxComponent
import com.hendraanggrian.openpss.control.DateBox
import com.hendraanggrian.openpss.control.TimeBox
import com.hendraanggrian.openpss.ui.wage.record.Record
import ktfx.controls.gap
import ktfx.coroutines.onAction
import ktfx.jfoenix.jfxButton
import ktfx.layouts.gridPane
import org.joda.time.DateTime

class DateTimePopover(
    component: FxComponent,
    titleId: String,
    defaultButtonTextId: String,
    prefill: DateTime
) : ResultablePopover<DateTime>(component, titleId) {

    private lateinit var dateBox: DateBox
    private lateinit var timeBox: TimeBox

    init {
        gridPane {
            gap = getDouble(R.dimen.padding_medium)
            dateBox = DateBox(prefill.toLocalDate())() row 0 col 1
            jfxButton("-${Record.WORKING_HOURS}") {
                onAction {
                    repeat(Record.WORKING_HOURS) {
                        timeBox.previousButton.fire()
                    }
                }
            } row 1 col 0
            timeBox = TimeBox(prefill.toLocalTime()).apply {
                onOverlap = { plus ->
                    dateBox.picker.value = when {
                        plus -> dateBox.picker.value.plusDays(1)
                        else -> dateBox.picker.value.minusDays(1)
                    }
                }
            }() row 1 col 1
            jfxButton("+${Record.WORKING_HOURS}") {
                onAction {
                    repeat(Record.WORKING_HOURS) {
                        timeBox.nextButton.fire()
                    }
                }
            } row 1 col 2
        }
        defaultButton.text = getString(defaultButtonTextId)
    }

    override val nullableResult: DateTime? get() = dateBox.value!!.toDateTime(timeBox.value)
}