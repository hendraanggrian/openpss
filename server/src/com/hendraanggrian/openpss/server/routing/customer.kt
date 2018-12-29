package com.hendraanggrian.openpss.server.routing

import com.hendraanggrian.openpss.data.Customer
import com.hendraanggrian.openpss.data.Log
import com.hendraanggrian.openpss.data.Page
import com.hendraanggrian.openpss.schema.Customers
import com.hendraanggrian.openpss.schema.Logs
import com.hendraanggrian.openpss.server.R
import com.hendraanggrian.openpss.server.getInt
import com.hendraanggrian.openpss.server.getString
import com.hendraanggrian.openpss.server.isNotEmpty
import com.hendraanggrian.openpss.server.resources
import com.hendraanggrian.openpss.server.transaction
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.delete
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.put
import io.ktor.routing.route
import kotlinx.nosql.update
import java.util.regex.Pattern
import kotlin.math.ceil

fun Routing.customerRouting() {
    route("$Customers") {
        get {
            val search = call.getString("search")
            val page = call.getInt("page")
            val count = call.getInt("count")
            call.respond(
                transaction {
                    val customers = Customers.buildQuery { _, or ->
                        if (search.isNotBlank()) {
                            or(name.matches(search, Pattern.CASE_INSENSITIVE))
                            or(address.matches(search, Pattern.CASE_INSENSITIVE))
                            or(note.matches(search, Pattern.CASE_INSENSITIVE))
                        }
                    }
                    Page(
                        ceil(customers.count() / count.toDouble()).toInt(),
                        customers.skip(count * page).take(count).toList()
                    )
                }
            )
        }
        post {
            val customer = call.receive<Customer>()
            when {
                transaction {
                    Customers { name.matches("^$customer$", Pattern.CASE_INSENSITIVE) }.isNotEmpty()
                } -> call.respond(HttpStatusCode.NotAcceptable, "Name taken")
                else -> {
                    customer.id = transaction { Customers.insert(customer) }
                    call.respond(customer)
                }
            }
        }
        route("{id}") {
            get {
                call.respond(transaction { Customers[call.getString("id")].single() })
            }
            put {
                val customer = call.receive<Customer>()
                transaction {
                    val query = Customers[call.getString("id")]
                    val customerName = query.single().name
                    query.projection { name + address + note }
                        .update(customer.name, customer.address, customer.note)
                    Logs += Log.new(
                        resources.getString(R.string.customer_edit).format(customerName),
                        call.getString("login")
                    )
                }
                call.respond(HttpStatusCode.OK)
            }
            route("${Customers.Contacts}") {
                post {
                    val contact = call.receive<Customer.Contact>()
                    transaction {
                        val query = Customers[call.getString("id")]
                        query.projection { contacts }
                            .update(query.single().contacts + contact)
                    }
                    call.respond(contact)
                }
                delete {
                    val contact = call.receive<Customer.Contact>()
                    transaction {
                        val query = Customers[call.getString("id")]
                        val customer = query.single()
                        query.projection { contacts }
                            .update(customer.contacts - contact)
                        Logs += Log.new(
                            resources.getString(R.string.contact_deleted).format(contact.value, customer.name),
                            call.getString("login")
                        )
                    }
                    call.respond(HttpStatusCode.OK)
                }
            }
        }
    }
}