package me.zodiia.lottery.storage.repositories

import me.zodiia.api.data.AbstractRepository
import me.zodiia.lottery.storage.LotteryDataSource
import me.zodiia.lottery.storage.entities.TicketEntity
import me.zodiia.lottery.storage.entities.TicketsTable
import org.jetbrains.exposed.sql.SizedIterable
import org.jetbrains.exposed.sql.and
import reactor.core.publisher.Flux
import java.util.UUID

object TicketsRepository: AbstractRepository<Long, TicketEntity>(TicketEntity.Companion, LotteryDataSource.db) {
    suspend fun findAllAfter(time: Long, lottery: String): Flux<TicketEntity> {
        return find {
            (TicketsTable.time greater time) and
                (TicketsTable.lotteryName eq lottery)
        }
    }

    suspend fun findAllForAfter(player: UUID, time: Long, lottery: String): Flux<TicketEntity> {
        return find {
            (TicketsTable.time greater time) and
                (TicketsTable.lotteryName eq lottery) and
                (TicketsTable.player eq player)
        }
    }
}
