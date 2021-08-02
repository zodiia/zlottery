package me.zodiia.lottery

import me.zodiia.api.command.Commands
import me.zodiia.api.config.KotlinConfigRealm
import me.zodiia.api.data.DataSourceProvider
import me.zodiia.api.plugins.KotlinPlugin
import me.zodiia.lottery.commands.lotteryCommand
import me.zodiia.lottery.commands.testCommand
import me.zodiia.lottery.config.LotteryConfigRealm
import me.zodiia.lottery.storage.LotteryDataSource
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.PluginDescriptionFile
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.plugin.java.JavaPluginLoader
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

class LotteryPlugin : KotlinPlugin {
    override val configRealm = LotteryConfigRealm(this)

    init {
        kotlinDescription {
            minecraftVersion(">= 1.16.5")

            pluginDependency("zApi", ">= 5.0 && < 6.0")

            file("config.yml")
            file("lang/fr.json")

            spigotId(-1)
        }
    }

    constructor() : super()
    constructor(loader: JavaPluginLoader, description: PluginDescriptionFile, dataFolder: File, file: File) : super(
        loader, description, dataFolder, file
    )

    override fun onEnable() {
        super.onEnable()

//        // Initialize the data source (avoid big lag at first request)
//        LotteryDataSource.getSource()

        // Setup /lottery command
        Commands.register("lottery", this, lotteryCommand)
        Commands.register("test", this, testCommand)
    }



    override fun onDisable() {
        super.onDisable()
        LotteryDataSource.close()
    }

    companion object {
        val plugin: Plugin by lazy { getPlugin(LotteryPlugin::class.java) }
    }
}
