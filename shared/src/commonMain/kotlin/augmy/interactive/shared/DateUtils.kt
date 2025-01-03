package augmy.interactive.shared

import androidx.compose.runtime.Composable
import augmy.shared.generated.resources.Res
import augmy.shared.generated.resources.date_minutes_ago
import augmy.shared.generated.resources.date_seconds_ago
import augmy.shared.generated.resources.date_yesterday
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format.FormatStringsInDatetimeFormats
import kotlinx.datetime.format.byUnicodePattern
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource

//https://docs.oracle.com/javase/8/docs/api/java/time/format/DateTimeFormatter.html
/** Utils for working with date and time */
object DateUtils {

    /** Current system time */
    val now
        get() = Clock.System.now()

    /** Current localized system time */
    val localNow
        get() = now.toLocalDateTime(TimeZone.currentSystemDefault())

    /** Formats milliseconds as minutes and seconds */
    fun formatTime(millis: Long): String {
        val seconds = ((millis / 1000.0) % 60.0).toInt()
        val minutes = ((millis / (1000.0 * 60.0)) % 60.0).toInt()

        return "${if(minutes < 10) "0$minutes" else minutes}:${if(seconds < 10) "0$seconds" else seconds}"
    }

    /** Formats a localized time to a string */
    @OptIn(FormatStringsInDatetimeFormats::class)
    fun LocalDateTime?.formatAs(pattern: String): String {
        return if(this == null) "" else LocalDateTime.Format {
            byUnicodePattern(pattern)
        }.format(this)
    }

    /** Formats a localized time to a string in a relative matter */
    @Composable
    fun LocalDateTime?.formatAsRelative(): String {
        if(this == null) return ""

        val localNow = localNow
        val dayDifference = (localNow.dayOfYear + localNow.year * 365) - (this.dayOfYear + this.year * 365)

        return when {
            dayDifference > 365 -> this.formatAs("dd. MM. yyyy")
            dayDifference == 1 -> {
                stringResource(Res.string.date_yesterday) + " " + this.formatAs("HH:mm")
            }
            localNow.hour - this.hour < 1 && localNow.minute - this.minute < 1 && dayDifference == 0 -> {
                "${localNow.second - this.second} ${stringResource(Res.string.date_seconds_ago)}"
            }
            localNow.hour - this.hour < 1 && dayDifference == 0 -> {
                "${localNow.minute - this.minute} ${stringResource(Res.string.date_minutes_ago)}"
            }
            dayDifference == 0 -> this.formatAs("HH:MM")
            else -> this.formatAs("dd. MM.")
        }
    }
}