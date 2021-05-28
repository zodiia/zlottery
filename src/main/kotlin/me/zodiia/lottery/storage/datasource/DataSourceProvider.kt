package me.zodiia.lottery.storage.datasource

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import me.zodiia.lottery.LotteryConfig
import me.zodiia.lottery.LotteryPlugin
import org.jetbrains.exposed.sql.Database
import java.io.File
import javax.sql.DataSource

object DataSourceProvider {
    private val hikariConfig: HikariConfig = HikariConfig()
    private val dataSource: HikariDataSource
    val config: DataSourceConfiguration

    init {
        config = DataSourceConfiguration.fromConfig(LotteryConfig.databaseConfig)
        hikariConfig.jdbcUrl = getUrl()
        if (config.storageType.requireAuth) {
            hikariConfig.username = config.username
            hikariConfig.password = config.password
        }
        hikariConfig.driverClassName = getDriverClass()
        hikariConfig.isAutoCommit = true
        hikariConfig.connectionTimeout = config.connectionTimeout
        hikariConfig.maxLifetime = config.maxLifetime
        hikariConfig.maximumPoolSize = config.poolSize
        hikariConfig.poolName = "zlottery"
        addAdditionalParameters()
        dataSource = HikariDataSource(hikariConfig)
        Database.connect(dataSource)
    }

    fun getUrl(): String = when (config.storageType) {
        DataSourceType.MYSQL -> "jdbc:mysql://${config.host}/${config.database}"
        DataSourceType.MARIADB -> "jdbc:mariadb://${config.host}/${config.database}"
        DataSourceType.POSTGRESQL -> "jdbc:postgresql://${config.host}/${config.database}"
        DataSourceType.SQLSERVER -> "jdbc:sqlserver://${config.host};instance=SQLEXPRESS;databaseName=${config.database}"
        DataSourceType.H2 -> "jdbc:h2:file:${File(LotteryPlugin.plugin.dataFolder, "database").absolutePath}"
        DataSourceType.SQLITE -> "jdbc:sqlite:${File(LotteryPlugin.plugin.dataFolder, "database").absolutePath}"
    }

    fun getDriverClass(): String = when (config.storageType) {
        DataSourceType.MYSQL -> "com.mysql.jdbc.Driver"
        DataSourceType.MARIADB -> "org.mariadb.jdbc.Driver"
        DataSourceType.POSTGRESQL -> "org.postgresql.Driver"
        DataSourceType.SQLSERVER -> "com.microsoft.sqlserver.jdbc.SQLServerDriver"
        DataSourceType.H2 -> "org.h2.Driver"
        DataSourceType.SQLITE -> "org.sqlite.JDBC"
    }

    fun addAdditionalParameters() = when (config.storageType) {
        DataSourceType.MYSQL, DataSourceType.MARIADB -> {
            hikariConfig.addDataSourceProperty("cachePrepStmts", "true")
            hikariConfig.addDataSourceProperty("prepStmtCacheSize", "400")
            hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
            hikariConfig.addDataSourceProperty("useServerPrepStmts", "true")
        }
        else -> Unit // TODO: Find optimization parameters for other databases
    }

    fun getSource(): DataSource = dataSource

    fun close() {
        dataSource.close()
    }
}
