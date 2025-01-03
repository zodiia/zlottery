package me.zodiia.lottery.storage.entities

import me.zodiia.lottery.storage.LotteryDataSource
import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.LongIdTable

class TicketEntity(id: EntityID<Long>): LongEntity(id) {
    companion object: LongEntityClass<TicketEntity>(TicketsTable)
    var player by TicketsTable.player
    var lotteryName by TicketsTable.lotteryName
    var time by TicketsTable.time
    var amount by TicketsTable.amount
}

object TicketsTable: LongIdTable(name = LotteryDataSource.tableName("tickets")) {
    val player = uuid("player")
    val lotteryName = varchar("lottery", 64)
    val time = long("time")
    val amount = long("amount")
}
