package su.knst.moneysaver
package objects

import java.time.{Instant, LocalDateTime}

class Transaction(val id: Int, val user: Int, val delta: Double, val tag: Int, val date: Instant, val account: Int, val description: String)
