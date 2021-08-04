package me.zodiia.lottery

import me.zodiia.api.command.Commands
import me.zodiia.api.logger.Console
import me.zodiia.api.plugins.KotlinPlugin
import me.zodiia.lottery.commands.lottery.lotteryCommand
import me.zodiia.lottery.config.LotteryConfigRealm
import me.zodiia.lottery.storage.LotteryDataSource
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.PluginDescriptionFile
import org.bukkit.plugin.java.JavaPluginLoader
import java.io.File

class LotteryPlugin : KotlinPlugin {
    override val configRealm = LotteryConfigRealm(this)

    init {
        kotlinDescription {
            minecraftVersion(">= 1.16.5")

            pluginDependency("zApi", ">= 5.0 && < 6.0")

            file("config.yml")
            file("lang/fr.json")
            file("lotteries/example.yml")

            spigotId(-1)
        }
    }

    constructor() : super()
    constructor(loader: JavaPluginLoader, description: PluginDescriptionFile, dataFolder: File, file: File) : super(
        loader, description, dataFolder, file
    )

    override fun onEnable() {
        super.onEnable()

        Console.log("Initializing data source")
        LotteryDataSource.getSource()

        Console.log("Registering commands")
        Commands.register("lottery", this, lotteryCommand)
    }

    override fun onDisable() {
        super.onDisable()
        LotteryDataSource.close()
    }

    companion object {
        val plugin: LotteryPlugin by lazy { getPlugin(LotteryPlugin::class.java) }
    }
}
