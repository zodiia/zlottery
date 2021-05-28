package me.zodiia.lottery.storage.entities

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class DrawEntity(id: EntityID<Long>): LongEntity(id) {
    companion object: LongEntityClass<DrawEntity>(DrawsTable)
    var winner by DrawsTable.winner
    var lotteryName by DrawsTable.lotteryName
    var time by DrawsTable.time
    var amount by DrawsTable.amount
}
