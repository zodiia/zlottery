package me.zodiia.lottery.storage.entities

import me.zodiia.lottery.storage.datasource.DataSourceProvider
import org.jetbrains.exposed.dao.id.LongIdTable

object TicketsTable: LongIdTable(name = "${DataSourceProvider.config.tablePrefix}tickets") {
    val player = uuid("player")
    val lotteryName = varchar("lottery", 64)
    val time = long("time")
    val amount = long("amount")
}
