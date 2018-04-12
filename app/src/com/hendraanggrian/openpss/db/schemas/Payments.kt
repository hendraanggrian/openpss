package com.hendraanggrian.openpss.db.schemas

import com.hendraanggrian.openpss.R
import com.hendraanggrian.openpss.db.Document
import com.hendraanggrian.openpss.db.dbDateTime
import com.hendraanggrian.openpss.db.schemas.PaymentMethod.CASH
import com.hendraanggrian.openpss.db.schemas.PaymentMethod.TRANSFER
import com.hendraanggrian.openpss.db.DateTimed
import com.hendraanggrian.openpss.ui.Resourced
import kotlinx.nosql.Id
import kotlinx.nosql.dateTime
import kotlinx.nosql.double
import kotlinx.nosql.equal
import kotlinx.nosql.id
import kotlinx.nosql.mongodb.DocumentSchema
import kotlinx.nosql.mongodb.MongoDBSession
import kotlinx.nosql.nullableString
import org.joda.time.DateTime

object Payments : DocumentSchema<Payment>("payments", Payment::class) {
    val invoiceId = id("invoice_id", Invoices)
    val employeeId = id("employee_id", Employees)
    val dateTime = dateTime("date_time")
    val value = double("value")
    val transfer = nullableString("transfer")
}

data class Payment(
    var invoiceId: Id<String, Invoices>,
    var employeeId: Id<String, Employees>,
    override val dateTime: DateTime,
    var value: Double,
    val transfer: String?
) : Document<Payments>, DateTimed {

    override lateinit var id: Id<String, Payments>

    val method: PaymentMethod get() = if (transfer == null) CASH else TRANSFER

    fun getMethodDisplayText(resourced: Resourced): String = method.getDisplayText(resourced).let {
        return when (method) {
            CASH -> it
            else -> "$it - $transfer"
        }
    }

    companion object {
        fun new(
            invoiceId: Id<String, Invoices>,
            employeeId: Id<String, Employees>,
            value: Double,
            transfer: String?
        ): Payment = Payment(invoiceId, employeeId, dbDateTime, value, transfer)
    }
}

enum class PaymentMethod {
    CASH, TRANSFER;

    fun getDisplayText(resourced: Resourced): String = resourced.getString(when (this) {
        CASH -> R.string.cash
        else -> R.string.transfer
    })
}

@Suppress("NOTHING_TO_INLINE")
inline fun MongoDBSession.calculateDue(invoice: Invoice): Double =
    invoice.total - Payments.find { invoiceId.equal(invoice.id) }.sumByDouble { it.value }