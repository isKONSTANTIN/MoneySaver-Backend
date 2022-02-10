package su.knst.moneysaver.http.routers.pushing

import akka.actor.ActorSystem
import com.google.inject.{Inject, Singleton}
import com.wanari.webpush.{PushService, Subscription, Utils}
import su.knst.moneysaver.objects.{PushNotification, UserNotificationData}
import su.knst.moneysaver.public_.tables.UsersNotifications.USERS_NOTIFICATIONS
import su.knst.moneysaver.utils.Database
import su.knst.moneysaver.utils.G.gson
import su.knst.moneysaver.utils.config.MainConfig

import java.security.interfaces.{ECPrivateKey, ECPublicKey}
import scala.collection.convert.ImplicitConversions.`collection AsScalaIterable`

@Singleton
class WebPushingDatabase @Inject()
(
  database: Database,
  config: MainConfig,
  implicit val system: ActorSystem
) {
  private implicit val domain: String = config.server.saveStringIfNull("url", "https://ms.knst.su/")

  def addUserNotificationData(user: Int, endpoint: String, auth: String, p256dh: String): Unit = {
    database.context
      .insertInto(USERS_NOTIFICATIONS)
      .set(USERS_NOTIFICATIONS.USER_ID, Int.box(user))
      .set(USERS_NOTIFICATIONS.ENDPOINT, endpoint)
      .set(USERS_NOTIFICATIONS.AUTH, auth)
      .set(USERS_NOTIFICATIONS.P256DH, p256dh)
      .execute()
  }

  def sendNotificationToUser(user: Int, notification: PushNotification): Int = {
    val publicKey = config.webPush.saveStringIfNull("publicKey", "XXX")
    val privateKey = config.webPush.saveStringIfNull("privateKey", "XXX")

    if (publicKey.equals("XXX") || privateKey.equals("XXX"))
      return 0

    val vapidPublicKey: ECPublicKey = Utils.loadPublicKey(publicKey)
    val vapidPrivateKey: ECPrivateKey = Utils.loadPrivateKey(privateKey)

    val pushService = PushService(vapidPublicKey, vapidPrivateKey, domain)
    val jsonNotify = gson.toJson(notification)

    val subscriptions = database.context
      .selectFrom(USERS_NOTIFICATIONS)
      .where(USERS_NOTIFICATIONS.USER_ID.eq(user))
      .fetch()
      .map[UserNotificationData](_.into(classOf[UserNotificationData]))
      .map(d => Subscription(d.endpoint, d.p256dh, d.auth))

    subscriptions.foreach(pushService.send(_, jsonNotify))

    subscriptions.size
  }
}
