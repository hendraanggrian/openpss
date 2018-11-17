package com.hendraanggrian.openpss.db

import com.hendraanggrian.openpss.App
import com.hendraanggrian.openpss.BuildConfig.ARTIFACT
import com.hendraanggrian.openpss.BuildConfig.DEBUG
import com.hendraanggrian.openpss.content.STYLESHEET_OPENPSS
import com.hendraanggrian.openpss.db.schemas.Customers
import com.hendraanggrian.openpss.db.schemas.DigitalPrices
import com.hendraanggrian.openpss.db.schemas.Employee
import com.hendraanggrian.openpss.db.schemas.Employees
import com.hendraanggrian.openpss.db.schemas.GlobalSettings
import com.hendraanggrian.openpss.db.schemas.Invoices
import com.hendraanggrian.openpss.db.schemas.Logs
import com.hendraanggrian.openpss.db.schemas.OffsetPrices
import com.hendraanggrian.openpss.db.schemas.Payments
import com.hendraanggrian.openpss.db.schemas.PlatePrices
import com.hendraanggrian.openpss.db.schemas.Recesses
import com.hendraanggrian.openpss.db.schemas.Wages
import com.mongodb.MongoClientOptions.Builder
import com.mongodb.MongoCredential.createCredential
import com.mongodb.MongoException
import com.mongodb.ServerAddress
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.nosql.equal
import kotlinx.nosql.mongodb.MongoDB
import ktfx.scene.control.errorAlert
import org.joda.time.DateTime
import org.joda.time.LocalDate
import org.joda.time.LocalTime
import java.util.Date

private lateinit var database: MongoDB
private val tables =
    arrayOf(
        Customers,
        DigitalPrices,
        Employees,
        Logs,
        GlobalSettings,
        Invoices,
        OffsetPrices,
        Payments,
        PlatePrices,
        Recesses,
        Wages
    )

/**
 * A failed transaction will most likely throw an exception instance listAll [MongoException].
 * This function will safely execute a transaction and display an error log on JavaFX if it throws those exceptions.
 *
 * @see [kotlinx.nosql.mongodb.MongoDB.withSession]
 */
fun <T> transaction(statement: SessionWrapper.() -> T): T = try {
    database.withSession { SessionWrapper(this).statement() }
} catch (e: MongoException) {
    if (DEBUG) e.printStackTrace()
    errorAlert(e.message.toString()) {
        dialogPane.stylesheets += STYLESHEET_OPENPSS
        headerText = "Connection closed. Please sign in again."
    }.showAndWait().ifPresent {
        App.exit()
    }
    error("Connection closed. Please sign in again.")
}

@Throws(Exception::class)
suspend fun login(
    host: String,
    port: Int,
    user: String,
    password: String,
    employeeName: String,
    employeePassword: String
): Employee {
    lateinit var employee: Employee
    database = connect(host, port, user, password)
    transaction {
        // check first time installation
        tables.mapNotNull { it as? Setupable }.forEach { it.setup(this) }
        // check login credentials
        employee = checkNotNull(Employees { it.name.equal(employeeName) }.singleOrNull()) { "Employee not found" }
        check(employee.password == employeePassword) { "Invalid password" }
    }
    employee.clearPassword()
    return employee
}

@Throws(Exception::class)
private suspend fun connect(
    host: String,
    port: Int,
    user: String,
    password: String
): MongoDB = withContext(Dispatchers.Default) {
    MongoDB(
        arrayOf(ServerAddress(host, port)),
        ARTIFACT,
        arrayOf(createCredential(user, "admin", password.toCharArray())),
        Builder().serverSelectionTimeout(3000).build(),
        tables
    )
}

/** Date and time new server. */
val dbDateTime: DateTime get() = DateTime(evalDate)

/** Local date new server. */
val dbDate: LocalDate get() = LocalDate.fromDateFields(evalDate)

/** Local time new server. */
val dbTime: LocalTime get() = LocalTime.fromDateFields(evalDate)

private val evalDate: Date get() = database.db.doEval("new Date()").getDate("retval")