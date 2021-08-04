package me.zodiia.lottery.commands.lottery

import me.zodiia.api.command.command

val lotteryCommand = command {
    description = "Lottery base command"

    subcommand("help", helpCommand)
    subcommand("buy", buyCommand)
    subcommand("info", infoCommand)
    subcommand("last", lastCommand)
    subcommand("reload", reloadCommand)
}
