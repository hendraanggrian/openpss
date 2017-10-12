package com.wijayaprinting.javafx

import com.wijayaprinting.javafx.io.MySQLFile
import com.wijayaprinting.javafx.io.PreferencesFile
import com.wijayaprinting.javafx.scene.control.IPField
import com.wijayaprinting.javafx.scene.control.IntField
import com.wijayaprinting.javafx.scene.utils.gaps
import com.wijayaprinting.javafx.utils.icon
import com.wijayaprinting.javafx.utils.setIconOnOSX
import com.wijayaprinting.mysql.MySQL
import io.reactivex.Completable
import io.reactivex.rxjavafx.schedulers.JavaFxScheduler
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import javafx.application.Application
import javafx.event.ActionEvent
import javafx.fxml.FXMLLoader
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.GridPane
import javafx.scene.layout.VBox
import javafx.stage.Stage
import kotfx.bindings.not
import kotfx.bindings.or
import kotfx.dialogs.errorAlert
import kotfx.dialogs.infoAlert
import kotfx.exitFX
import kotfx.runLater
import java.awt.Toolkit
import java.net.InetAddress

/**
 * @author Hendra Anggrian (hendraanggrian@gmail.com)
 */
class App : Application() {

    companion object {
        private const val IP_LOOKUP_TIMEOUT = 3000

        @JvmStatic
        fun main(vararg args: String) = launch(App::class.java, *args)
    }

    override fun init() {
        setResources(Language.parse(PreferencesFile()[PreferencesFile.LANGUAGE].value).getResources("string"))
    }

    override fun start(stage: Stage) {
        stage.icon = Image(R.png.logo_launcher)
        setIconOnOSX(Toolkit.getDefaultToolkit().getImage(App::class.java.getResource(R.png.logo_launcher)))

        LoginDialog()
                .showAndWait()
                .filter { it is String }
                .ifPresent {
                    val minSize = Pair(720.0, 640.0)
                    stage.apply {
                        scene = Scene(FXMLLoader.load(App::class.java.getResource(R.fxml.layout_main), resources), minSize.first, minSize.second)
                        icons.add(Image(R.png.ic_launcher))
                        title = "${getString(R.string.app_name)} ${BuildConfig.VERSION}"
                        minWidth = minSize.first
                        minHeight = minSize.second
                    }.show()
                }
    }

    inner class LoginDialog : Dialog<Any>() {

        val preferencesFile = PreferencesFile()
        val mysqlFile = MySQLFile()

        val content = Content()
        val expandableContent = ExpandableContent()
        val loginButton = ButtonType(getString(R.string.login), ButtonBar.ButtonData.OK_DONE)

        init {
            icon = Image(R.png.ic_launcher)
            title = getString(R.string.app_name)
            headerText = getString(R.string.login)
            graphic = ImageView(R.png.ic_launcher)
            isResizable = false

            dialogPane.content = content
            dialogPane.expandableContent = expandableContent

            dialogPane.buttonTypes.addAll(ButtonType.CANCEL, loginButton)
            dialogPane.lookupButton(loginButton).addEventFilter(ActionEvent.ACTION) { event ->
                event.consume()
                mysqlFile.save()
                if (InetAddress.getByName(content.ipField.text).isReachable(IP_LOOKUP_TIMEOUT)) {
                    errorAlert(getString(R.string.ip_address_unreachable)).showAndWait()
                    return@addEventFilter
                } else {

                    
                    Completable
                            .create {
                                try {
                                    MySQL.connect(content.ipField.text, content.portField.text, content.usernameField.text, "")
                                    it.onComplete()
                                } catch (e: Exception) {
                                    it.onError(e)
                                }
                            }
                            .subscribeOn(Schedulers.io())
                            .observeOn(JavaFxScheduler.platform())
                            .subscribeBy({ errorAlert(it.message ?: "Unknown error!").showAndWait() }) {
                                result = content.usernameField.text
                                close()
                            }
                }
            }
            dialogPane.lookupButton(loginButton).disableProperty().bind(content.usernameField.textProperty().isEmpty
                    or not(content.ipField.validProperty)
                    or content.portField.textProperty().isEmpty)

            content.usernameField.textProperty().bindBidirectional(mysqlFile[MySQLFile.USERNAME])
            content.ipField.textProperty().bindBidirectional(mysqlFile[MySQLFile.IP])
            content.portField.textProperty().bindBidirectional(mysqlFile[MySQLFile.PORT])

            runLater { content.usernameField.requestFocus() }
        }

        inner class Content : GridPane() {
            val languageLabel = Label(getString(R.string.language))
            val languageBox = ChoiceBox<Language>(Language.listAll()).apply { maxWidth = Double.MAX_VALUE }
            val usernameLabel = Label(getString(R.string.username))
            val usernameField = TextField(getString(R.string.username))
            val serverLabel = Label(getString(R.string.server))
            val ipField = IPField().apply {
                promptText = getString(R.string.ip_address)
                prefWidth = 128.0
            }
            val portField = IntField().apply {
                promptText = getString(R.string.port)
                prefWidth = 64.0
            }

            init {
                gaps = 8.0
                add(languageLabel, 0, 0)
                add(languageBox, 1, 0, 2, 1)
                add(usernameLabel, 0, 1)
                add(usernameField, 1, 1, 2, 1)
                add(serverLabel, 0, 2)
                add(ipField, 1, 2)
                add(portField, 2, 2)

                val initialLanguage = Language.parse(preferencesFile[PreferencesFile.LANGUAGE].value)
                languageBox.selectionModel.select(initialLanguage)
                languageBox.selectionModel.selectedItemProperty().addListener { _, _, newValue ->
                    preferencesFile.apply { get(PreferencesFile.LANGUAGE).set(newValue.locale) }.save()
                    close()
                    infoAlert(getString(R.string.language_changed)).showAndWait()
                    exitFX()
                }
            }
        }

        inner class ExpandableContent : VBox() {
            val aboutLabel = Label("MySQL version ${com.wijayaprinting.mysql.BuildConfig.VERSION}")
            val hyperlink = Hyperlink("https://github.com/WijayaPrinting/")

            init {
                spacing = 8.0
                children.addAll(aboutLabel, hyperlink)
            }
        }
    }
}