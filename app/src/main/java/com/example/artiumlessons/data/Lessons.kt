package com.example.artiumlessons.data


data class Lesson(
    val mentor_name: String,
    val lesson_title: String,
    val video_thumbnail_url: String,
    val lesson_image_url: String,
    val video_url: String
)

data class LessonsResponse(val lessons: List<Lesson>)


data class PracticeSubmission(
    val lesson: Lesson,
    val notes: String,
    val timestamp: Long = System.currentTimeMillis()
)
