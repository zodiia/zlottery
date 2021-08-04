package me.zodiia.lottery.commands.lottery

import kotlinx.coroutines.runBlocking
import me.zodiia.api.command.command
import me.zodiia.lottery.LotteryPlugin
import me.zodiia.lottery.commands.lottery.LotteryCommandHelper.i18n
import me.zodiia.lottery.lotteries.Lottery
import me.zodiia.lottery.storage.repositories.DrawsRepository
import org.bukkit.Bukkit

val lastCommand = command {
    permission = "lottery.last"

    argument(0) {
        completer { addAll(LotteryPlugin.plugin.configRealm.getLotteryIds()) }
    }

    executor { ctx ->
        val lottery: Lottery

        try {
            lottery = LotteryCommandHelper.getLottery(ctx, 0)
        } catch (th: Throwable) { return@executor }
        runBlocking {
            DrawsRepository.findLast(lottery.id, 10)
                .collectList()
                .map { draws ->
                    ctx.sender.sendMessage(i18n.get("last.header", mapOf("lottery" to lottery.displayName.lowercase())))
                    draws.forEach {
                        ctx.sender.sendMessage(
                            i18n.get("last.drawLine", mapOf(
                                "date" to LotteryCommandHelper.dateToString(it.time),
                                "player" to (Bukkit.getOfflinePlayer(it.winner).name ?: "undefined"),
                                "amount" to "${it.amount * lottery.ticketValue * lottery.tax}",
                            )))
                    }
                }
        }
    }
}
