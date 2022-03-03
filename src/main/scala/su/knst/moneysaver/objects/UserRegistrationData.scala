package su.knst.moneysaver.objects

import java.time.LocalDateTime

class UserRegistrationData(val id: Int, val user: Int, val registrationTime: LocalDateTime, val expiresIn: LocalDateTime, val demoAccount: Boolean)

object UserRegistrationData {
  def noLimit(user: Int): UserRegistrationData = new UserRegistrationData(0, user, LocalDateTime.MIN, LocalDateTime.MAX, false)
}