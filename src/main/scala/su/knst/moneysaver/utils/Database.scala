package su.knst.moneysaver
package utils

import com.google.inject.{Inject, Singleton}
import org.jooq.{DSLContext, SQLDialect}
import org.jooq.impl.DSL
import su.knst.moneysaver.utils.config.MainConfig

import java.sql.{Connection, DriverManager, Statement}

@Singleton
class Database @Inject()(
  config: MainConfig
) {
  val connection: Connection = DriverManager.getConnection(
    config.database.saveStringIfNull("url", "jdbc:postgresql://localhost/db"),
    config.database.saveStringIfNull("user", "money_saver"),
    config.database.saveStringIfNull("password", "change_me"),
  )

  val statement: Statement = connection.createStatement()
  val context: DSLContext = DSL.using(connection, SQLDialect.POSTGRES)
}
