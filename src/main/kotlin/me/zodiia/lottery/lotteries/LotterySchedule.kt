package me.zodiia.lottery.lotteries

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import org.bukkit.configuration.ConfigurationSection
import java.util.HashSet
import java.time.LocalTime
import java.time.ZoneId

class LotterySchedule(cfg: ConfigurationSection?) {
    private val daysOfWeek: MutableSet<Int> = hashSetOf()
    private val times: MutableSet<LocalTime> = hashSetOf()
    private val reminders: MutableSet<Int> = hashSetOf()
    private var next: LocalDateTime? = null

    fun getNextReminders(): Set<LocalDateTime> {
        val nextReminders: MutableSet<LocalDateTime> = HashSet()
        if (next == null) {
            return nextReminders
        }
        for (reminder in reminders) {
            nextReminders.add(next!!.minusMinutes(reminder.toLong()))
        }
        return nextReminders
    }

    fun getNextDraw(): LocalDateTime? {
        if (next == null || next!!.isBefore(LocalDateTime.now(ZoneId.systemDefault()))) {
            setNextDraw()
        }
        return next
    }

    private fun setNextDraw() {
        var isAnotherDay = false
        var nextTime: LocalTime?
        next = LocalDateTime.now(ZoneId.systemDefault())
        while (!daysOfWeek.contains(next?.dayOfWeek?.value?.rem(7) ?: -1)) {
            next = next?.plusDays(1)
            isAnotherDay = true
        }
        if (isAnotherDay) {
            nextTime = findFirstTimeAfter(LocalTime.MIDNIGHT)
        } else {
            nextTime = next?.toLocalTime()?.let { findFirstTimeAfter(it) }
            if (nextTime == null) {
                next = next?.plusDays(1)
                nextTime = findFirstTimeAfter(LocalTime.MIDNIGHT)
            }
        }
        next = next?.toLocalDate()?.atTime(nextTime)
    }

    private fun findFirstTimeAfter(moment: LocalTime): LocalTime? {
        var firstTime: LocalTime? = null
        var smallestOffset: Long = -1

        for (time in times) {
            if (time.isAfter(moment)) {
                val offset = ChronoUnit.SECONDS.between(moment, time)
                if (smallestOffset > offset || smallestOffset == -1L) {
                    smallestOffset = offset
                    firstTime = time
                }
            }
        }
        return firstTime
    }

    init {
        val timesCfg = cfg!!.getString("time")!!.split(",").toTypedArray()
        val daysCfg = cfg.getString("day")!!.split(",").toTypedArray()
        val remindersCfg = cfg.getString("reminders")!!.split(",").toTypedArray()
        for (time in timesCfg) {
            val parts = time.split("-").toTypedArray()
            val localTime = LocalTime.of(parts[0].toInt(), parts[1].toInt(), 0)
            times.add(localTime)
        }
        for (day in daysCfg) {
            daysOfWeek.add(day.toInt())
        }
        for (reminder in remindersCfg) {
            reminders.add(reminder.toInt())
        }
    }
}
