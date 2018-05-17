package com.hendraanggrian.openpss.controls

import com.hendraanggrian.openpss.layouts.DateBox
import com.hendraanggrian.openpss.layouts.TimeBox
import com.hendraanggrian.openpss.layouts.dateBox
import com.hendraanggrian.openpss.layouts.timeBox
import com.hendraanggrian.openpss.localization.Resourced
import com.hendraanggrian.openpss.ui.wage.record.Record
import ktfx.coroutines.onAction
import ktfx.layouts.button
import ktfx.layouts.gridPane
import ktfx.scene.layout.gap
import org.joda.time.DateTime

class DateTimePopOver(
    resourced: Resourced,
    titleId: String,
    defaultButtonTextId: String,
    prefill: DateTime
) : DefaultPopOver<DateTime>(resourced, titleId) {

    private lateinit var dateBox: DateBox
    private lateinit var timeBox: TimeBox

    init {
        gridPane {
            gap = 8.0
            dateBox = dateBox(prefill.toLocalDate()) row 0 col 1
            button("-${Record.WORKING_HOURS}") {
                onAction { repeat(Record.WORKING_HOURS) { timeBox.previousButton.fire() } }
            } row 1 col 0
            timeBox = timeBox(prefill.toLocalTime()) {
                setOnOverlap { plus ->
                    dateBox.picker.value = when {
                        plus -> dateBox.picker.value.plusDays(1)
                        else -> dateBox.picker.value.minusDays(1)
                    }
                }
            } row 1 col 1
            button("+${Record.WORKING_HOURS}") {
                onAction { repeat(Record.WORKING_HOURS) { timeBox.nextButton.fire() } }
            } row 1 col 2
        }
        defaultButton.text = getString(defaultButtonTextId)
    }

    override fun getResult(): DateTime = dateBox.value.toDateTime(timeBox.value)
}