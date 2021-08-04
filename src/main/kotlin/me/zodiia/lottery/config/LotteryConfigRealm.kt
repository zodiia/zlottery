package me.zodiia.lottery.config

import me.zodiia.api.config.KotlinConfigRealm
import me.zodiia.api.plugins.KotlinPlugin
import me.zodiia.api.scheduler.Scheduler
import me.zodiia.lottery.LotteryPlugin
import me.zodiia.lottery.lotteries.Lottery
import java.io.File

class LotteryConfigRealm(plugin: KotlinPlugin) : KotlinConfigRealm(plugin) {
    lateinit var config: Config
    val lotteries: HashSet<Lottery> = hashSetOf()

    override fun reloadConfig() {
        // Unload
        lotteries.clear()
        Scheduler.cancelTasks("lottery")

        // Languages
        loadLanguages("lang")

        // Configs
        config = load("config.yml", Config::class)
        File(LotteryPlugin.plugin.dataFolder, "lotteries").listFiles()?.forEach {
            val lottery = load(it, Lottery::class)

            lottery.id = it.nameWithoutExtension
            lotteries.add(lottery)
        }
        lotteries.forEach { it.scheduleNextDraw() }
    }

    fun getLottery(id: String): Lottery? = lotteries.find { it.id == id }

    fun getLotteryIds() = lotteries.map { it.id }
}
