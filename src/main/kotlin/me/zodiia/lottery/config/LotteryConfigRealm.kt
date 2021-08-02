package me.zodiia.lottery.config

import me.zodiia.api.config.KotlinConfigRealm
import me.zodiia.api.plugins.KotlinPlugin

class LotteryConfigRealm(plugin: KotlinPlugin) : KotlinConfigRealm(plugin) {
    init {
        loadLanguages("lang")
    }

    val config = load("config.yml", Config::class)
}
