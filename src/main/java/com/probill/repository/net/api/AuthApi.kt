package com.probill.repository.net.api

import com.probill.repository.net.res.Login
import com.probill.repository.net.res.Validate
import retrofit2.http.*

interface AuthApi {
    @GET("/login")
    suspend fun login(
        @Query("username") username: String,
        @Query("password") password: String
    ): Login

    @GET("/validate")
    suspend fun validate(@Query("username") username: String): Validate

    @PUT("/resetPassword")
    suspend fun resetPassword(): Validate
}