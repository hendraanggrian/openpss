package com.wijayaprinting.javafx.scene.layout

import com.wijayaprinting.javafx.scene.control.IntField
import com.wijayaprinting.mysql.utils.PATTERN_TIME
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.geometry.Pos.CENTER
import javafx.scene.control.Label
import javafx.scene.layout.HBox
import kotfx.bindings.bindingOf
import kotfx.bindings.booleanBindingOf
import org.joda.time.LocalTime
import org.joda.time.format.DateTimeFormat

open class TimeBox : HBox() {

    val hourBox = IntField()
    val minBox = IntField()

    val valueProperty = SimpleObjectProperty<LocalTime>()
    val validProperty = SimpleBooleanProperty()

    init {
        alignment = CENTER
        spacing = 8.0

        listOf(hourBox, minBox).forEach {
            it.text = "00"
            it.promptText = "00"
            it.maxWidth = 48.0
            it.alignment = CENTER
        }
        children.addAll(hourBox, Label(":"), minBox)

        valueProperty.bind(bindingOf(hourBox.textProperty(), minBox.textProperty()) {
            try {
                LocalTime.parse("${hourBox.text}:${minBox.text}", DateTimeFormat.forPattern(PATTERN_TIME))
            } catch (e: Exception) {
                LocalTime.MIDNIGHT
            }
        })
        validProperty.bind(booleanBindingOf(hourBox.textProperty(), minBox.textProperty()) {
            try {
                LocalTime.parse("${hourBox.text}:${minBox.text}", DateTimeFormat.forPattern(PATTERN_TIME))
                true
            } catch (e: Exception) {
                false
            }
        })
    }

    var value: LocalTime
        get() = valueProperty.get()
        set(value) = valueProperty.set(value)

    val isValid: Boolean = validProperty.get()
}