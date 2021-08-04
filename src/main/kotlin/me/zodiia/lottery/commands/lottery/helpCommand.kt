package me.zodiia.lottery.commands.lottery

import me.zodiia.api.command.HelpMenu
import me.zodiia.api.command.command
import me.zodiia.lottery.commands.lottery.LotteryCommandHelper.i18n

val help: HelpMenu by lazy {
    val help = HelpMenu("Loterie", "lottery")

    help.footer = "        &aMade with &c♥ §aby &eZodiia"
    i18n.getKeys("help").forEach {
        help.addCommand(i18n.get("help.$it.syntax"), i18n.get("help.$it.description"))
    }
    help.build()
    help
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
