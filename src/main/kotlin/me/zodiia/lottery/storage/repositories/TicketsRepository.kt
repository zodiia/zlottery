package me.zodiia.lottery.storage.repositories

import me.zodiia.lottery.storage.entities.TicketEntity
import me.zodiia.lottery.storage.entities.TicketsTable
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SizedIterable
import org.jetbrains.exposed.sql.SqlExpressionBuilder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.emptySized
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.UUID

object TicketsRepository {
    fun findOne(id: Long): TicketEntity? {
        var result: TicketEntity? = null

        transaction {
            result = TicketEntity.findById(id)
        }
        return result
    }

    fun find(op: SqlExpressionBuilder.() -> Op<Boolean>): SizedIterable<TicketEntity> {
        var results: SizedIterable<TicketEntity> = emptySized()

        transaction {
            results = TicketEntity.find(op)
        }
        return results
    }

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
