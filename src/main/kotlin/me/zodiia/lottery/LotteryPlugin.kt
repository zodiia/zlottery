package me.zodiia.lottery

import me.zodiia.api.command.Commands
import me.zodiia.lottery.commands.lotteryCommand
import me.zodiia.lottery.storage.datasource.DataSourceProvider
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.PluginDescriptionFile
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.plugin.java.JavaPluginLoader
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

class LotteryPlugin : JavaPlugin {
    constructor() : super()
    constructor(loader: JavaPluginLoader, description: PluginDescriptionFile, dataFolder: File, file: File) : super(
        loader, description, dataFolder, file
    )

    override fun onEnable() {
        // Save files from classpath
        dataFolder.mkdirs()
        val filesToSave = arrayOf("config.yml", "lang.json")
        for (fileToSave in filesToSave) {
            try {
                val newFile = File(dataFolder, fileToSave)
                if (newFile.exists()) {
                    continue
                }
                saveFile(javaClass.getResourceAsStream("/$fileToSave"), newFile)
            } catch (err: IOException) {
                throw IllegalStateException("Could not save a file from classpath.", err)
            }
        }

        // Initialize the data source (avoid big lag at first request)
        DataSourceProvider.getSource()

        // Setup /lottery command
        Commands.register("lottery", this, lotteryCommand)
    }

    override fun onDisable() {
        DataSourceProvider.close()
    }

    @Throws(IOException::class)
    private fun saveFile(stream: InputStream?, path: File) {
        if (stream == null) {
            return
        }
        val output: OutputStream = FileOutputStream(path)
        val buffer = ByteArray(stream.available())
        stream.read(buffer)
        output.write(buffer)
    }

    companion object {
        val plugin: Plugin by lazy { getPlugin(LotteryPlugin::class.java) }
    }
}
