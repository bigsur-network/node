package db

import org.apache.commons.dbcp2.BasicDataSource

object PgDataSource {
  def apply(config: Config): BasicDataSource = {
    val dataSource = new BasicDataSource()

    dataSource.setDriverClassName("org.postgresql.Driver")
    dataSource.setUrl(config.url)
    dataSource.setUsername(config.user)
    dataSource.setPassword(config.password)
    dataSource.setInitialSize(config.initialConnections)
    dataSource.setMaxIdle(config.maxIdleConnections)
    dataSource.setMaxTotal(config.maxTotalConnections)

    dataSource
  }
}
