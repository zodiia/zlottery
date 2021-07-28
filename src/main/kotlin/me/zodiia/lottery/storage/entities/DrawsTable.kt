package me.zodiia.lottery.storage.entities

import me.zodiia.lottery.storage.LotteryDataSource
import org.jetbrains.exposed.dao.id.LongIdTable

object DrawsTable: LongIdTable(name = "${LotteryDataSource.config.tablePrefix}draws") {
    val winner = uuid("winner")
    val lotteryName = varchar("lottery", 64)
    val time = long("time")
    val amount = long("amount")
}
