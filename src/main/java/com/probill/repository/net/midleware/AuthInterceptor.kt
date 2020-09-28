package com.probill.repository.net.midleware

import com.probill.repository.db.AppDb
import okhttp3.Interceptor
import okhttp3.Response

class AuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val meta = AppDb.metaDao.getLastMeta()
        if (meta?.isLoggedIn == false) return chain.proceed(chain.request())
        val username = meta?.user?.username
            ?: return chain.proceed(chain.request())
        AppDb.userDao.getByUsername(username)
            ?.let {
                val originalRequest = chain.request()
                val request = originalRequest.newBuilder()
                    .headers(originalRequest.headers)
                    .header(
                        "Authorization",
                        String.format("Bearer: %s", it.session)
                    )
                    .method(originalRequest.method, originalRequest.body)
                    .build()
                return chain.proceed(request)
            } ?: kotlin.run {
            return chain.proceed(chain.request())
        }
    }
}