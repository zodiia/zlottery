package me.zodiia.lottery.storage.datasource

enum class DataSourceType(
    val requireAuth: Boolean = false
) {
    MYSQL(true),
    MARIADB(true),
    POSTGRESQL(true),
    SQLSERVER(true),
    H2,
    SQLITE,
}
