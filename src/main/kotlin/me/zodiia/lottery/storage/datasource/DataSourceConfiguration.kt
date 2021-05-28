package me.zodiia.lottery.storage.datasource

import org.bukkit.configuration.ConfigurationSection

data class DataSourceConfiguration(
    val storageType: DataSourceType,
    val host: String,
    val database: String,
    val username: String,
    val password: String,
    val poolSize: Int,
    val keepAliveTime: Int,
    val connectionTimeout: Long,
    val maxLifetime: Long,
    val tablePrefix: String,
) {
    companion object {
        fun fromConfig(config: ConfigurationSection) = DataSourceConfiguration(
            DataSourceType.valueOf(config.getString("storage-type").orEmpty().toUpperCase()),
            config.getString("host").orEmpty(),
            config.getString("database").orEmpty(),
            config.getString("username").orEmpty(),
            config.getString("password").orEmpty(),
            config.getInt("pool-size"),
            config.getInt("keepalive-time"),
            config.getLong("connection-timeout"),
            config.getLong("max-lifetime"),
            config.getString("table-prefix").orEmpty(),
        )
    }
}
