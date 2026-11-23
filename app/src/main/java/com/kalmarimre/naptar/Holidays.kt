package com.kalmarimre.naptar

import java.time.LocalDate

object Holidays {

    /** Magyar munkaszüneti napok és ünnepek egy adott évre: dátum -> név */
    fun forYear(y: Int): Map<LocalDate, String> {
        val map = mutableMapOf<LocalDate, String>()
        map[LocalDate.of(y, 1, 1)] = "Újév"
        map[LocalDate.of(y, 3, 15)] = "Nemzeti ünnep"
        map[LocalDate.of(y, 5, 1)] = "Munkaszüneti nap"
        map[LocalDate.of(y, 8, 20)] = "Állami ünnep"
        map[LocalDate.of(y, 10, 23)] = "Nemzeti ünnep"
        map[LocalDate.of(y, 11, 1)] = "Mindenszentek"
        map[LocalDate.of(y, 12, 24)] = "Karácsony"
        map[LocalDate.of(y, 12, 25)] = "Karácsony"
        map[LocalDate.of(y, 12, 26)] = "Karácsony"

        val easter = easterSunday(y)
        map[easter] = "Húsvét"
        map[easter.plusDays(1)] = "Húsvét"
        map[easter.plusDays(49)] = "Pünkösd"
        map[easter.plusDays(50)] = "Pünkösd"
        return map
    }

    /** Húsvétvasárnap (Anonymous Gregorian algoritmus) */
    fun easterSunday(y: Int): LocalDate {
        val a = y % 19
        val b = y / 100
        val c = y % 100
        val d = b / 4
        val e = b % 4
        val f = (b + 8) / 25
        val g = (b - f + 1) / 3
        val h = (19 * a + b - d - g + 15) % 30
        val i = c / 4
        val k = c % 4
        val l = (32 + 2 * e + 2 * i - h - k) % 7
        val m = (a + 11 * h + 22 * l) / 451
        val month = (h + l - 7 * m + 114) / 31
        val day = ((h + l - 7 * m + 114) % 31) + 1
        return LocalDate.of(y, month, day)
    }
}
