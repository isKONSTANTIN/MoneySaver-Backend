package su.knst.moneysaver.objects

import java.time.{Instant, LocalDateTime}

class UserRegistrationData(val id: Int, val user: Int, val registrationTime: Instant, val expiresIn: Instant, val demoAccount: Boolean)

object UserRegistrationData {
  def noLimit(user: Int): UserRegistrationData = new UserRegistrationData(0, user, Instant.MIN, Instant.MAX, false)
}