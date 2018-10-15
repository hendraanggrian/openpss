package com.hendraanggrian.openpss.ui.wage.readers

import com.hendraanggrian.openpss.ui.wage.Attendee
import javafx.collections.ObservableList
import javafx.scene.control.Separator
import javafx.stage.FileChooser
import ktfx.collections.toMutableObservableList
import java.io.File

/** A file readers that generates collection of [Attendee] given input file. */
abstract class Reader {

    /** Identifier of a reader. */
    abstract val name: String

    /** Expected file extensions for [FileChooser.ExtensionFilter]. */
    abstract val extensions: Array<String>

    /**
     * The reading process is executed in background thread.
     * During its long operation, exception throwing may happen in [read].
     */
    @Throws(Exception::class)
    abstract suspend fun read(file: File): Collection<Attendee>

    override fun toString(): String = name

    companion object {
        private val READERS: List<Reader> get() = listOf(EClockingReader, TestReader)

        fun listAll(): ObservableList<Any> = READERS.toMutableObservableList<Any>().apply { add(1, Separator()) }

        fun of(name: String): Reader = READERS.single { it.name == name }
    }
}