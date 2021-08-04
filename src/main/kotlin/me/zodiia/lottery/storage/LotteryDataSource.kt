package me.zodiia.lottery.storage

import me.zodiia.api.data.DataSourceProvider
import me.zodiia.lottery.LotteryPlugin

object LotteryDataSource: DataSourceProvider(
    dataFolder = LotteryPlugin.plugin.dataFolder,
    config = LotteryPlugin.plugin.configRealm.config.database,
) {
    fun tableName(name: String) = "${config.tablePrefix}$name"
}
