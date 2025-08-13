package com.example.artiumlessons.data

import com.example.artiumlessons.network.ApiService
import javax.inject.Inject

class Repository @Inject constructor(private val api: ApiService){

    suspend fun fetchLessons(): LessonsResponse = api.getLessons()
}