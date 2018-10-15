package com.hendraanggrian.openpss.control.popover

import com.hendraanggrian.openpss.R
import com.hendraanggrian.openpss.control.ActionManager
import com.hendraanggrian.openpss.control.dialog.Dialog
import com.hendraanggrian.openpss.i18n.Resourced
import com.hendraanggrian.openpss.util.getColor
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.geometry.Pos.CENTER_LEFT
import javafx.geometry.Pos.CENTER_RIGHT
import javafx.scene.Node
import javafx.scene.control.ListView
import javafx.scene.control.TableView
import javafx.scene.layout.Pane
import ktfx.NodeManager
import ktfx.beans.value.getValue
import ktfx.beans.value.setValue
import ktfx.coroutines.listener
import ktfx.coroutines.onAction
import ktfx.layouts.borderPane
import ktfx.layouts.button
import ktfx.layouts.buttonBar
import ktfx.scene.layout.updatePadding
import ktfx.scene.text.fontSize
import org.controlsfx.control.PopOver

/** Base [PopOver] class used across applications. */
@Suppress("LeakingThis")
open class Popover(
    private val resourced: Resourced,
    titleId: String
) : PopOver(), NodeManager, ActionManager, Resourced by resourced {

    private val contentPane = Pane()

    override val collection: MutableCollection<Node> get() = contentPane.children

    private val graphicProperty = SimpleObjectProperty<Node>()
    fun graphicProperty(): ObjectProperty<Node> = graphicProperty
    var graphic: Node by graphicProperty

    init {
        contentNode = ktfx.layouts.vbox(R.dimen.padding_medium.toDouble()) {
            updatePadding(12.0, 16.0, 12.0, 16.0)
            borderPane {
                left = ktfx.layouts.label(getString(titleId)) {
                    fontSize = 18.0
                    textFill = getColor(R.color.blue)
                } align CENTER_LEFT
                rightProperty().run {
                    bindBidirectional(graphicProperty)
                    listener { _, _, value -> value align CENTER_RIGHT }
                }
            }
            contentPane()
            buttonBar {
                button(getString(R.string.close)) {
                    isCancelButton = true
                    onAction { hide() }
                }
                onCreateActions()
            } marginTop R.dimen.padding_medium.toDouble()
        }
    }

    fun showAt(node: Node) {
        // to avoid error when closing window/stage during popover display
        if (resourced is Dialog<*>) {
            resourced.dialogPane.scene.window.setOnCloseRequest {
                isAnimated = false
                hide()
            }
        }
        // now check for coordinate to show popover
        val selectedIndex = (node as? TableView<*>)?.selectionModel?.selectedIndex
            ?: (node as? ListView<*>)?.selectionModel?.selectedIndex
        when (selectedIndex) {
            null -> show(node)
            else -> {
                val bounds = node.localToScreen(node.boundsInLocal)
                show(node.scene.window,
                    bounds.minX + bounds.width,
                    bounds.minY + selectedIndex * 22.0 + (0 until selectedIndex).sumByDouble { 2.0 })
            }
        }
    }

    final override fun getString(id: String): String = super.getString(id)
}