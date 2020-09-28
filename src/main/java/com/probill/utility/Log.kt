package com.probill.utility

import java.text.SimpleDateFormat
import java.util.*

object Log {
    private val printStream = System.out
    private val logDateFormatter = SimpleDateFormat( "dd/MMM/yy HH:mm:ss Z" )
    private fun time() = logDateFormatter.format(
            Date().apply {
                time = System.currentTimeMillis()
            }
    )

    fun e(tag: String, log: String) {
        printStream.println("[ ${time()} ]\t ERROR [ ${tag} ]\t[ ${log} ]")
    }

    fun i(tag: String, log: String) {
        printStream.println("[ ${time()} ]\t INFO [ ${tag} ]\t[ ${log} ]")
    }

    fun d(tag: String, log: String) {
        printStream.println("[ ${time()} ]\t DEBUG [ ${tag} ]\t[ ${log} ]")
    }
}