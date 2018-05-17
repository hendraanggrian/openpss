package com.hendraanggrian.openpss.localization

import java.util.ResourceBundle

/** Easier access to [ResourceBundle] across components. */
interface Resourced {

    val resources: ResourceBundle

    val language: Language get() = Language.ofCode(resources.baseBundleName.substringAfter('_'))

    fun getString(id: String): String = resources.getString(id)

    fun getString(id: String, vararg args: String): String = getString(id).format(*args)
}