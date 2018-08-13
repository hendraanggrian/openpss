@file:Suppress("NOTHING_TO_INLINE")

package com.hendraanggrian.openpss.util

/** The only reason why Apache Commons Math is used. */
inline fun Double.round(): Double = org.apache.commons.math3.util.Precision.round(this, 2)