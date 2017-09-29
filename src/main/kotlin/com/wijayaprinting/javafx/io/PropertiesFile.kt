package com.wijayaprinting.javafx.io

import com.wijayaprinting.javafx.utils.useInputStream
import com.wijayaprinting.javafx.utils.useOutputStream
import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.StringProperty
import java.io.File
import java.util.*
import kotlin.collections.HashMap

/**
 * @author Hendra Anggrian (hendraanggrian@gmail.com)
 */
abstract class PropertiesFile(child: String, vararg keys: Any) : File(WPFolder(), child), Map<String, StringProperty> {

    /** Properties reference to get, set, and finally save into this mysqlFile. */
    private val properties = Properties()
    /** Actual map that stores properties for bindings. */
    private val map = HashMap<String, StringProperty>()

    init {
        createNewFileIfNotExists()
        useInputStream { properties.load(it) }
        keys.forEach {
            when (it) {
                is String -> map.put(it, SimpleStringProperty(properties.getProperty(it)))
                is Pair<*, *> -> {
                    check(it.first is String)
                    check(it.second is String)
                    map.put(it.first as String, SimpleStringProperty(properties.getProperty(it.first as String, it.second as String)))
                }
                else -> throw UnsupportedOperationException()
            }
        }
    }

    @JvmOverloads
    fun save(comments: String? = null) {
        map.keys.forEach { properties.setProperty(it, map[it]!!.value) }
        useOutputStream { properties.store(it, comments) }
    }

    private fun createNewFileIfNotExists() {
        if (!exists()) createNewFile()
    }

    override val entries get() = map.entries
    override val keys get() = map.keys
    override val size get() = map.size
    override val values get() = map.values
    override fun containsKey(key: String) = map.containsKey(key)
    override fun containsValue(value: StringProperty) = map.containsValue(value)
    override fun get(key: String): StringProperty = map[key]!!
    override fun isEmpty() = map.isEmpty()
}