package me.zodiia.lottery.storage.entities

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class TicketEntity(id: EntityID<Long>): LongEntity(id) {
    companion object: LongEntityClass<TicketEntity>(TicketsTable)
    var player by TicketsTable.player
    var lotteryName by TicketsTable.lotteryName
    var time by TicketsTable.time
    var amount by TicketsTable.amount
}
