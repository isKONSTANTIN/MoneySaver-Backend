package su.knst.moneysaver.utils.time

import java.time.{LocalDate, LocalDateTime}

trait TaskScheduleScheme {
  def apply(date: LocalDateTime, arg: Int): LocalDateTime
}
