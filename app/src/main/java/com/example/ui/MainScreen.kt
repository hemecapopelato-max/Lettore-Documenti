package com.example.ui

import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ButtonDefaults
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Document
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val documents by viewModel.documents.collectAsState()
    val context = LocalContext.current

    // Launcher for file picking (.txt files)
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val contentResolver = context.contentResolver
                val inputStream = contentResolver.openInputStream(uri)
                var fileName = "Documento Importato"
                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1 && cursor.moveToFirst()) {
                        fileName = cursor.getString(nameIndex)
                    }
                }
                viewModel.importTextFromUri(inputStream, fileName)
            } catch (e: Exception) {
                Toast.makeText(context, "Impossibile caricare il file", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                // Draw mic/headphone-like clean accent
                                Text("🎙️", fontSize = 18.sp)
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Lettore Documenti",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Text-to-Speech Italiano & Multilingua",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { filePickerLauncher.launch("text/plain") },
                        modifier = Modifier.testTag("import_file_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share, 
                            contentDescription = "Importa file di testo (.txt)",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            if (!uiState.isPlayerExpanded) {
                FloatingActionButton(
                    onClick = { viewModel.showAddDocument(true) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("add_document_fab")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Aggiungi file o testo")
                }
            }
        }
    ) { innerPadding ->
        
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Document List
            if (documents.isEmpty()) {
                EmptyStateView(
                    onImportClick = { filePickerLauncher.launch("text/plain") },
                    onPasteClick = { viewModel.showAddDocument(true) }
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "I Tuoi Documenti (${documents.size})",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                        )
                    }

                    itemsIndexed(documents) { _, doc ->
                        DocumentItemCard(
                            document = doc,
                            isActive = uiState.activeDocument?.id == doc.id,
                            onPlayClick = { viewModel.loadDocument(doc) },
                            onDeleteClick = { viewModel.triggerDeleteDialog(doc) }
                        )
                    }
                    
                    // Add padding to avoid overlaying floating player
                    item {
                        Spacer(modifier = Modifier.height(110.dp))
                    }
                }
            }

            // Collapsed Floating Player
            AnimatedVisibility(
                visible = uiState.activeDocument != null && !uiState.isPlayerExpanded,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp, start = 12.dp, end = 12.dp)
            ) {
                CollapsedFloatingPlayer(
                    uiState = uiState,
                    onExpandClick = { viewModel.setPlayerExpanded(true) },
                    onPlayPauseClick = {
                        if (uiState.isPlaying) viewModel.pause() else viewModel.play()
                    },
                    onCloseClick = { viewModel.stopPlayback() }
                )
            }

            // Expanded Full Immersive Reader (overlay)
            AnimatedVisibility(
                visible = uiState.isPlayerExpanded && uiState.activeDocument != null,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                ImmersiveReaderView(
                    uiState = uiState,
                    onBackClick = { viewModel.setPlayerExpanded(false) },
                    onPlayPauseClick = {
                        if (uiState.isPlaying) viewModel.pause() else viewModel.play()
                    },
                    onStopClick = { viewModel.stopPlayback() },
                    onNextClick = { viewModel.next() },
                    onPreviousClick = { viewModel.previous() },
                    onSentenceClick = { index -> viewModel.jumpToSentence(index) },
                    onSpeedChange = { viewModel.setSpeed(it) },
                    onPitchChange = { viewModel.setPitch(it) },
                    onLanguageSelect = { viewModel.setLanguage(it) }
                )
            }
        }
    }

    // Add Document Dialog / Modal Bottom Sheet
    if (uiState.showAddDocumentSheet) {
        AddDocumentDialog(
            onDismiss = { viewModel.showAddDocument(false) },
            onSave = { title, content -> viewModel.saveNewDocument(title, content) }
        )
    }

    // Delete Document Dialog
    uiState.showDeleteDialogFor?.let { doc ->
        DeleteConfirmDialog(
            document = doc,
            onDismiss = { viewModel.triggerDeleteDialog(null) },
            onConfirm = { viewModel.deleteDocument(doc) }
        )
    }
}

@Composable
fun EmptyStateView(
    onImportClick: () -> Unit,
    onPasteClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            // Dynamic speaking wave aesthetic using raw typography
            Text("📚", fontSize = 48.sp)
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Nessun documento inserito",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Converti qualsiasi file di testo o appunti in voce parlata chiara. Inizia importando un documento o inserendo il testo manualmente.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onPasteClick,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("empty_state_paste_button")
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Copia e Incolla Testo")
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        OutlinedButton(
            onClick = { onImportClick() },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("empty_state_import_button")
        ) {
            Icon(Icons.Default.Share, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Importa File .txt")
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DocumentItemCard(
    document: Document,
    isActive: Boolean,
    onPlayClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val formattedDate = remember(document.createdAt) {
        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        sdf.format(Date(document.createdAt))
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = onPlayClick,
                onLongClick = onDeleteClick
            )
            .testTag("document_card_${document.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            }
        ),
        shape = RoundedCornerShape(16.dp),
        border = if (isActive) {
            CardDefaults.outlinedCardBorder().copy(
                brush = Brush.linearGradient(
                    colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                )
            )
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Audio Indicator or Doc Icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isActive) "🎵" else "📄",
                    fontSize = 20.sp
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = document.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = document.content,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "⏱️ ${document.speechDurationMinutes} min",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "$formattedDate",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier.testTag("delete_document_${document.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Rimuovi documento",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun CollapsedFloatingPlayer(
    uiState: MainViewModel.UiState,
    onExpandClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onCloseClick: () -> Unit
) {
    val document = uiState.activeDocument ?: return

    val progress = remember(uiState.activeSentenceIndex, uiState.sentences.size) {
        if (uiState.sentences.isEmpty() || uiState.activeSentenceIndex < 0) 0f
        else (uiState.activeSentenceIndex + 1).toFloat() / uiState.sentences.size.toFloat()
    }

    Surface(
        tonalElevation = 8.dp,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = CardDefaults.outlinedCardBorder(),
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onExpandClick() }
            .testTag("collapsed_player_bar")
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onPlayPauseClick) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (uiState.isPlaying) {
                            // Custom Pause shape inside a circle
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                modifier = Modifier.align(Alignment.Center)
                            ) {
                                Box(modifier = Modifier.size(width = 3.dp, height = 12.dp).background(MaterialTheme.colorScheme.onPrimary))
                                Box(modifier = Modifier.size(width = 3.dp, height = 12.dp).background(MaterialTheme.colorScheme.onPrimary))
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = document.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (uiState.activeSentenceIndex >= 0 && uiState.activeSentenceIndex < uiState.sentences.size) {
                            uiState.sentences[uiState.activeSentenceIndex]
                        } else {
                            "Ascolta file"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(onClick = onExpandClick) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Espandi visualizzazione lettura"
                    )
                }

                IconButton(onClick = onCloseClick) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Chiudi lettore"
                    )
                }
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(4.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
            )
        }
    }
}

@Composable
fun ImmersiveReaderView(
    uiState: MainViewModel.UiState,
    onBackClick: () -> Unit,
    onPlayPauseClick: () -> Unit,
    onStopClick: () -> Unit,
    onNextClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onSentenceClick: (Int) -> Unit,
    onSpeedChange: (Float) -> Unit,
    onPitchChange: (Float) -> Unit,
    onLanguageSelect: (Locale) -> Unit
) {
    val document = uiState.activeDocument ?: return
    val listState = rememberLazyListState()
    var isSettingsVisible by remember { mutableStateOf(false) }

    // Automatic smooth scroll to follow word highlight or active sentence
    LaunchedEffect(uiState.activeSentenceIndex) {
        if (uiState.activeSentenceIndex in uiState.sentences.indices) {
            // Center the sentence in the reading pane
            val scrollOffset = 180
            listState.animateScrollToItem(
                index = uiState.activeSentenceIndex,
                scrollOffset = -scrollOffset
            )
        }
    }

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("immersive_reader_view")
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // IMMERSIVE TOP NAVIGATION
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.testTag("reader_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = "Torna indietro"
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.widthIn(max = 200.dp)) {
                        Text(
                            text = document.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${uiState.sentences.size} segmenti di lettura",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }

                Row {
                    IconButton(onClick = { isSettingsVisible = !isSettingsVisible }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Impostazioni voce",
                            tint = if (isSettingsVisible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    }
                    IconButton(onClick = onStopClick) {
                        // Custom Stop button vector drawing
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                        )
                    }
                }
            }

            // SETTINGS SUB-DRAWER (SPEED, PITCH, VOICES)
            AnimatedVisibility(visible = isSettingsVisible) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Opzioni di Sintesi Vocale",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        // SPEED SLIDER
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Velocità lettura",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "${uiState.speed}x",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Slider(
                                value = uiState.speed,
                                onValueChange = onSpeedChange,
                                valueRange = 0.5f..2.5f,
                                steps = 19,
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.primary,
                                    activeTrackColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }

                        // PITCH SLIDER
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Tonalità voce",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "${uiState.pitch}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                            Slider(
                                value = uiState.pitch,
                                onValueChange = onPitchChange,
                                valueRange = 0.5f..2.0f,
                                steps = 14,
                                colors = SliderDefaults.colors(
                                    thumbColor = MaterialTheme.colorScheme.secondary,
                                    activeTrackColor = MaterialTheme.colorScheme.secondary
                                )
                            )
                        }

                        // LANGUAGE CHIPS
                        if (uiState.availableLanguages.isNotEmpty()) {
                            Column {
                                Text(
                                    text = "Lingua / Accento: ",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    val topLanguages = listOf(Locale.ITALY, Locale.US, Locale.UK)
                                    topLanguages.forEach { locale ->
                                        val isSelected = uiState.selectedLanguage.language == locale.language
                                        Surface(
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                                            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                            shape = RoundedCornerShape(8.dp),
                                            border = CardDefaults.outlinedCardBorder(),
                                            modifier = Modifier
                                                .clickable { onLanguageSelect(locale) }
                                                .padding(2.dp)
                                        ) {
                                            Text(
                                                text = locale.displayLanguage.capitalize(Locale.getDefault()),
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // IMMERSIVE READING PANE
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    itemsIndexed(uiState.sentences) { index, sentence ->
                        val isActive = index == uiState.activeSentenceIndex

                        val annotatedText = buildAnnotatedString {
                            if (isActive && uiState.currentWordRange != null) {
                                val range = uiState.currentWordRange
                                val start = range.first.coerceIn(0, sentence.length)
                                val end = range.second.coerceIn(0, sentence.length)

                                if (start < end) {
                                    append(sentence.substring(0, start))
                                    withStyle(
                                        style = SpanStyle(
                                            background = MaterialTheme.colorScheme.primary,
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    ) {
                                        append(sentence.substring(start, end))
                                    }
                                    append(sentence.substring(end))
                                } else {
                                    append(sentence)
                                }
                            } else {
                                append(sentence)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isActive) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                            } else Color.Transparent,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSentenceClick(index) }
                                .padding(vertical = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.width(28.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = annotatedText,
                                    style = if (isActive) {
                                        MaterialTheme.typography.bodyLarge.copy(lineHeight = 28.sp)
                                    } else {
                                        MaterialTheme.typography.bodyLarge.copy(lineHeight = 28.sp)
                                    },
                                    color = if (isActive) {
                                        MaterialTheme.colorScheme.onSurface
                                    } else {
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    },
                                    fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(160.dp))
                    }
                }

                // Scroll to top badge
                if (listState.firstVisibleItemIndex > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(bottom = 24.dp)
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.clickable {
                                // Scroll back up to start
                                onSentenceClick(0)
                            }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.KeyboardArrowUp, 
                                    contentDescription = null, 
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Text(
                                    text = "All'inizio", 
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }
            }

            // IMMERSIVE BOTTOM MEDIA CONTROLS
            Surface(
                tonalElevation = 12.dp,
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val progress = remember(uiState.activeSentenceIndex, uiState.sentences.size) {
                        if (uiState.sentences.isEmpty() || uiState.activeSentenceIndex < 0) 0f
                        else (uiState.activeSentenceIndex + 1).toFloat() / uiState.sentences.size.toFloat()
                    }

                    // Progress indicators
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (uiState.activeSentenceIndex >= 0) "${uiState.activeSentenceIndex + 1} di ${uiState.sentences.size}" else "0 di 0",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.outline
                        )
                        Text(
                            text = "${(progress * 100).toInt()}% completato",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Previous Sentence Call Button
                        IconButton(
                            onClick = onPreviousClick,
                            enabled = uiState.activeSentenceIndex > 0,
                            modifier = Modifier.size(54.dp).testTag("prev_sentence_button")
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        if (uiState.activeSentenceIndex > 0) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent, 
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                // Draw beautifully styled left arrow
                                Text("⏮️", fontSize = 20.sp)
                            }
                        }

                        Spacer(modifier = Modifier.width(20.dp))

                        // Large Play-Pause button container
                        IconButton(
                            onClick = onPlayPauseClick,
                            modifier = Modifier.size(72.dp).testTag("play_pause_button")
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (uiState.isPlaying) {
                                    // Robust pause capsules
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.align(Alignment.Center)
                                    ) {
                                        Box(modifier = Modifier.size(width = 5.dp, height = 24.dp).background(MaterialTheme.colorScheme.onPrimary))
                                        Box(modifier = Modifier.size(width = 5.dp, height = 24.dp).background(MaterialTheme.colorScheme.onPrimary))
                                    }
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Riproduci",
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(20.dp))

                        // Next Sentence Call Button
                        IconButton(
                            onClick = onNextClick,
                            enabled = uiState.activeSentenceIndex < uiState.sentences.size - 1,
                            modifier = Modifier.size(54.dp).testTag("next_sentence_button")
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        if (uiState.activeSentenceIndex < uiState.sentences.size - 1) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent, 
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                // Draw beautifully styled right arrow
                                Text("⏭️", fontSize = 20.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
fun AddDocumentDialog(
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Nuovo Documento",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Titolo (opzionale)") },
                    placeholder = { Text("es. Capitolo Spagnolo, Ricetta...") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("add_document_title_input")
                )

                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Contenuto di testo") },
                    placeholder = { Text("Incolla o scrivi il testo del documento qui per essere letto e ascoltato...") },
                    minLines = 4,
                    maxLines = 10,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Default
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("add_document_content_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(title, content)
                },
                enabled = content.isNotBlank(),
                modifier = Modifier.testTag("save_document_button")
            ) {
                Text("Crea Documento")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("dismiss_add_document_button")
            ) {
                Text("Annulla")
            }
        }
    )
}

@Composable
fun DeleteConfirmDialog(
    document: Document,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Default.Info, 
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = {
            Text(
                text = "Elimina Documento",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = "Sei sicuro di voler eliminare definitivamente il documento \"${document.title}\"? Questa operazione non può essere annullata.",
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.testTag("confirm_delete_button")
            ) {
                Text("Elimina", color = MaterialTheme.colorScheme.onError)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("dismiss_delete_button")
            ) {
                Text("Annulla")
            }
        }
    )
}
