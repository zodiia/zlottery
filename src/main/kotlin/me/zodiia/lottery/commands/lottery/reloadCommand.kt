package me.zodiia.lottery.commands.lottery

import me.zodiia.api.command.command
import me.zodiia.lottery.LotteryPlugin
import me.zodiia.lottery.commands.lottery.LotteryCommandHelper.i18n

val reloadCommand = command {
    permission = "lottery.reload"

    executor { ctx ->
        LotteryPlugin.plugin.configRealm.reloadConfig()
        ctx.sender.sendMessage(i18n.get("reload.done", mapOf()))
    }
}
