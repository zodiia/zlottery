package me.zodiia.lottery.storage.repositories

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.sql.Op
import org.jetbrains.exposed.sql.SizedIterable
import org.jetbrains.exposed.sql.SqlExpressionBuilder
import org.jetbrains.exposed.sql.emptySized
import org.jetbrains.exposed.sql.transactions.transaction

abstract class AbstractRepository<I: Comparable<I>, out T: Entity<I>>(
    protected val entityClass: EntityClass<I, T>,
) {
    fun findOne(id: I): T? {
        var res: T? = null

        transaction {
            res = entityClass.findById(id)
        }
        return res
    }

    fun find(op: SqlExpressionBuilder.() -> Op<Boolean>): SizedIterable<T> {
        var res: SizedIterable<T> = emptySized()

        transaction {
            res = entityClass.find(op)
        }
        return res
    }

    fun findLimit(limit: Int = 0, offset: Long = 0L, op: SqlExpressionBuilder.() -> Op<Boolean>): SizedIterable<T> {
        var res: SizedIterable<T> = emptySized()

        transaction {
            res = entityClass.find(op).limit(limit, offset)
        }
        return res
    }
}
