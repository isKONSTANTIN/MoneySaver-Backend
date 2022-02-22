package su.knst.moneysaver
package utils

import com.google.inject.{Inject, Singleton}
import org.flywaydb.core.Flyway
import org.flywaydb.core.internal.database.DatabaseType
import org.flywaydb.core.internal.plugin.PluginRegister
import org.jooq.{DSLContext, SQLDialect}
import org.jooq.impl.DSL
import su.knst.moneysaver.utils.config.MainConfig

import java.sql.{Connection, DriverManager, Statement}
import java.util.stream.Collectors

@Singleton
class Database @Inject()(
  config: MainConfig
) {
  var url: String = config.database.saveStringIfNull("url", "jdbc:postgresql://database/money_saver")
  var user: String = config.database.saveStringIfNull("user", "money_saver")
  var password: String = config.database.saveStringIfNull("password", System.getenv("POSTGRES_PASSWORD"))

  val flyway: Flyway = Flyway.configure
    .dataSource(url, user, password)
    .baselineVersion("1.0.0").load
  flyway.migrate()

  val connection: Connection = DriverManager.getConnection(url, user, password)

  val statement: Statement = connection.createStatement()
  val context: DSLContext = DSL.using(connection, SQLDialect.POSTGRES)
}
