package su.knst.moneysaver.objects

import java.time.Instant
import java.util.UUID

class UserSession(val id: Int, val user: Int, val session: UUID, val expiredAt: Instant)
