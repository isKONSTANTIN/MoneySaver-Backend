package su.knst.moneysaver.objects

import com.google.inject.Inject
import su.knst.moneysaver.http.routers.admin.AdminDatabase

import java.util.UUID

class AuthedUser(val id: Int, val email: String, val token: UUID, val receiptToken: String, val isAdmin: Boolean)

object AuthedUser{
  def apply(user: User, token: UUID, isAdmin: Boolean): AuthedUser = new AuthedUser(user.id, user.email, token, user.receiptToken, isAdmin)
}