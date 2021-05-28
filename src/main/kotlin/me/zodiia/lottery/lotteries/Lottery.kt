package me.zodiia.lottery.lotteries

import me.zodiia.api.scheduler.Scheduler
import me.zodiia.api.threads.Threads
import me.zodiia.api.util.Vault
import me.zodiia.lottery.LotteryConfig
import me.zodiia.lottery.storage.entities.DrawEntity
import me.zodiia.lottery.storage.entities.TicketEntity
import me.zodiia.lottery.storage.repositories.DrawsRepository
import me.zodiia.lottery.storage.repositories.TicketsRepository
import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import org.bukkit.configuration.ConfigurationSection
import org.jetbrains.exposed.sql.SizedIterable
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Locale
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Consumer

class Lottery(val id: String, cfg: ConfigurationSection) {
    val schedule: LotterySchedule
    val displayName: String
    val ticketValue: Double
    val maxTicketsPerPlayer: Long
    val tax: Double
    val commandRewards: List<String>
    var nextDraw: LocalDateTime? = null

    init {
        schedule = LotterySchedule(cfg.getConfigurationSection("schedule"))
        displayName = cfg.getString("display-name", "undefined")!!
        ticketValue = cfg.getDouble("ticket-value")
        maxTicketsPerPlayer = cfg.getLong("max-tickets-per-player")
        tax = 1 - cfg.getDouble("tax") / 100.0
        commandRewards = cfg.getStringList("extra-rewards")
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
        Threads.runAsync {
            try {
                val lastDraw: DrawEntity? = DrawsRepository.findLast(id).find { true }
                val drawTickets = TicketsRepository.findAllAfter(lastDraw?.time ?: 0, id)

                if (drawTickets.empty()) {
                    Bukkit.broadcastMessage(LotteryConfig.language.get("draw.noParticipants", mapOf("lottery" to displayName.toLowerCase(Locale.ROOT))))
                    return@runAsync
                }
                val winner = pickWinner(drawTickets)
                val currentDraw = DrawEntity.new {
                    this.winner = winner.player
                    this.lotteryName = this@Lottery.id
                    this.time = System.currentTimeMillis()
                    this.amount = drawTickets.sumOf(TicketEntity::amount)
                }
                val winnerPlayer = Bukkit.getOfflinePlayer(winner.player)

                rewardWinner(currentDraw, winnerPlayer)
                scheduleNextDraw()
            } catch (err: Throwable) {
                err.printStackTrace()
                throw IllegalStateException("An exception occured while drawing the lottery.", err)
            }
        }
    }

    private fun remind(draw: LocalDateTime?, reminder: LocalDateTime?) {
        val offsetMinutes: Long = ChronoUnit.MINUTES.between(draw, reminder)
        val offsetString = prettyPrintMinutes(offsetMinutes)
        Bukkit.broadcastMessage(LotteryConfig.language.get("reminder.reminder", mapOf(
            "lottery" to displayName.toLowerCase(Locale.ROOT),
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
                LotteryConfig.language.get("time.days", mapOf())
            } else {
                LotteryConfig.language.get("time.day", mapOf())
            }
            displayedUnits += 1
        }
        if (hours > 0) {
            if (displayedUnits > 0) {
                finalString += " et "
            }
            finalString += "$hours "
            finalString += if (hours > 1) {
                LotteryConfig.language.get("time.hours", mapOf())
            } else {
                LotteryConfig.language.get("time.hour", mapOf())
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
                LotteryConfig.language.get("time.minutes", mapOf())
            } else {
                LotteryConfig.language.get("time.minute", mapOf())
            }
        }
        return finalString
    }

    private fun pickWinner(tickets: SizedIterable<TicketEntity>): TicketEntity {
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
        Bukkit.broadcastMessage(LotteryConfig.language.get("draw.winner", mapOf(
            "lottery" to displayName.toLowerCase(Locale.ROOT),
            "player" to (winnerPlayer.name ?: "undefined"),
            "value" to "${currentDraw.amount * ticketValue * tax}",
        )))
        Vault.economy?.depositPlayer(winnerPlayer, currentDraw.amount * ticketValue * tax)
        Threads.runSync {
            commandRewards.forEach {
                Bukkit.dispatchCommand(Bukkit.getServer().consoleSender, it.replace("%player%".toRegex(), winnerPlayer.name ?: "undefined"))
            }
        }
    }
}
