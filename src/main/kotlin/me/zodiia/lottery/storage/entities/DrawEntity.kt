package me.zodiia.lottery.storage.entities

import me.zodiia.lottery.storage.LotteryDataSource
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable

class DrawEntity(id: EntityID<Long>): LongEntity(id) {
    companion object: LongEntityClass<DrawEntity>(DrawsTable)
    var winner by DrawsTable.winner
    var lotteryName by DrawsTable.lotteryName
    var time by DrawsTable.time
    var amount by DrawsTable.amount
}

object DrawsTable: LongIdTable(name = LotteryDataSource.tableName("draws")) {
    val winner = uuid("winner")
    val lotteryName = varchar("lottery", 64)
    val time = long("time")
    val amount = long("amount")
}
