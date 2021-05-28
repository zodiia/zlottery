package me.zodiia.lottery.commands

import com.google.gson.JsonObject
import me.zodiia.api.command.*
import me.zodiia.api.threads.Threads
import me.zodiia.api.util.Vault
import me.zodiia.api.util.tryFct
import me.zodiia.lottery.LotteryConfig
import me.zodiia.lottery.LotteryConfig.language
import me.zodiia.lottery.lotteries.Lottery
import me.zodiia.lottery.storage.entities.TicketEntity
import me.zodiia.lottery.storage.repositories.DrawsRepository
import me.zodiia.lottery.storage.repositories.TicketsRepository
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.jetbrains.exposed.sql.SizedIterable
import java.sql.SQLException
import java.time.Instant
import java.time.ZoneId
import java.util.UUID
import kotlin.jvm.Throws

val help: HelpMenu by lazy {
    val help = HelpMenu("Loterie", "lottery")

    help.footer = "        &aMade with &c♥ §aby &eZodiia"
    language.json.getAsJsonObject("help").entrySet().forEach {
        val helpLine: JsonObject = it.value.asJsonObject
        help.addCommand(helpLine["syntax"].asString, helpLine["description"].asString)
    }
    help.build()
    help
}

val lotteryCommand = command {
    description = "Lottery base command"
    aliases = listOf("loterie", "euromillions")

    subcommand("help", helpCommand)
    subcommand("buy", buyCommand)
    subcommand("info", infoCommand)
    subcommand("last", lastCommand)
    subcommand("reload", reloadCommand)
}

val helpCommand = command {
    permission = "lottery.help"

    argument(0) {
        required = false

        staticCompleter { addAll(1 until (help.getTotalPages() + 1)) }
    }

    executor { ctx ->
        help.sendPage(ctx.args[0]?.toInt() ?: 1, ctx.sender)
    }
}

val buyCommand = command {
    permission = "lottery.buy"
    description = ""

    argument(0) {
        staticCompleter { addAll(listOf(1, 2, 5, 10, 20)) }
        filter { tryFct { it.toInt() } == null }
    }

    argument(1) {
        completer { addAll(LotteryConfig.config.getConfigurationSection("lotteries")?.getKeys(false) ?: setOf()) }
    }

    argument(2) {
        permission = "lottery.buy.others"
        required = false

        completer { Bukkit.getOnlinePlayers().map { it.name } }
    }

    executor { ctx ->
        val tickets: Long
        val lottery: Lottery
        val player: Player

        try {
            tickets = Companion.getTickets(ctx, 0)
            lottery = Companion.getLottery(ctx, 1)
            player = Companion.getPlayer(ctx, 2)
        } catch (th: Throwable) { return@executor }
        if (Vault.economy?.getBalance(ctx.player!!)!! < tickets * lottery.ticketValue) {
            ctx.sender.sendMessage(language.get("errors.notEnoughMoney", mapOf(
                "amount" to "${(tickets * lottery.ticketValue - Vault.economy?.getBalance(ctx.player!!)!!)}"
            )))
            return@executor
        }
        Threads.runAsync {
            if (Companion.getPlayerTicketsCount(player.uniqueId, lottery.id) + tickets > lottery.maxTicketsPerPlayer) {
                ctx.sender.sendMessage(language.get("errors.tooMuchTickets", mapOf()))
            } else {
                Companion.buyTickets(ctx.player!!, player, lottery, tickets)
            }
        }
    }
}

val infoCommand = command {
    permission = "lottery.info"

    argument(0) {
        completer { addAll(LotteryConfig.config.getConfigurationSection("lotteries")?.getKeys(false) ?: setOf()) }
    }

    executor { ctx ->
        val lottery: Lottery

        try {
            lottery = Companion.getLottery(ctx, 0)
        } catch (th: Throwable) { return@executor }
        Threads.runAsync {
            val lastDraw = DrawsRepository.findLast(lottery.id).find { true }
            val tickets = TicketsRepository.findAllAfter(lastDraw?.time ?: 0, ctx.args[0]!!)
            val totalTickets = Companion.boughtTicketsTotal(tickets)

            ctx.sender.sendMessage(language.get("info.header", mapOf("lottery" to lottery.displayName.toLowerCase())))
            ctx.sender.sendMessage(language.get("info.toBuyTickets", mapOf(
                "command" to ctx.label.split(' ')[0],
                "lottery" to (ctx.args[0] ?: "undefined"),
            )))
            ctx.sender.sendMessage(language.get("info.amountOfPlayers", mapOf("amount" to "${Companion.getDifferentPlayersAmount(tickets)}")))
            ctx.sender.sendMessage(language.get("info.priceAndNumberOfTickets", mapOf(
                "price" to "${lottery.ticketValue}",
                "amount" to "${Companion.boughtTicketsFor(ctx.player!!.uniqueId, tickets)}",
                "max" to "${lottery.maxTicketsPerPlayer}"
            )))
            ctx.sender.sendMessage(language.get("info.ticketsAndPrizePool", mapOf(
                "amount" to "$totalTickets",
                "value" to "${totalTickets * lottery.ticketValue * lottery.tax}"
            )))
            ctx.sender.sendMessage(language.get("info.nextDraw", mapOf("time" to lottery.getNextDrawText())))
        }
    }
}

val lastCommand = command {
    permission = "lottery.last"

    argument(0) {
        completer { addAll(LotteryConfig.config.getConfigurationSection("lotteries")?.getKeys(false) ?: setOf()) }
    }

    executor { ctx ->
        val lottery: Lottery

        try {
            lottery = Companion.getLottery(ctx, 0)
        } catch (th: Throwable) { return@executor }
        Threads.runAsync {
            val lastDraws = DrawsRepository.findLast(lottery.id, 10).sortedByDescending { it.time }

            ctx.sender.sendMessage(language.get("last.header", mapOf("lottery" to lottery.displayName.toLowerCase())))
            lastDraws.forEach {
                ctx.sender.sendMessage(language.get("last.drawLine", mapOf(
                    "date" to Companion.dateToString(it.time),
                    "player" to (Bukkit.getOfflinePlayer(it.winner).name ?: "undefined"),
                    "amount" to "${it.amount * lottery.ticketValue * lottery.tax}",
                )))
            }
        }
    }
}

val reloadCommand = command {
    permission = "lottery.reload"

    executor { ctx ->
        LotteryConfig.reload()
        ctx.sender.sendMessage(language.get("reload.done", mapOf()))
    }
}

private object Companion {
    fun getTickets(context: Context, idx: Int): Long {
        try {
            return context.args[idx]!!.toLong()
        } catch (th: Throwable) {
            context.sender.sendMessage(language.get("errors.mustBePositiveNumber", mapOf("amount" to context.args[idx]!!)))
            throw IllegalStateException()
        }
    }

    fun getLottery(context: Context, idx: Int): Lottery {
        val lottery = LotteryConfig.getLottery(context.args[idx]!!)

        if (lottery == null) {
            context.sender.sendMessage(language.get("errors.noSuchLottery", mapOf()))
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
            context.sender.sendMessage(language.get("errors.playerOffline", mapOf("player" to context.args[idx]!!)))
            throw IllegalStateException()
        }
        return player
    }

    @Throws(SQLException::class)
    fun getPlayerTicketsCount(player: UUID, lottery: String): Long {
        val lastDraw = DrawsRepository.findLast(lottery).find { true }
        val tickets = TicketsRepository.findAllForAfter(player, lastDraw?.time ?: 0, lottery)
        var count = 0L

        tickets.forEach { count += it.amount }
        return count
    }

    @Throws(SQLException::class)
    fun buyTickets(sender: Player, player: Player, lottery: Lottery, tickets: Long) {
        val entity = TicketEntity.new {
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
            sender.sendMessage(language.get("buy.boughtSelf", messageMapping))
            Bukkit.broadcastMessage(language.get("buy.broadcastSelf", messageMapping))
        } else {
            sender.sendMessage(language.get("buy.boughtOther", messageMapping))
            Bukkit.broadcastMessage(language.get("buy.broadcastOther", messageMapping))
        }
    }

    fun getDifferentPlayersAmount(tickets: SizedIterable<TicketEntity>): Int = tickets
        .distinctBy { it.player }
        .size

    fun boughtTicketsFor(player: UUID, tickets: SizedIterable<TicketEntity>): Long = tickets
        .filter { it.player.toString() == player.toString() }
        .map { it.amount }
        .reduce { a, b -> a + b }

    fun boughtTicketsTotal(tickets: SizedIterable<TicketEntity>): Long = tickets
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

    fun numberLeadingZero(value: Int): String =
        if (value < 10) "0$value"
        else "$value"
}
