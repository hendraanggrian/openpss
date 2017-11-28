package com.wijayaprinting.javafx.data

import com.hendraanggrian.rxexposed.SQLCompletables
import com.hendraanggrian.rxexposed.SQLSingles
import com.wijayaprinting.javafx.utils.multithread
import com.wijayaprinting.mysql.dao.Wage
import com.wijayaprinting.mysql.dao.Wages
import javafx.beans.property.*
import javafx.collections.ObservableList
import kotfx.bindings.doubleBindingOf
import kotfx.collections.mutableObservableListOf
import org.apache.commons.math3.util.Precision.round
import org.jetbrains.exposed.sql.update
import org.joda.time.DateTime
import org.joda.time.Minutes.minutes
import org.joda.time.Period
import java.math.BigDecimal
import java.util.Optional.ofNullable

/** Data class representing an Employee with 'no' as its identifier to avoid duplicates in [Set] scenario. */
data class Employee(
        /** Id and name are final values that should be determined upon xlsx reading. */
        val id: Int,
        val name: String,

        /** Attendances and shift should be set with [EmployeeTitledPane]. */
        val attendances: ObservableList<DateTime> = mutableObservableListOf(),
        private val duplicates: ObservableList<DateTime> = mutableObservableListOf(),

        /** Wages below are retrieved from sql, or empty if there is none. */
        val daily: IntegerProperty = SimpleIntegerProperty(),
        val hourlyOvertime: IntegerProperty = SimpleIntegerProperty(),
        val recess: DoubleProperty = SimpleDoubleProperty()
) {
    companion object {
        const val WORKING_HOURS = 8.0
    }

    init {
        SQLSingles.transaction { ofNullable(Wage.findById(id)) }
                .multithread()
                .filter { it.isPresent }
                .map { it.get() }
                .subscribe({
                    daily.value = it.daily
                    hourlyOvertime.value = it.hourlyOvertime
                    recess.value = it.recess.toDouble()
                }) {}
    }

    fun saveWage() {
        SQLCompletables
                .transaction {
                    when (Wage.findById(id)) {
                        null -> Wage.new(id) {
                            daily = this@Employee.daily.value
                            hourlyOvertime = this@Employee.hourlyOvertime.value
                            recess = BigDecimal.valueOf(this@Employee.recess.value)
                        }
                        else -> Wages.update({ Wages.id eq id }) {
                            it[daily] = this@Employee.daily.value
                            it[hourlyOvertime] = this@Employee.hourlyOvertime.value
                            it[recess] = BigDecimal.valueOf(this@Employee.recess.value)
                        }
                    }
                }.subscribe()
    }

    fun addAttendance(element: DateTime) {
        attendances.add(element)
        duplicates.add(element)
    }

    fun addAllAttendances(elements: Collection<DateTime>) {
        attendances.addAll(elements)
        duplicates.addAll(elements)
    }

    fun revert() {
        attendances.clear()
        attendances.addAll(duplicates)
    }

    fun mergeDuplicates() {
        val toRemove = (0 until (attendances.size - 1))
                .filter { Period(attendances[it], attendances[it + 1]).toStandardMinutes().isLessThan(minutes(5)) }
                .map { attendances[it] }
        attendances.removeAll(toRemove)
        duplicates.removeAll(toRemove)
    }

    override fun hashCode(): Int = id.hashCode()
    override fun equals(other: Any?): Boolean = other != null && other is Employee && other.id == id

    override fun toString(): String = "$id. $name"

    fun toNodeRecord(): Record = Record(Record.TYPE_NODE, this, SimpleObjectProperty(DateTime.now()), SimpleObjectProperty(DateTime.now()))

    fun toChildRecords(): Set<Record> {
        val records = mutableSetOf<Record>()
        val iterator = attendances.iterator()
        while (iterator.hasNext()) {
            records.add(Record(Record.TYPE_CHILD, this, SimpleObjectProperty(iterator.next()), SimpleObjectProperty(iterator.next())))
        }
        return records
    }

    fun toTotalRecords(childs: Collection<Record>): Record = Record(Record.TYPE_TOTAL, this, SimpleObjectProperty(DateTime(0)), SimpleObjectProperty(DateTime(0))).apply {
        childs.map { it.daily }.toTypedArray().let { mains ->
            daily.bind(doubleBindingOf(*mains) {
                round(mains.map { it.value }.sum(), 2)
            })
        }
        childs.map { it.dailyIncome }.toTypedArray().let { mainIncomes ->
            dailyIncome.bind(doubleBindingOf(*mainIncomes) {
                round(mainIncomes.map { it.value }.sum(), 2)
            })
        }
        childs.map { it.overtime }.toTypedArray().let { overtimes ->
            overtime.bind(doubleBindingOf(*overtimes) {
                round(overtimes.map { it.value }.sum(), 2)
            })
        }
        childs.map { it.overtimeIncome }.toTypedArray().let { overtimeIncomes ->
            overtimeIncome.bind(doubleBindingOf(*overtimeIncomes) {
                round(overtimeIncomes.map { it.value }.sum(), 2)
            })
        }
        childs.map { it.total }.toTypedArray().let { totals ->
            total.bind(doubleBindingOf(*totals) {
                round(totals.map { it.value }.sum(), 2)
            })
        }
    }
}