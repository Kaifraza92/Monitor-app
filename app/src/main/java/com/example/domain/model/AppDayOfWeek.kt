package com.example.domain.model

enum class AppDayOfWeek(val displayName: String, val shortName: String, val calendarDay: Int) {
    MONDAY("Monday", "Mon", java.util.Calendar.MONDAY),
    TUESDAY("Tuesday", "Tue", java.util.Calendar.TUESDAY),
    WEDNESDAY("Wednesday", "Wed", java.util.Calendar.WEDNESDAY),
    THURSDAY("Thursday", "Thu", java.util.Calendar.THURSDAY),
    FRIDAY("Friday", "Fri", java.util.Calendar.FRIDAY),
    SATURDAY("Saturday", "Sat", java.util.Calendar.SATURDAY),
    SUNDAY("Sunday", "Sun", java.util.Calendar.SUNDAY);

    companion object {
        fun fromCalendar(calendarDay: Int): AppDayOfWeek {
            return entries.firstOrNull { it.calendarDay == calendarDay } ?: MONDAY
        }

        fun parseDays(commaSeparated: String): Set<AppDayOfWeek> {
            if (commaSeparated.isBlank()) return emptySet()
            return commaSeparated.split(",")
                .mapNotNull { name ->
                    entries.firstOrNull { it.name.equals(name.trim(), ignoreCase = true) }
                }
                .toSet()
        }

        fun formatDays(days: Set<AppDayOfWeek>): String {
            return entries.filter { days.contains(it) }.joinToString(",") { it.name }
        }
    }
}
