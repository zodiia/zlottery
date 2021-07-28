package me.zodiia.lottery.storage.repositories

import me.zodiia.api.data.AbstractRepository
import me.zodiia.lottery.storage.LotteryDataSource
import me.zodiia.lottery.storage.entities.TicketEntity
import me.zodiia.lottery.storage.entities.TicketsTable
import org.jetbrains.exposed.sql.SizedIterable
import org.jetbrains.exposed.sql.and
import java.util.UUID

object TicketsRepository: AbstractRepository<Long, TicketEntity>(TicketEntity.Companion, LotteryDataSource.db) {
    fun findAllAfter(time: Long, lottery: String): SizedIterable<TicketEntity> {
        return find {
            (TicketsTable.time greater time) and
                (TicketsTable.lotteryName eq lottery)
        }
    }

    fun findAllForAfter(player: UUID, time: Long, lottery: String): SizedIterable<TicketEntity> {
        return find {
            (TicketsTable.time greater time) and
                (TicketsTable.lotteryName eq lottery) and
                (TicketsTable.player eq player)
        }
    }
}
