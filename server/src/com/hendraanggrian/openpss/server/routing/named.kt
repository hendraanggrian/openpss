package com.hendraanggrian.openpss.server.routing

import com.hendraanggrian.openpss.data.DigitalPrice
import com.hendraanggrian.openpss.data.Employee
import com.hendraanggrian.openpss.data.Log
import com.hendraanggrian.openpss.data.OffsetPrice
import com.hendraanggrian.openpss.data.PlatePrice
import com.hendraanggrian.openpss.nosql.DocumentQuery
import com.hendraanggrian.openpss.nosql.NamedDocument
import com.hendraanggrian.openpss.nosql.NamedSchema
import com.hendraanggrian.openpss.nosql.SessionWrapper
import com.hendraanggrian.openpss.schema.DigitalPrices
import com.hendraanggrian.openpss.schema.Employees
import com.hendraanggrian.openpss.schema.Logs
import com.hendraanggrian.openpss.schema.OffsetPrices
import com.hendraanggrian.openpss.schema.PlatePrices
import com.hendraanggrian.openpss.server.R
import com.hendraanggrian.openpss.server.getBoolean
import com.hendraanggrian.openpss.server.getDouble
import com.hendraanggrian.openpss.server.getInt
import com.hendraanggrian.openpss.server.getString
import com.hendraanggrian.openpss.server.isNotEmpty
import com.hendraanggrian.openpss.server.resources
import com.hendraanggrian.openpss.server.transaction
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.delete
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.put
import io.ktor.routing.route
import kotlinx.nosql.equal
import kotlinx.nosql.update

fun Routing.platePriceRouting() = namedRouting("$PlatePrices", PlatePrices,
    onCreate = { call -> PlatePrice.new(call.getString("name")) },
    onEdit = { call, query ->
        query.projection { price }
            .update(call.getDouble("price"))
    })

fun Routing.offsetPriceRouting() = namedRouting("$OffsetPrices", OffsetPrices,
    onCreate = { call -> OffsetPrice.new(call.getString("name")) },
    onEdit = { call, query ->
        query.projection { minQty + minPrice + excessPrice }
            .update(call.getInt("minQty"), call.getDouble("minPrice"), call.getDouble("excessPrice"))
    })

fun Routing.digitalPriceRouting() = namedRouting("$DigitalPrices", DigitalPrices,
    onCreate = { call -> DigitalPrice.new(call.getString("name")) },
    onEdit = { call, query ->
        query.projection { oneSidePrice + twoSidePrice }
            .update(call.getDouble("oneSidePrice"), call.getDouble("twoSidePrice"))
    })

fun Routing.employeeRouting() = namedRouting("$Employees", Employees,
    onGet = {
        val employees = Employees()
        employees.forEach { it.clearPassword() }
        employees.toList()
    },
    onCreate = { call -> Employee.new(call.getString("name")) },
    onEdit = { call, query ->
        query.projection { password + isAdmin }
            .update(call.getString("password"), call.getBoolean("isAdmin"))
        Logs += Log.new(
            resources.getString(R.string.employee_edit).format(query.single().name),
            call.getString("login")
        )
    },
    onDeleted = { call, query ->
        Logs += Log.new(
            resources.getString(R.string.employee_delete).format(query.single().name),
            call.getString("login")
        )
    })

private fun <S : NamedSchema<D>, D : NamedDocument<S>> Routing.namedRouting(
    path: String,
    schema: S,
    onGet: SessionWrapper.(call: ApplicationCall) -> List<D> = { schema().toList() },
    onCreate: (call: ApplicationCall) -> D,
    onEdit: SessionWrapper.(call: ApplicationCall, query: DocumentQuery<S, String, D>) -> Unit,
    onDeleted: SessionWrapper.(call: ApplicationCall, query: DocumentQuery<S, String, D>) -> Unit = { _, _ -> }
) {
    route(path) {
        get {
            call.respond(transaction { onGet(call) })
        }
        post {
            val doc = onCreate(call)
            when {
                transaction { schema { name.equal(doc.name) }.isNotEmpty() } ->
                    call.respond(HttpStatusCode.NotAcceptable, "Name taken")
                else -> {
                    doc.id = transaction { schema.insert(doc) }
                    call.respond(doc)
                }
            }
        }
        route("{id}") {
            get {
                call.respond(transaction { schema[call.getString("id")].single() })
            }
            put {
                transaction {
                    onEdit(call, schema[call.getString("id")])
                }
                call.respond(HttpStatusCode.OK)
            }
            delete {
                transaction {
                    val query = schema[call.getString("id")]
                    schema -= query.single()
                    onDeleted(call, query)
                }
                call.respond(HttpStatusCode.OK)
            }
        }
    }
}