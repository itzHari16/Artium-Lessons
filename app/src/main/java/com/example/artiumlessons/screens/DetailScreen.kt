package com.example.artiumlessons.screens

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import coil.compose.AsyncImage
import com.example.artiumlessons.data.Lesson
import com.example.artiumlessons.viewmodel.LessonViewModel
import com.example.artiumlessons.viewmodel.UploadState
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.ui.StyledPlayerView

private val HeaderHeight = 220.dp

@Composable
fun DetailScreen(
    lessonTitle: String,
    viewModel: LessonViewModel,
    onBack: () -> Unit
) {
    var isFullScreen by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val activity = context as? Activity
    val window = activity?.window

    LaunchedEffect(isFullScreen) {
        if (window != null) {
            val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
            if (isFullScreen) {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                windowInsetsController.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
            } else {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    val configuration = LocalConfiguration.current
    if (configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            VideoPlayer(exoPlayer = viewModel.exoPlayer) { isFullScreen = false }
        }
    } else {
        PortraitView(lessonTitle, viewModel, onBack) { isFullScreen = true }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortraitView(
    lessonTitle: String,
    viewModel: LessonViewModel,
    onBack: () -> Unit,
    onToggleFullScreen: () -> Unit
) {
    val lesson = remember(lessonTitle) { viewModel.findLessonByTitle(lessonTitle) }
    if (lesson == null) {
        return
    }

    var showBottomSheet by remember { mutableStateOf(false) }
    if (showBottomSheet) {
        UploadBottomSheet(
            onDismiss = {
                showBottomSheet = false
                viewModel.resetUploadState()
            },
            viewModel = viewModel,
            lessonToSubmit = lesson
        )
    }

    val lazyListState = rememberLazyListState()
    val headerHeightPx = with(LocalDensity.current) { HeaderHeight.toPx() }
    val toolbarHeightPx = with(LocalDensity.current) { 64.dp.toPx() }

    val toolbarAlpha by remember {
        derivedStateOf {
            if (lazyListState.firstVisibleItemIndex > 0) 1f
            else (lazyListState.firstVisibleItemScrollOffset / (headerHeightPx - toolbarHeightPx)).coerceIn(0f, 1f)
        }
    }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text("Submit Practice") },
                icon = { Icon(Icons.Filled.FileUpload, contentDescription = null) },
                onClick = { showBottomSheet = true }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            LazyColumn(state = lazyListState) {
                item { VideoPlayerHeader(lesson, viewModel, onToggleFullScreen) }
                item { LessonTitleSection(lesson) }
                item { LessonNotesSection() }
                item { Spacer(modifier = Modifier.height(100.dp)) }
            }

            TopAppBar(
                title = { Text(lesson.lesson_title, color = LocalContentColor.current.copy(alpha = toolbarAlpha)) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = toolbarAlpha))
            )
        }
    }
}

@Composable
private fun VideoPlayerHeader(
    lesson: Lesson,
    viewModel: LessonViewModel,
    onToggleFullScreen: () -> Unit
) {
    var isPlaying by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(HeaderHeight)
    ) {
        Crossfade(targetState = isPlaying, label = "Video-Thumbnail-Crossfade") { playerVisible ->
            if (!playerVisible) {
                Box(Modifier.fillMaxSize()) {
                    AsyncImage(
                        model = lesson.video_thumbnail_url,
                        contentDescription = "Video Thumbnail",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)))
                    IconButton(
                        onClick = {
                            viewModel.preparePlayer(lesson.video_url)
                            isPlaying = true
                        },
                        modifier = Modifier.align(Alignment.Center)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = "Play Video",
                            tint = Color.White,
                            modifier = Modifier.size(72.dp)
                        )
                    }
                }
            } else {
                VideoPlayer(exoPlayer = viewModel.exoPlayer, onToggleFullScreen = onToggleFullScreen)
            }
        }
    }
}

@Composable
private fun LessonTitleSection(lesson: Lesson) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = lesson.lesson_title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "with ${lesson.mentor_name}",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun LessonNotesSection() {
    var isExpanded by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(MaterialTheme.shapes.large)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                isExpanded = !isExpanded
            }
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Lesson Notes",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { isExpanded = !isExpanded }) {
                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse notes" else "Expand notes"
                )
            }
        }

        AnimatedContent(
            targetState = isExpanded,
            label = "Notes-Expand-Collapse-Animation",
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
        ) { expanded ->
            Text(
                text = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed non risus. Suspendisse lectus tortor, dignissim sit amet, adipiscing nec, ultricies sed, dolor. Cras elementum ultrices diam. Maecenas ligula massa, varius a, semper congue, euismod non, mi. Proin porttitor, orci nec nonummy molestie, enim est eleifend mi, non fermentum diam nisl sit amet erat. Duis semper. Duis arcu massa, scelerisque vitae, consequat in, pretium a, enim. Pellentesque congue. Ut in risus volutpat libero pharetra tempor. Cras vestibulum bibendum augue. Praesent egestas leo in pede. Praesent blandit odio eu enim.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.5,
                maxLines = if (expanded) Int.MAX_VALUE else 3,
                overflow = if (expanded) TextOverflow.Clip else TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun VideoPlayer(exoPlayer: ExoPlayer, onToggleFullScreen: () -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> exoPlayer.pause()
                Lifecycle.Event.ON_RESUME -> exoPlayer.play()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            exoPlayer.pause()
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    AndroidView(
        factory = { context ->
            StyledPlayerView(context).apply {
                player = exoPlayer
                useController = true
                setFullscreenButtonClickListener {
                    onToggleFullScreen()
                }
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadBottomSheet(
    onDismiss: () -> Unit,
    viewModel: LessonViewModel,
    lessonToSubmit: Lesson
) {
    val uploadState by viewModel.uploadState.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var notes by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Submit Your Practice", style = MaterialTheme.typography.headlineSmall)

            when (val state = uploadState) {
                is UploadState.Idle -> {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Add notes (optional)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(onClick = { viewModel.simulateUpload(lessonToSubmit, notes) }) {
                        Text("Upload File")
                    }
                }
                is UploadState.Uploading -> {
                    Text("Uploading... ${state.progress}%")
                    LinearProgressIndicator(
                        progress = { state.progress / 100f },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                is UploadState.Success -> {
                    Text("✅ Upload Successful!", color = MaterialTheme.colorScheme.primary)
                    Button(onClick = onDismiss) {
                        Text("Done")
                    }
                }
                is UploadState.Failure -> {
                    Text("❌ Upload Failed", color = MaterialTheme.colorScheme.error)
                    Button(onClick = { viewModel.simulateUpload(lessonToSubmit, notes) }) {
                        Text("Retry")
                    }
                }
            }
        }
    }
}