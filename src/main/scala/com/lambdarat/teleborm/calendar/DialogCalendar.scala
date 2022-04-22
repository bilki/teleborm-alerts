package com.lambdarat.teleborm.calendar

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month

import scala.annotation.tailrec
import scala.jdk.StreamConverters._

import cats.syntax.all._
import com.bot4s.telegram.Implicits._
import com.bot4s.telegram.models.InlineKeyboardButton
import com.bot4s.telegram.models.InlineKeyboardMarkup

object DialogCalendar {

  val tag = "dcalendar"

  case class CalendarCallbackData(
      action: CalendarAction,
      year: Option[Int] = none[Int],
      month: Option[Int] = none[Int],
      day: Option[Int] = none[Int]
  ) {
    lazy val serialized = {
      val strYear  = year.fold("_")(_.toString)
      val strMonth = month.fold("_")(_.toString)
      val strDay   = day.fold("_")(_.toString)

      s"$tag:${action.entryName}:$strYear:$strMonth:$strDay"
    }
  }

  private val emptyButton =
    InlineKeyboardButton(" ", CalendarCallbackData(CalendarAction.Ignore).serialized)

  def calendarCallbackDataFrom(rawCb: String): Option[CalendarCallbackData] =
    for {
      parts <- rawCb.split(":").some
      if (parts.size == 5)
      cbData <- parts.toList match {
        case _ :: rawAction :: rawYear :: rawMonth :: rawDay :: Nil =>
          CalendarAction
            .withNameOption(rawAction)
            .map { action =>
              CalendarCallbackData(
                action,
                rawYear.toIntOption,
                rawMonth.toIntOption,
                rawDay.toIntOption
              )
            }
        case _ => none[CalendarCallbackData]
      }
    } yield cbData

  def getYearMarkup(startingYear: Int): InlineKeyboardMarkup = {
    val yearsButtons = (startingYear - 2 to startingYear + 3).toList
      .map { year =>
        val yearCallbackData = CalendarCallbackData(CalendarAction.SetYear, year)
        InlineKeyboardButton(year.toString, yearCallbackData.serialized)
      }

    val backYearsCallbackData = CalendarCallbackData(CalendarAction.PrevYears, startingYear)
    val nextYearsCallbackData = CalendarCallbackData(CalendarAction.NextYears, startingYear)

    val navButtons = Seq(
      InlineKeyboardButton("<<", backYearsCallbackData.serialized),
      InlineKeyboardButton(">>", nextYearsCallbackData.serialized)
    )

    InlineKeyboardMarkup(
      Seq(
        yearsButtons,
        navButtons
      )
    )
  }

  private val months = List(
    "Ene",
    "Feb",
    "Mar",
    "Abr",
    "May",
    "Jun",
    "Jul",
    "Ago",
    "Sep",
    "Oct",
    "Nov",
    "Dic"
  )

  def getMonthMarkup(withYear: Int): InlineKeyboardMarkup = {
    val toStartYear =
      InlineKeyboardButton(
        withYear.toString,
        CalendarCallbackData(CalendarAction.Start, withYear.some).serialized
      )

    val yearRow = Seq(
      emptyButton,
      toStartYear,
      emptyButton
    )

    def buttonsForMonths(monthsIdx: Seq[(String, Int)]) =
      monthsIdx
        .map { case (month, idx) =>
          val monthCallbackData = CalendarCallbackData(CalendarAction.SetMonth, withYear, idx + 1)
          InlineKeyboardButton(month, monthCallbackData.serialized)
        }

    val firstHalfYear  = buttonsForMonths(months.zipWithIndex.take(6))
    val secondHalfYear = buttonsForMonths(months.zipWithIndex.drop(6))

    InlineKeyboardMarkup(
      Seq(
        yearRow,
        firstHalfYear,
        secondHalfYear
      )
    )
  }

  private val weekDays = List("Lun", "Mar", "Mie", "Jue", "Vie", "Sáb", "Dom")

  def getDayMarkup(withYear: Int, withMonth: Int): InlineKeyboardMarkup = {
    val yearButton =
      InlineKeyboardButton(
        withYear.toString,
        CalendarCallbackData(CalendarAction.Start, withYear.some).serialized
      )

    val monthButton =
      InlineKeyboardButton(
        months(withMonth - 1),
        CalendarCallbackData(CalendarAction.SetYear, withYear.some).serialized
      )

    val yearMonthRow = Seq(
      yearButton,
      monthButton
    )

    val dayNamesRow =
      weekDays.map(day =>
        InlineKeyboardButton(day, CalendarCallbackData(CalendarAction.Ignore).serialized)
      )

    val startOfMonthYear = LocalDate.of(withYear, Month.of(withMonth), 1)

    val daysOfMonth = startOfMonthYear.datesUntil(startOfMonthYear.plusMonths(1)).toScala(List)

    val weeksDayOfMonth = List.fill(6)(DayOfWeek.values.toList).flatten

    @tailrec
    def getMonthDaysButtons(
        weekDaysOfMonth: List[DayOfWeek],
        daysOfMonth: List[LocalDate],
        accWeek: List[InlineKeyboardButton],
        accMonth: List[List[InlineKeyboardButton]]
    ): List[List[InlineKeyboardButton]] = weekDaysOfMonth match {
      case Nil => accMonth :+ accWeek
      case dayOfWeek :: moreDaysOfWeek =>
        daysOfMonth match {
          case Nil =>
            if (dayOfWeek == DayOfWeek.SUNDAY)
              getMonthDaysButtons(
                moreDaysOfWeek,
                List.empty,
                List.empty,
                accMonth :+ (accWeek :+ emptyButton)
              )
            else
              getMonthDaysButtons(
                moreDaysOfWeek,
                List.empty,
                accWeek :+ emptyButton,
                accMonth
              )
          case nextDayOfMonth :: moreDaysOfMonth if (nextDayOfMonth.getDayOfWeek == dayOfWeek) =>
            val dayOfMonthButton = InlineKeyboardButton(
              nextDayOfMonth.getDayOfMonth.toString,
              CalendarCallbackData(
                CalendarAction.SetDay,
                withYear,
                withMonth,
                nextDayOfMonth.getDayOfMonth
              ).serialized
            )
            if (dayOfWeek == DayOfWeek.SUNDAY)
              getMonthDaysButtons(
                moreDaysOfWeek,
                moreDaysOfMonth,
                List.empty,
                accMonth :+ (accWeek :+ dayOfMonthButton)
              )
            else
              getMonthDaysButtons(
                moreDaysOfWeek,
                moreDaysOfMonth,
                accWeek :+ dayOfMonthButton,
                accMonth
              )
          case _ =>
            if (dayOfWeek == DayOfWeek.SUNDAY)
              getMonthDaysButtons(
                moreDaysOfWeek,
                daysOfMonth,
                List.empty,
                accMonth :+ (accWeek :+ emptyButton)
              )
            else
              getMonthDaysButtons(moreDaysOfWeek, daysOfMonth, accWeek :+ emptyButton, accMonth)
        }
    }

    val monthDaysButtons = getMonthDaysButtons(weeksDayOfMonth, daysOfMonth, List.empty, List.empty)

    InlineKeyboardMarkup(
      Seq(
        yearMonthRow,
        dayNamesRow
      ) ++ monthDaysButtons
    )
  }

}
