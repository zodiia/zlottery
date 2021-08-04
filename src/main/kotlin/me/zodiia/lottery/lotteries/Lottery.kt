package me.zodiia.lottery.lotteries

import kotlinx.coroutines.reactor.flux
import kotlinx.coroutines.runBlocking
import me.zodiia.api.hooks.useI18n
import me.zodiia.api.scheduler.Scheduler
import me.zodiia.api.threads.Threads
import me.zodiia.api.util.Vault
import me.zodiia.lottery.storage.entities.DrawEntity
import me.zodiia.lottery.storage.entities.TicketEntity
import me.zodiia.lottery.storage.repositories.DrawsRepository
import me.zodiia.lottery.storage.repositories.TicketsRepository
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Locale
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Consumer

data class Lottery(
    val enabled: Boolean,
    val schedule: LotterySchedule,
    val displayName: String,
    val ticketValue: Double,
    val maxTicketsPerPlayer: Double,
    val tax: Double,
    val extraRewards: Array<String>,
) {
    lateinit var id: String
    var nextDraw: LocalDateTime? = null

    companion object {
        val i18n by useI18n()
    }

    fun getNextDrawText(): String {
        val offsetMinutes: Long = ChronoUnit.MINUTES.between(LocalDateTime.now(ZoneId.systemDefault()), nextDraw)

        return prettyPrintMinutes(offsetMinutes + 1)
    }

    fun scheduleNextDraw() {
        val nextReminders: Set<LocalDateTime> = schedule.getNextReminders()

        nextDraw = schedule.getNextDraw()
        Scheduler.schedule(nextDraw!!, { draw() }, "lottery.$id.draw")
        for (reminder in nextReminders) {
            Scheduler.schedule(reminder, { remind(reminder, nextDraw) }, "lottery.$id.reminders")
        }
    }

    private fun draw() {
        runBlocking {
            DrawsRepository.findLast(id)
                .next()
                .flatMapMany {
                    flux<TicketEntity> {
                        TicketsRepository.findAllAfter(it.time, id)
                    }
                }
                .collectList()
                .map {
                    if (it.isEmpty()) {
                        Bukkit.broadcastMessage(i18n.get("draw.noParticipants", mapOf("lottery" to displayName.lowercase(Locale.ROOT))))
                        return@map
                    }
                    val winner = pickWinner(it)
                    val currentDraw = DrawEntity.new {
                        this.winner = winner.player
                        this.lotteryName = this@Lottery.id
                        this.time = System.currentTimeMillis()
                        this.amount = it.sumOf(TicketEntity::amount)
                    }
                    val winnerPlayer = Bukkit.getOfflinePlayer(winner.player)

                    rewardWinner(currentDraw, winnerPlayer)
                    scheduleNextDraw()
                }
        }
    }

    private fun remind(draw: LocalDateTime?, reminder: LocalDateTime?) {
        val offsetMinutes: Long = ChronoUnit.MINUTES.between(draw, reminder)
        val offsetString = prettyPrintMinutes(offsetMinutes)
        Bukkit.broadcastMessage(
            i18n.get("reminder.reminder", mapOf(
            "lottery" to displayName.lowercase(Locale.ROOT),
            "time" to offsetString,
        )))
    }

    private fun prettyPrintMinutes(offsetMinutes: Long): String {
        val days = offsetMinutes / 1440
        val hours = offsetMinutes / 60 % 24
        val minutes = offsetMinutes % 60
        var displayedUnits = 0
        var finalString = ""

        if (days > 0) {
            finalString += "$days "
            finalString += if (days > 1) {
                i18n.get("time.days", mapOf())
            } else {
                i18n.get("time.day", mapOf())
            }
            displayedUnits += 1
        }
        if (hours > 0) {
            if (displayedUnits > 0) {
                finalString += " et "
            }
            finalString += "$hours "
            finalString += if (hours > 1) {
                i18n.get("time.hours", mapOf())
            } else {
                i18n.get("time.hour", mapOf())
            }
            displayedUnits += 1
            if (displayedUnits == 2) {
                return finalString
            }
        }
        if (minutes > 0) {
            if (displayedUnits > 0) {
                finalString += " et "
            }
            finalString += "$minutes "
            finalString += if (minutes > 1) {
                i18n.get("time.minutes", mapOf())
            } else {
                i18n.get("time.minute", mapOf())
            }
        }
        return finalString
    }

    private fun pickWinner(tickets: List<TicketEntity>): TicketEntity {
        val totalWeight = tickets.sumOf(TicketEntity::amount)
        val winningTicket = AtomicLong(ThreadLocalRandom.current().nextLong(totalWeight))
        val winner: AtomicReference<TicketEntity> = AtomicReference<TicketEntity>(null)
        tickets.forEach(Consumer { ticket: TicketEntity ->
            winningTicket.addAndGet(-ticket.amount)
            if (winningTicket.get() <= 0) {
                winner.set(ticket)
            }
        })
        return winner.get()
    }

    private fun rewardWinner(currentDraw: DrawEntity, winnerPlayer: OfflinePlayer) {
        Bukkit.broadcastMessage(
            i18n.get("draw.winner", mapOf(
            "lottery" to displayName.lowercase(Locale.ROOT),
            "player" to (winnerPlayer.name ?: "undefined"),
            "value" to "${currentDraw.amount * ticketValue * tax}",
        )))
        Vault.economy?.depositPlayer(winnerPlayer, currentDraw.amount * ticketValue * tax)
        Threads.runSync {
            extraRewards.forEach {
                Bukkit.dispatchCommand(Bukkit.getServer().consoleSender, it.replace("%player%".toRegex(), winnerPlayer.name ?: "undefined"))
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Lottery

        if (enabled != other.enabled) return false
        if (schedule != other.schedule) return false
        if (displayName != other.displayName) return false
        if (ticketValue != other.ticketValue) return false
        if (maxTicketsPerPlayer != other.maxTicketsPerPlayer) return false
        if (tax != other.tax) return false
        if (!extraRewards.contentEquals(other.extraRewards)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = enabled.hashCode()
        result = 31 * result + schedule.hashCode()
        result = 31 * result + displayName.hashCode()
        result = 31 * result + ticketValue.hashCode()
        result = 31 * result + maxTicketsPerPlayer.hashCode()
        result = 31 * result + tax.hashCode()
        result = 31 * result + extraRewards.contentHashCode()
        return result
    }
}
