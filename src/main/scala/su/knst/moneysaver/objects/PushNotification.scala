package su.knst.moneysaver.objects

class PushNotification(var title: String, var body: String, var icon: String, var badge: String, var url: String)

object PushNotification {
  def createDefault(title: String, text: String)(implicit domain: String) : PushNotification = {
    new PushNotification(
      title,
      text,
      s"${domain}icon.png",
      s"${domain}icon.png",
      s"$domain"
    )
  }
}
