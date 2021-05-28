package me.zodiia.lottery.storage.repositories

import me.zodiia.lottery.storage.entities.DrawEntity
import me.zodiia.lottery.storage.entities.DrawsTable
import org.jetbrains.exposed.sql.SizedIterable

object DrawsRepository: AbstractRepository<Long, DrawEntity>(DrawEntity.Companion) {
    fun findLast(lottery: String, limit: Int = 1): SizedIterable<DrawEntity> {
        return findLimit(limit) {
            (DrawsTable.lotteryName eq lottery)
        }
    }
}
