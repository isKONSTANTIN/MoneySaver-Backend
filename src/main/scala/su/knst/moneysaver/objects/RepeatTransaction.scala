package su.knst.moneysaver
package objects

import java.time.Instant

class RepeatTransaction(val id: Int, val user: Int, val tag: Int, val delta: Double, val account: Int, val arg: Int, val lastRepeat: Instant, val nextRepeat: Instant, val repeatFunc: Int, val description: String)
