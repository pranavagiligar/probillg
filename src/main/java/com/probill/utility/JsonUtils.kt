package com.probill.utility

import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

object JsonUtils {
    // Give type of generic type
    fun <T> tyto(): Type {
        return object : TypeToken<T>() {}.type
    }
}