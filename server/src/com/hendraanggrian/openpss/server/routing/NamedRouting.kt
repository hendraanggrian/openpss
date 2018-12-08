package com.hendraanggrian.openpss.server.routing

import com.hendraanggrian.openpss.db.Document
import com.hendraanggrian.openpss.db.Named
import com.hendraanggrian.openpss.db.NamedSchema
import com.hendraanggrian.openpss.db.SessionWrapper
import com.hendraanggrian.openpss.db.schemas.DigitalPrice
import com.hendraanggrian.openpss.db.schemas.DigitalPrices
import com.hendraanggrian.openpss.db.schemas.Employee
import com.hendraanggrian.openpss.db.schemas.Employees
import com.hendraanggrian.openpss.db.schemas.OffsetPrice
import com.hendraanggrian.openpss.db.schemas.OffsetPrices
import com.hendraanggrian.openpss.db.schemas.PlatePrice
import com.hendraanggrian.openpss.db.schemas.PlatePrices
import com.hendraanggrian.openpss.server.db.transaction
import com.hendraanggrian.openpss.server.util.getBoolean
import com.hendraanggrian.openpss.server.util.getDouble
import com.hendraanggrian.openpss.server.util.getInt
import com.hendraanggrian.openpss.server.util.getString
import com.hendraanggrian.openpss.util.isNotEmpty
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
import kotlinx.nosql.DocumentSchemaQueryWrapper
import kotlinx.nosql.equal
import kotlinx.nosql.mongodb.DocumentSchema
import kotlinx.nosql.update

fun Routing.routePlatePrice() = routeNamed(
    "plate-prices",
    PlatePrices,
    { call -> PlatePrice.new(call.getString("name")) },
    { call, wrapper ->
        wrapper.projection { price }
            .update(call.getDouble("price"))
    })

fun Routing.routeOffsetPrice() = routeNamed(
    "offset-prices",
    OffsetPrices,
    { call -> OffsetPrice.new(call.getString("name")) },
    { call, wrapper ->
        wrapper.projection { minQty + minPrice + excessPrice }
            .update(call.getInt("minQty"), call.getDouble("minPrice"), call.getDouble("excessPrice"))
    })

fun Routing.routeDigitalPrice() = routeNamed(
    "digital-prices",
    DigitalPrices,
    { call -> DigitalPrice.new(call.getString("name")) },
    { call, wrapper ->
        wrapper.projection { oneSidePrice + twoSidePrice }
            .update(call.getDouble("oneSidePrice"), call.getDouble("twoSidePrice"))
    })

fun Routing.routeEmployee() = routeNamed(
    "employees",
    Employees,
    { call -> Employee.new(call.getString("name")) },
    { call, wrapper ->
        wrapper.projection { password + isAdmin }
            .update(call.getString("password"), call.getBoolean("isAdmin"))
    })

private fun <S, D> Routing.routeNamed(
    route: String,
    schema: S,
    create: (ApplicationCall) -> D,
    edit: SessionWrapper.(ApplicationCall, DocumentSchemaQueryWrapper<S, String, D>) -> Unit
) where S : DocumentSchema<D>, S : NamedSchema, D : Document<S>, D : Named {
    route(route) {
        get {
            call.respond(transaction { schema() })
        }
        post {
            val doc = create(call)
            when {
                transaction { schema { it.name.equal(doc.name) }.isNotEmpty() } ->
                    call.respond(HttpStatusCode.NotAcceptable, "Name taken")
                else -> {
                    doc.id = transaction { schema.insert(doc) }
                    call.respond(doc)
                }
            }
        }
        route("{name}") {
            get {
                call.respond(transaction { schema { it.name.equal(call.getString("name")) }.single() })
            }
            put {
                transaction {
                    edit(call, schema { it.name.equal(call.getString("name")) })
                }
                call.respond(HttpStatusCode.OK)
            }
            delete {
                transaction {
                    schema -= schema { it.name.equal(call.getString("name")) }.single()
                }
                call.respond(HttpStatusCode.OK)
            }
        }
    }
}