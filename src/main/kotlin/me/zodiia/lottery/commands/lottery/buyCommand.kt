package me.zodiia.lottery.commands.lottery

import kotlinx.coroutines.runBlocking
import me.zodiia.api.command.command
import me.zodiia.api.util.Vault
import me.zodiia.api.util.tryFct
import me.zodiia.lottery.LotteryPlugin
import me.zodiia.lottery.commands.lottery.LotteryCommandHelper.i18n
import me.zodiia.lottery.lotteries.Lottery
import org.bukkit.Bukkit
import org.bukkit.entity.Player

val buyCommand = command {
    permission = "lottery.buy"
    description = ""

    argument(0) {
        staticCompleter { addAll(listOf(1, 2, 5, 10, 20)) }
        filter { tryFct { it.toInt() } == null }
    }

    argument(1) {
        completer { addAll(LotteryPlugin.plugin.configRealm.getLotteryIds()) }
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
            tickets = LotteryCommandHelper.getTickets(ctx, 0)
            lottery = LotteryCommandHelper.getLottery(ctx, 1)
            player = LotteryCommandHelper.getPlayer(ctx, 2)
        } catch (th: Throwable) { return@executor }
        if (Vault.economy?.getBalance(ctx.player!!)!! < tickets * lottery.ticketValue) {
            ctx.sender.sendMessage(
                i18n.get("errors.notEnoughMoney", mapOf(
                "amount" to "${(tickets * lottery.ticketValue - Vault.economy?.getBalance(ctx.player!!)!!)}"
            )))
            return@executor
        }
        runBlocking {
            LotteryCommandHelper.getPlayerTicketsCount(player.uniqueId, lottery.id)
                .map {
                    if (it + tickets > lottery.maxTicketsPerPlayer) {
                        ctx.sender.sendMessage(i18n.get("errors.tooMuchTickets", mapOf()))
                    } else {
                        LotteryCommandHelper.buyTickets(ctx.player!!, player, lottery, tickets)
                    }
                }
        }
    }
}
