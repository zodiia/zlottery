package me.zodiia.lottery.config

import me.zodiia.api.i18n.I18nLanguage
import me.zodiia.api.scheduler.Scheduler
import me.zodiia.lottery.LotteryPlugin
import me.zodiia.lottery.lotteries.Lottery
import org.bukkit.configuration.ConfigurationSection
import java.util.HashSet
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.util.function.Consumer
import kotlin.properties.Delegates

object LotteryConfig {
    var config: YamlConfiguration by Delegates.notNull()
        private set
    var language: I18nLanguage by Delegates.notNull()
        private set
    var databaseConfig: ConfigurationSection by Delegates.notNull()
        private set
    private var lotteries: HashSet<Lottery>

    init {
        lotteries = hashSetOf()
        reloadConfig()
    }

    private fun reloadConfig() {
        config = YamlConfiguration.loadConfiguration(File(LotteryPlugin.plugin.dataFolder, "config.yml"))
        language = I18nLanguage("default", File(LotteryPlugin.plugin.dataFolder, "lang/fr.json"))
        databaseConfig = config.getConfigurationSection("database")!!

        // Load lotteries
        val lotteriesSection = config.getConfigurationSection("lotteries")!!
        Scheduler.cancelTasks("lottery")
        lotteries = hashSetOf()
        lotteriesSection.getKeys(false).forEach(Consumer { lotteryKey: String ->
            lotteries.add(
                Lottery(lotteryKey, lotteriesSection.getConfigurationSection(lotteryKey)!!)
            )
        })
        lotteries.forEach(Consumer { obj: Lottery -> obj.scheduleNextDraw() })
    }

    fun getLottery(id: String): Lottery? {
        for (lottery in lotteries) {
            if (lottery.id == id) {
                return lottery
            }
        }
        return null
    }

    fun reload() {
        reloadConfig()
    }
}
