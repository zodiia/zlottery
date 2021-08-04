package me.zodiia.lottery.commands.lottery

import kotlinx.coroutines.reactor.flux
import kotlinx.coroutines.runBlocking
import me.zodiia.api.command.command
import me.zodiia.api.threads.SpigotDispatchers
import me.zodiia.lottery.LotteryPlugin
import me.zodiia.lottery.commands.lottery.LotteryCommandHelper.i18n
import me.zodiia.lottery.lotteries.Lottery
import me.zodiia.lottery.storage.entities.TicketEntity
import me.zodiia.lottery.storage.repositories.DrawsRepository
import me.zodiia.lottery.storage.repositories.TicketsRepository

val infoCommand = command {
    permission = "lottery.info"

    argument(0) {
        completer { addAll(LotteryPlugin.plugin.configRealm.getLotteryIds()) }
    }

    executor { ctx ->
        val lottery: Lottery

        try {
            lottery = LotteryCommandHelper.getLottery(ctx, 0)
        } catch (th: Throwable) { return@executor }
        runBlocking {
            DrawsRepository.findLast(lottery.id)
                .next()
                .flatMapMany {
                    flux<TicketEntity> {
                        TicketsRepository.findAllAfter(it?.time ?: 0, ctx.args[0]!!)
                    }
                }
                .collectList()
                .map {
                    val totalTickets = LotteryCommandHelper.boughtTicketsTotal(it)

                    ctx.sender.sendMessage(i18n.get("info.header", mapOf("lottery" to lottery.displayName.lowercase())))
                    ctx.sender.sendMessage(
                        i18n.get("info.toBuyTickets", mapOf(
                            "command" to ctx.label.split(' ')[0],
                            "lottery" to (ctx.args[0] ?: "undefined"),
                        )))
                    ctx.sender.sendMessage(i18n.get("info.amountOfPlayers", mapOf("amount" to "${LotteryCommandHelper.getDifferentPlayersAmount(it)}")))
                    ctx.sender.sendMessage(
                        i18n.get("info.priceAndNumberOfTickets", mapOf(
                            "price" to "${lottery.ticketValue}",
                            "amount" to "${LotteryCommandHelper.boughtTicketsFor(ctx.player!!.uniqueId, it)}",
                            "max" to "${lottery.maxTicketsPerPlayer}"
                        )))
                    ctx.sender.sendMessage(
                        i18n.get("info.ticketsAndPrizePool", mapOf(
                            "amount" to "$totalTickets",
                            "value" to "${totalTickets * lottery.ticketValue * lottery.tax}"
                        )))
                    ctx.sender.sendMessage(i18n.get("info.nextDraw", mapOf("time" to lottery.getNextDrawText())))
                }
                .doOnError {
                    this@command.internalError(ctx, it)
                }
                .subscribeOn(SpigotDispatchers.Reactor)
        }
    }
}
