package com.hendraanggrian.openpss

import com.google.gson.GsonBuilder
import com.hendraanggrian.openpss.data.GlobalSetting
import com.hendraanggrian.openpss.nosql.Database
import com.hendraanggrian.openpss.nosql.transaction
import com.hendraanggrian.openpss.routing.AuthRouting
import com.hendraanggrian.openpss.routing.CustomersRouting
import com.hendraanggrian.openpss.routing.DateTimeRouting
import com.hendraanggrian.openpss.routing.DigitalPriceRouting
import com.hendraanggrian.openpss.routing.EmployeeRouting
import com.hendraanggrian.openpss.routing.GlobalSettingsRouting
import com.hendraanggrian.openpss.routing.InvoicesRouting
import com.hendraanggrian.openpss.routing.LogsRouting
import com.hendraanggrian.openpss.routing.OffsetPriceRouting
import com.hendraanggrian.openpss.routing.PaymentsRouting
import com.hendraanggrian.openpss.routing.PlatePriceRouting
import com.hendraanggrian.openpss.routing.RecessesRouting
import com.hendraanggrian.openpss.routing.WagesRouting
import com.hendraanggrian.openpss.routing.route
import com.hendraanggrian.openpss.ui.TextDialog
import com.hendraanggrian.openpss.ui.menuItem
import com.hendraanggrian.openpss.ui.popupMenu
import com.hendraanggrian.openpss.ui.systemTray
import com.hendraanggrian.openpss.ui.trayIcon
import com.hendraanggrian.openpss.util.registerJodaTimeSerializers
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.AutoHeadResponse
import io.ktor.features.CallLogging
import io.ktor.features.Compression
import io.ktor.features.ConditionalHeaders
import io.ktor.features.ContentNegotiation
import io.ktor.features.PartialContent
import io.ktor.features.StatusPages
import io.ktor.features.XForwardedHeaderSupport
import io.ktor.gson.GsonConverter
import io.ktor.gson.gson
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.routing
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.util.error
import io.ktor.websocket.WebSockets
import org.slf4j.Logger
import java.awt.Desktop
import java.awt.Frame
import java.awt.SystemTray
import java.net.URI
import java.util.ResourceBundle

object OpenPssServer : StringResources {

    private val frame = Frame()

    private lateinit var log: Logger

    val logger: Logger? get() = log.takeIf { BuildConfig.DEBUG }

    override val resourceBundle: ResourceBundle
        get() = Language.ofFullCode(transaction {
            findGlobalSetting(GlobalSetting.KEY_LANGUAGE).value
        }).toResourcesBundle()

    @JvmStatic
    fun main(args: Array<String>) {
        Database.start()
        when {
            !SystemTray.isSupported() ->
                TextDialog(this, frame, R.string.system_tray_is_unsupported).show2()
            else -> systemTray {
                trayIcon("/icon.png") {
                    toolTip = buildString {
                        append("${BuildConfig.NAME} ${BuildConfig.VERSION}")
                        if (BuildConfig.DEBUG) {
                            append(" - DEBUG")
                        }
                    }
                    isImageAutoSize = true
                    popupMenu {
                        if (BuildConfig.DEBUG) {
                            menuItem(
                                getString(R.string.active_on)
                                    .format("localhost", BuildConfig.SERVER_PORT)
                            )
                        }
                        menuItem(
                            getString(R.string.active_on)
                                .format(BuildConfig.SERVER_HOST, BuildConfig.SERVER_PORT)
                        )
                        menuItem("-")
                        menuItem(getString(R.string.about).format(toolTip)) {
                            addActionListener {
                                when {
                                    !Desktop.isDesktopSupported() -> TextDialog(
                                        this@OpenPssServer,
                                        frame,
                                        R.string.desktop_is_unsupported
                                    ).show2()
                                    else -> Desktop.getDesktop().browse(URI(BuildConfig.WEBSITE))
                                }
                            }
                        }
                        menuItem(getString(R.string.quit), 81) {
                            addActionListener { System.exit(0) }
                        }
                    }
                }
            }
        }
        log = embeddedServer(Netty, applicationEngineEnvironment {
            if (BuildConfig.DEBUG) {
                connector {
                    host = "localhost"
                    port = BuildConfig.SERVER_PORT
                }
            }
            connector {
                host = BuildConfig.SERVER_HOST
                port = BuildConfig.SERVER_PORT
            }
            module {
                if (BuildConfig.DEBUG) {
                    install(CallLogging)
                }
                install(ConditionalHeaders)
                install(Compression)
                install(PartialContent)
                install(AutoHeadResponse)
                install(WebSockets)
                install(XForwardedHeaderSupport)
                install(StatusPages) {
                    exception<ServiceUnavailable> {
                        call.respond(HttpStatusCode.ServiceUnavailable)
                    }
                    exception<BadRequest> {
                        call.respond(HttpStatusCode.BadRequest)
                    }
                    exception<Unauthorized> {
                        call.respond(HttpStatusCode.Unauthorized)
                    }
                    exception<NotFound> {
                        call.respond(HttpStatusCode.NotFound)
                    }
                    exception<SecretInvalidError> {
                        call.respond(HttpStatusCode.Forbidden)
                    }
                    exception<Throwable> {
                        environment.log.error(it)
                        call.respond(HttpStatusCode.InternalServerError)
                    }
                }
                install(ContentNegotiation) {
                    gson {
                        register(
                            ContentType.Application.Json,
                            GsonConverter(GsonBuilder().registerJodaTimeSerializers().create())
                        )
                        if (BuildConfig.DEBUG) {
                            setPrettyPrinting()
                        }
                    }
                }
                routing {
                    route(AuthRouting)
                    route(CustomersRouting)
                    route(DateTimeRouting)
                    route(GlobalSettingsRouting)
                    route(InvoicesRouting)
                    route(LogsRouting)
                    route(PlatePriceRouting)
                    route(OffsetPriceRouting)
                    route(DigitalPriceRouting)
                    route(EmployeeRouting)
                    route(PaymentsRouting)
                    route(RecessesRouting)
                    route(WagesRouting)
                }
            }
        }).start(wait = true).environment.log
        log.info("Welcome to ${BuildConfig.NAME} ${BuildConfig.VERSION}")
        log.info("For more information, visit ${BuildConfig.WEBSITE}")
        logger?.info("Debug mode is activated, server activities will be logged here.")
    }
}