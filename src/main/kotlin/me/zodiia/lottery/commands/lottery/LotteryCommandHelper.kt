package me.zodiia.lottery.commands.lottery

import kotlinx.coroutines.reactor.flux
import me.zodiia.api.command.Context
import me.zodiia.api.hooks.useI18n
import me.zodiia.api.util.Vault
import me.zodiia.lottery.LotteryPlugin
import me.zodiia.lottery.lotteries.Lottery
import me.zodiia.lottery.storage.entities.TicketEntity
import me.zodiia.lottery.storage.repositories.DrawsRepository
import me.zodiia.lottery.storage.repositories.TicketsRepository
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import reactor.core.publisher.Mono
import java.sql.SQLException
import java.time.Instant
import java.time.ZoneId
import java.util.*
import kotlin.jvm.Throws

internal object LotteryCommandHelper {
    val i18n by useI18n()

    fun getTickets(context: Context, idx: Int): Long {
        try {
            return context.args[idx]!!.toLong()
        } catch (th: Throwable) {
            context.sender.sendMessage(i18n.get("errors.mustBePositiveNumber", mapOf("amount" to context.args[idx]!!)))
            throw IllegalStateException()
        }
    }

    fun getLottery(context: Context, idx: Int): Lottery {
        val lottery = LotteryPlugin.plugin.configRealm.getLottery(context.args[idx]!!)

        if (lottery == null) {
            context.sender.sendMessage(i18n.get("errors.noSuchLottery", mapOf()))
            throw IllegalStateException()
        }
        return lottery
    }

    fun getPlayer(context: Context, idx: Int): Player {
        if (context.args[idx] == null) {
            return context.player!!
        }
        val player = Bukkit.getPlayer(context.args[idx]!!)

        if (player == null) {
            context.sender.sendMessage(i18n.get("errors.playerOffline", mapOf("player" to context.args[idx]!!)))
            throw IllegalStateException()
        }
        return player
    }

    @Throws(SQLException::class)
    suspend fun getPlayerTicketsCount(player: UUID, lottery: String): Mono<Long> {
        return DrawsRepository
            .findLast(lottery)
            .next()
            .flatMapMany {
                flux<TicketEntity> {
                    TicketsRepository.findAllForAfter(player, it?.time ?: 0, lottery)
                }
            }
            .reduce(0L) { acc, it ->
                acc + it.amount
            }
    }

    @Throws(SQLException::class)
    fun buyTickets(sender: Player, player: Player, lottery: Lottery, tickets: Long) {
        TicketEntity.new {
            this.player = player.uniqueId
            this.lotteryName = lottery.id
            this.time = System.currentTimeMillis()
            this.amount = tickets
        }
        val value = lottery.ticketValue * tickets
        val messageMapping = mapOf(
            "amount" to "$tickets",
            "value" to "$value",
            "player" to sender.name,
            "receiver" to player.name,
        )

        Vault.economy?.withdrawPlayer(sender, value)
        if (sender.name == player.name) {
            sender.sendMessage(i18n.get("buy.boughtSelf", messageMapping))
            Bukkit.broadcastMessage(i18n.get("buy.broadcastSelf", messageMapping))
        } else {
            sender.sendMessage(i18n.get("buy.boughtOther", messageMapping))
            Bukkit.broadcastMessage(i18n.get("buy.broadcastOther", messageMapping))
        }
    }

    fun getDifferentPlayersAmount(tickets: List<TicketEntity>): Int = tickets
        .distinctBy { it.player }
        .size

    fun boughtTicketsFor(player: UUID, tickets: List<TicketEntity>): Long = tickets
        .filter { it.player.toString() == player.toString() }
        .map { it.amount }
        .reduce { a, b -> a + b }

    fun boughtTicketsTotal(tickets: List<TicketEntity>): Long = tickets
        .map { it.amount }
        .reduce { a, b -> a + b }

    fun dateToString(timeMillis: Long): String {
        val time = Instant.ofEpochMilli(timeMillis).atZone(ZoneId.systemDefault()).toLocalDateTime()

        return "${numberLeadingZero(time.dayOfMonth)}/" +
            "${numberLeadingZero(time.monthValue)}/" +
            "${time.year} " +
            "${numberLeadingZero(time.hour)}:" +
            numberLeadingZero(time.minute)
    }

    private fun numberLeadingZero(value: Int): String =
        if (value < 10) "0$value"
        else "$value"
}
