package me.zodiia.lottery.storage.repositories

import me.zodiia.api.data.AbstractRepository
import me.zodiia.lottery.storage.LotteryDataSource
import me.zodiia.lottery.storage.entities.DrawEntity
import me.zodiia.lottery.storage.entities.DrawsTable
import org.jetbrains.exposed.sql.SizedIterable
import org.jetbrains.exposed.sql.SortOrder
import reactor.core.publisher.Flux

object DrawsRepository: AbstractRepository<Long, DrawEntity>(DrawEntity.Companion, LotteryDataSource.db) {
    suspend fun findLast(lottery: String, limit: Int = 1): Flux<DrawEntity> {
        return find(limit = limit, orderBy = mapOf(DrawsTable.time to SortOrder.DESC)) {
            (DrawsTable.lotteryName eq lottery)
        }
    }
}
