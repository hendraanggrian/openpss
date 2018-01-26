package com.wijayaprinting.ui.main

import com.wijayaprinting.BuildConfig.VERSION
import com.wijayaprinting.R
import com.wijayaprinting.ui.Resourced
import com.wijayaprinting.ui.scene.control.GraphicListCell
import com.wijayaprinting.util.getFont
import javafx.event.ActionEvent.ACTION
import javafx.geometry.Insets
import javafx.geometry.Pos.CENTER_LEFT
import javafx.scene.Node
import javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE
import javafx.scene.control.ButtonType
import javafx.scene.control.ButtonType.CLOSE
import javafx.scene.control.Dialog
import javafx.scene.control.ListView
import javafx.scene.image.Image
import kotfx.*
import java.awt.Desktop.getDesktop
import java.net.URI

class AboutDialog(resourced: Resourced) : Dialog<Unit>(), Resourced by resourced {

    init {
        title = getString(R.string.about)
        content = hbox {
            padding = Insets(48.0)
            imageView(Image(R.png.logo_launcher)) {
                fitWidth = 172.0
                fitHeight = 172.0
            }
            vbox {
                alignment = CENTER_LEFT
                textFlow {
                    text("Wijaya ") { font = getFont(R.ttf.lato_bold, 24) }
                    text("Printing") { font = getFont(R.ttf.lato_light, 24) }
                }
                text("${getString(R.string.version)} $VERSION") { font = getFont(R.ttf.lato_regular, 12) } marginTop 2
                text(getString(R.string.about_notice)) { font = getFont(R.ttf.lato_bold, 12) } marginTop 20
                textFlow {
                    text("${getString(R.string.powered_by)}  ") { font = getFont(R.ttf.lato_bold, 12) }
                    text("JavaFX") { font = getFont(R.ttf.lato_regular, 12) }
                } marginTop 4
                textFlow {
                    text("${getString(R.string.author)}  ") { font = getFont(R.ttf.lato_bold, 12) }
                    text("Hendra Anggrian") { font = getFont(R.ttf.lato_regular, 12) }
                } marginTop 4
                hbox {
                    button("GitHub") { setOnAction { getDesktop().browse(URI("https://github.com/hendraanggrian/wijayaprinting")) } }
                    button(getString(R.string.check_for_updates)) { setOnAction { getDesktop().browse(URI("https://github.com/hendraanggrian/wijayaprinting/releases")) } } marginLeft 8
                } marginTop 20
            } marginLeft 48
        }
        lateinit var listView: ListView<License>
        expandableContent = hbox {
            listView = kotfx.listView {
                prefHeight = 256.0
                items = License.values().toObservableList()
                setCellFactory {
                    object : GraphicListCell<License>() {
                        override operator fun get(item: License): Node = kotfx.vbox {
                            label(item.repo) { font = getFont(R.ttf.lato_regular, 12) }
                            label(item.owner) { font = getFont(R.ttf.lato_bold, 12) }
                        }
                    }
                }
            }
            titledPane(getString(R.string.open_source_software), listView) { isCollapsible = false }
            titledPane(getString(R.string.license), kotfx.textArea {
                prefHeight = 256.0
                isEditable = false
                textProperty() bind stringBindingOf(listView.selectionModel.selectedIndexProperty()) {
                    listView.selectionModel.selectedItem?.content ?: getString(R.string.select_license)
                }
            }) { isCollapsible = false }
        }
        button(ButtonType("Homepage", CANCEL_CLOSE)).apply {
            visibleProperty() bind (dialogPane.expandedProperty() and booleanBindingOf(listView.selectionModel.selectedIndexProperty()) { listView.selectionModel.selectedItem != null })
            addEventFilter(ACTION) {
                it.consume()
                getDesktop().browse(URI(listView.selectionModel.selectedItem.homepage))
            }
        }
        button(CLOSE)
    }
}