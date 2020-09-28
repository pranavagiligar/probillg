package com.probill.utility

import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.util.*

object DateTimeUtils {
    private val dateFormatter = SimpleDateFormat( "dd/MMM/yy hh:mm:ss a" )
    fun time(timestamp: Timestamp): String = dateFormatter.format(
        Date().apply {
            time = timestamp.time
        }
    )
}