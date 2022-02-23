package su.knst.moneysaver.http.routers.admin

import com.google.inject.Inject
import su.knst.moneysaver.jooq.tables.Users.USERS
import su.knst.moneysaver.objects.{Transaction, User}
import su.knst.moneysaver.utils.Database

import java.util

class AdminDatabase @Inject()
(
  database: Database
) {
  def getUsers(offset: Int, count: Int): util.List[User] = {
    database.context.selectFrom(USERS)
      .orderBy(USERS.ID.desc)
      .limit(offset, count)
      .fetch().map(r => r.into(classOf[User]))
  }
}
