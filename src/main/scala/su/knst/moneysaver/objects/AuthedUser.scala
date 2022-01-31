package su.knst.moneysaver.objects

import java.util.UUID

class AuthedUser(val id: Int, val email: String, val token: UUID, val receiptToken: String)

object AuthedUser{
  def apply(user: User, token: UUID): AuthedUser = new AuthedUser(user.id, user.email, token, user.receiptToken)
}