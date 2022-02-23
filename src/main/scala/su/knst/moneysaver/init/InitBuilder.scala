package su.knst.moneysaver.init

import com.google.inject.{Inject, Singleton}
import su.knst.moneysaver.http.routers.user.UsersDatabase
import su.knst.moneysaver.utils.config.InitConfig

import java.util.UUID

@Singleton
class InitBuilder @Inject()(config: InitConfig, users: UsersDatabase){
  def checkAll() : Unit = {
    admin()
  }

  def admin() : Unit = {
    val adminPassword = config.admin.loadString("init_password")

    if (adminPassword == null){
      val newPassword = UUID.randomUUID().toString
      users.registerUser("admin", newPassword)
      config.admin.saveString("init_password", newPassword)
    }
  }
}
