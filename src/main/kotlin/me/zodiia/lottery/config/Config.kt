package me.zodiia.lottery.config

data class Config(
    val database: Database,
    val lotteries: Array<Lottery>,
) {
    data class Database(
        val storageType: String,
        val host: String,
        val database: String,
        val username: String,
        val password: String,
        val poolSize: Int,
        val keepaliveTime: Long,
        val maximumLifetime: Long,
        val tablePrefix: String,
    )

    data class Lottery(
        val schedule: Schedule,
        val displayName: String,
        val ticketValue: Double,
        val maxTicketsPerPlayer: Double,
        val tax: Double,
        val extraRewards: Array<String>,
    ) {
        data class Schedule(
            val time: String,
            val day: String,
            val reminders: String,
        )
    }
}
