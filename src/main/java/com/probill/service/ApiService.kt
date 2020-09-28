package com.probill.service

import com.google.gson.Gson
import com.probill.repository.net.midleware.AuthInterceptor
import com.probill.utility.JsonUtils
import com.probill.utility.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiService {

    private val loggingInterceptor = HttpLoggingInterceptor(
        HttpLoggingInterceptor.Logger { message ->
            Log.d("API_SERVICE", message)
        }
    )

    private val okHttpClient = OkHttpClient().newBuilder()
        .addInterceptor(AuthInterceptor())
//        .addInterceptor(loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY))
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl(com.probill.Constant.BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .client(okHttpClient)
        .build()

    /**
     *  The method act as factory method to create retrofit API interface
     *  @param T: Generic type of retrofit API interface
     *  @param clazz: Generic type of retrofit API interface
     *  @return T of type retrofit API object
     */
    fun <T> getApiService(clazz: Class<T>): T = retrofit.create(clazz)

    /**
     *  Runs given function in specified co-routine scope using default dispatcher
     *  @param T: Error body content type, if error type is unknown, pass [Any]
     *  @param scope: The [CoroutineScope] for executing the job
     *  @param requestFunc: The function that has request logic
     *  @param errorFunc:  The function that has error handling logic
     */
    fun <T> request(
        scope: CoroutineScope,
        requestFunc: suspend () -> Unit,
        errorFunc: (err: Error) -> Unit = {}
    ) {
        scope.launch(Dispatchers.Default) {
            try {
                requestFunc()
            } catch (e: Exception) {
                e.printStackTrace()
                getError<T>(e).apply {
                    errorFunc(this)
                }
            }
        }
    }

    /**
     *  Handles error and pass this error in a formatted manner.
     *  Also implements default action for error codes.
     *  @param T: Error body content type, if error type is unknown, pass [Any]
     *  @param e: The exception which might has [HttpException]
     *  @return [Error]:
     */
    private fun <T> getError(e: Exception): Error {
        return if (e is HttpException) {
            val data: Any? = if (e.response()?.errorBody() != null) {
                try {
                    Gson().fromJson<T>(e.response()?.errorBody()?.string(), JsonUtils.tyto<T>())
                } catch (jsonException: Exception) {
                    jsonException.printStackTrace()
                }
            } else {
                null
            }
            when (e.code()) {
                // TODO: Put default behaviour for 401
                400 -> Error(401, "Bad Request", data)
                401 -> {
                    // TODO: Logout or silent report
                    Error(401, "Unauthorized", data)
                }
                403 -> Error(403, "Forbidden", data)
                404 -> Error(403, "Not found", data)
                405 -> Error(403, "Method Not Allowed", data)
                else -> Error(400, "Something went wrong: ${e.message}", data)
            }
        } else {
            Error(400, "Something went wrong: ${e.message}", null)
        }
    }

    data class Error(val code: Int, val message: String, val data: Any?)
}