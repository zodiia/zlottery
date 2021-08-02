package me.zodiia.lottery.storage

import me.zodiia.api.data.DataSourceProvider
import me.zodiia.lottery.config.LotteryConfig
import me.zodiia.lottery.LotteryPlugin

object LotteryDataSource: DataSourceProvider(
    dataFolder = LotteryPlugin.plugin.dataFolder,
    yamlConfig = LotteryConfig.databaseConfig,
)
