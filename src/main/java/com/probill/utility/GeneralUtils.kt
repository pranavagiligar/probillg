package com.probill.utility

import java.lang.String.*
import java.math.BigDecimal
import java.util.*
import kotlin.math.floor


object GeneralUtils {
    fun isNullOrEmpty(str: String?) = str == null || str.trim().isEmpty()

    fun convertToIndianCurrency(num: String): String {
        val bd = BigDecimal(num)
        var number = bd.toLong()
        var no = bd.toLong()
        val decimal = (bd.remainder(BigDecimal.ONE).toDouble() * 100).toInt()
        val digitsLength = no.toString().length
        var i = 0
        val str = ArrayList<String?>()
        val words = HashMap<Int, String>()
        words[0] = ""
        words[1] = "One"
        words[2] = "Two"
        words[3] = "Three"
        words[4] = "Four"
        words[5] = "Five"
        words[6] = "Six"
        words[7] = "Seven"
        words[8] = "Eight"
        words[9] = "Nine"
        words[10] = "Ten"
        words[11] = "Eleven"
        words[12] = "Twelve"
        words[13] = "Thirteen"
        words[14] = "Fourteen"
        words[15] = "Fifteen"
        words[16] = "Sixteen"
        words[17] = "Seventeen"
        words[18] = "Eighteen"
        words[19] = "Nineteen"
        words[20] = "Twenty"
        words[30] = "Thirty"
        words[40] = "Forty"
        words[50] = "Fifty"
        words[60] = "Sixty"
        words[70] = "Seventy"
        words[80] = "Eighty"
        words[90] = "Ninety"
        val digits = arrayOf("", "Hundred", "Thousand", "Lakh", "Crore")
        while (i < digitsLength) {
            val divider = if (i == 2) 10 else 100
            number = no % divider
            no /= divider
            i += if (divider == 10) 1 else 2
            if (number > 0) {
                val counter = str.size
                val plural = if (counter > 0 && number > 9) "s" else ""
                val tmp = if (number < 21) {
                    words[Integer.valueOf(number.toInt())].toString() + " " + digits[counter] + plural
                } else {
                    words[Integer.valueOf(floor(number / 10.toDouble()).toInt() * 10)].toString() + " " + words[Integer.valueOf((number % 10).toInt())] + " " + digits[counter] + plural
                }
                str.add(tmp)
            } else {
                str.add("")
            }
        }
        str.reverse()
        val rupees = join(" ", str).trim { it <= ' ' }
        val paise =
            if (decimal > 0) " And Paise " + words[Integer.valueOf((decimal - decimal % 10))] + " " + words[Integer.valueOf((decimal % 10))]
            else ""

        return "Rupees $rupees$paise Only"
    }
}