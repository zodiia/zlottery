package me.zodiia.lottery.lotteries

import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import org.bukkit.configuration.ConfigurationSection
import java.util.HashSet
import java.time.LocalTime
import java.time.ZoneId

class LotterySchedule(
    val time: String,
    val day: String,
    val reminders: String,
) {
    private val dayList: MutableSet<Int> = hashSetOf()
    private val timeList: MutableSet<LocalTime> = hashSetOf()
    private val remindersList: MutableSet<Int> = hashSetOf()
    private var next: LocalDateTime? = null

    fun getNextReminders(): Set<LocalDateTime> {
        val nextReminders: MutableSet<LocalDateTime> = HashSet()
        if (next == null) {
            return nextReminders
        }
        for (reminder in reminders) {
            nextReminders.add(next!!.minusMinutes(reminder.code.toLong()))
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
        while (!dayList.contains(next?.dayOfWeek?.value?.rem(7) ?: -1)) {
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

        for (time in timeList) {
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
        val timeCfg = time.split(",").toTypedArray()
        val dayCfg = day.split(",").toTypedArray()
        val remindersCfg = reminders.split(",").toTypedArray()
        for (time in timeCfg) {
            val parts = time.split("-").toTypedArray()
            val localTime = LocalTime.of(parts[0].toInt(), parts[1].toInt(), 0)
            timeList.add(localTime)
        }
        for (day in dayCfg) {
            dayList.add(day.toInt())
        }
        for (reminder in remindersCfg) {
            remindersList.add(reminder.toInt())
        }
    }
}
