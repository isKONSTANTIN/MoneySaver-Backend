package su.knst.moneysaver.utils.time

import java.time.{DayOfWeek, LocalDate}
import java.time.temporal.{ChronoUnit, TemporalAdjusters, TemporalUnit}

object TaskTimes {
  val WEEKLY: TaskScheduleScheme = (ld, a) => ld.`with`(TemporalAdjusters.next(DayOfWeek.of(a))).withHour(12).withMinute(0).withSecond(0)
  val MONTHLY: TaskScheduleScheme = (ld, a) => {
    if (ld.getDayOfMonth >= a)
      ld.`with`(TemporalAdjusters.firstDayOfNextMonth()).plusDays(a - 1).withHour(12).withMinute(0).withSecond(0)
    else
      ld.plusDays(a - ld.getDayOfMonth).withHour(12).withMinute(0).withSecond(0)
  }
  val IN_DAYS: TaskScheduleScheme = (ld, a) => ld.plusDays(a).withHour(12).withMinute(0).withSecond(0)

  def get(id: Int): TaskScheduleScheme = {
    val array = Array(WEEKLY, MONTHLY, IN_DAYS)

    if (id < 0 || id >= array.length)
      MONTHLY
    else
      array(id)
  }


}
