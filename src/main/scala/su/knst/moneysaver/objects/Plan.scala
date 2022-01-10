package su.knst.moneysaver
package objects

import java.time.{Instant, LocalDateTime}

class Plan(val id: Int, val user: Int, val delta: Double, val tag: Int, val date: Instant, val account: Int, val description: String, val state: Int)
