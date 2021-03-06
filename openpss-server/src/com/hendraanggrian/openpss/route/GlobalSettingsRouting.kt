package com.hendraanggrian.openpss.route

import com.hendraanggrian.openpss.Server
import com.hendraanggrian.openpss.nosql.transaction
import com.hendraanggrian.openpss.schema.GlobalSetting
import com.hendraanggrian.openpss.schema.GlobalSettings
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Routing
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.route
import kotlinx.nosql.equal
import kotlinx.nosql.update

fun Routing.globalSetting() {
    route("${GlobalSettings.schemaName}/{key}") {
        get {
            call.respond(transaction {
                GlobalSettings { key.equal(call.getString("key")) }.single()
            })
        }
        post {
            val (key, value) = call.receive<GlobalSetting>()
            transaction {
                GlobalSettings { this.key.equal(call.getString("key")) }
                    .projection { this.value }
                    .update(value)
            }
            call.respond(HttpStatusCode.OK)
            Server.log?.info("GlobalSetting '$key' has been changed to '$value'")
        }
    }
}
