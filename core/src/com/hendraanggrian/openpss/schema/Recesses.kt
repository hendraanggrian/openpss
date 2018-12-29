package com.hendraanggrian.openpss.schema

import com.hendraanggrian.openpss.data.Recess
import com.hendraanggrian.openpss.nosql.Schema
import kotlinx.nosql.time

object Recesses : Schema<Recess>("$Recesses", Recess::class) {
    val start = time("start")
    val end = time("end")

    override fun toString(): String = "recesses"
}