package com.example.artiumlessons.network

import com.example.artiumlessons.data.LessonsResponse
import retrofit2.http.GET

interface ApiService {
    @GET("b/7JF5")
    suspend fun getLessons(): LessonsResponse
}