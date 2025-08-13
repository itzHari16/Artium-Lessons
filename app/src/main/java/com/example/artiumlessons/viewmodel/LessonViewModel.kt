package com.example.artiumlessons.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.artiumlessons.data.Lesson
import com.example.artiumlessons.data.PracticeSubmission
import com.example.artiumlessons.data.Repository
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject


@HiltViewModel
class LessonViewModel @Inject constructor(
    private val repository: Repository,
    val exoPlayer: ExoPlayer
) : ViewModel() {
    private val _uiState = MutableStateFlow<LessonUiState>(LessonUiState.Loading)
    val uiState: StateFlow<LessonUiState> = _uiState.asStateFlow()

    private val _uploadState = MutableStateFlow<UploadState>(UploadState.Idle)
    val uploadState: StateFlow<UploadState> = _uploadState.asStateFlow()

    private val _submittedPractices = MutableStateFlow<List<PracticeSubmission>>(emptyList())
    val submittedPractices: StateFlow<List<PracticeSubmission>> = _submittedPractices.asStateFlow()

    init {
        fetchLessons()
    }

    private fun fetchLessons() {
        viewModelScope.launch {
            _uiState.value = LessonUiState.Loading
            try {
                val lessons = repository.fetchLessons().lessons
                _uiState.value = LessonUiState.Success(lessons)
            } catch (e: Exception) {
                // Properly handle error state
                _uiState.value =
                    LessonUiState.Error("Failed to load lessons. Please check your connection.")
            }
        }
    }

    fun findLessonByTitle(title: String): Lesson? {
        return if (_uiState.value is LessonUiState.Success) {
            (_uiState.value as LessonUiState.Success).lessons.firstOrNull { it.lesson_title == title }
        } else {
            null
        }
    }

    fun preparePlayer(videoUrl: String) {
        exoPlayer.setMediaItem(MediaItem.fromUri(videoUrl))
        exoPlayer.prepare()
        exoPlayer.playWhenReady = true
    }

    fun simulateUpload(lesson: Lesson, notes: String) {
        viewModelScope.launch {
            _uploadState.value = UploadState.Uploading(0)
            for (i in 1..100 step 5) {
                delay(100)
                _uploadState.value = UploadState.Uploading(i)
            }
            val success = kotlin.random.Random.nextBoolean()

            if (success) {
                _uploadState.value = UploadState.Success
                val submission = PracticeSubmission(lesson = lesson, notes = notes)
                _submittedPractices.update { currentList -> currentList + submission }
            } else {
                _uploadState.value = UploadState.Failure
            }
        }
    }

    fun resetUploadState() {
        _uploadState.value = UploadState.Idle
    }


    override fun onCleared() {
        super.onCleared()
        exoPlayer.release()
    }
}

sealed interface LessonUiState {
    object Loading : LessonUiState
    data class Success(val lessons: List<Lesson>) : LessonUiState
    data class Error(val message: String) : LessonUiState
}


sealed class UploadState {
    object Idle : UploadState()
    data class Uploading(val progress: Int) : UploadState()
    object Success : UploadState()
    object Failure : UploadState()
}