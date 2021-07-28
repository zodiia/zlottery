package me.zodiia.lottery.commands

import me.zodiia.api.command.command
import me.zodiia.api.logger.Console
import org.spigotmc.CustomTimingsHandler
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.nio.charset.StandardCharsets

val testCommand = command {
    executor {
        val baos = ByteArrayOutputStream()
        val ps = PrintStream(baos, true, StandardCharsets.UTF_8.name())

        CustomTimingsHandler.printTimings(ps)
        Console.log(baos.toString(StandardCharsets.UTF_8))
    }
}
