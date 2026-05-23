package com.example.ui

import android.app.Application
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.Document
import com.example.data.DocumentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.util.Locale

class MainViewModel(application: Application) : AndroidViewModel(application), TextToSpeech.OnInitListener {

    private val repository: DocumentRepository
    val documents: StateFlow<List<Document>>

    private var tts: TextToSpeech? = null

    // UI and Playback State
    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    data class UiState(
        val isTtsInitialized: Boolean = false,
        val availableLanguages: List<Locale> = emptyList(),
        val selectedLanguage: Locale = Locale.getDefault(),
        
        // Active document details
        val activeDocument: Document? = null,
        val sentences: List<String> = emptyList(),
        
        // Playback state
        val isPlaying: Boolean = false,
        val isPaused: Boolean = false,
        val activeSentenceIndex: Int = -1,
        val currentWordRange: Pair<Int, Int>? = null, // (start, end) indices in the currently playing sentence
        
        // Global speech settings
        val speed: Float = 1.0f,
        val pitch: Float = 1.0f,
        
        // UI Navigation / Overlay state
        val isPlayerExpanded: Boolean = false,
        val showDeleteDialogFor: Document? = null,
        val showAddDocumentSheet: Boolean = false
    )

    init {
        val db = AppDatabase.getDatabase(application)
        repository = DocumentRepository(db.documentDao())
        
        documents = repository.allDocuments.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Initialize TTS Engine
        try {
            tts = TextToSpeech(application, this)
        } catch (e: Exception) {
            Log.e("MainViewModel", "Error creating TTS instance", e)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val availableLocales = mutableListOf<Locale>()
            tts?.let { engine ->
                // Gather some standard speech Locales that might be supported
                val testLocales = listOf(
                    Locale.ITALIAN, Locale.ITALY,
                    Locale.ENGLISH, Locale.US, Locale.UK,
                    Locale.FRENCH, Locale.GERMAN, Locale("es")
                )
                for (locale in testLocales) {
                    try {
                        val res = engine.isLanguageAvailable(locale)
                        if (res >= TextToSpeech.LANG_AVAILABLE) {
                            availableLocales.add(locale)
                        }
                    } catch (e: Exception) {
                        // Ignore locale check failures
                    }
                }
                
                // Add default
                val defaultLang = engine.defaultVoice?.locale ?: Locale.getDefault()
                if (!availableLocales.contains(defaultLang)) {
                    availableLocales.add(0, defaultLang)
                }
                
                engine.setLanguage(defaultLang)
                
                // Set listener for word highlights and completion tracking
                engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        val index = parseUtteranceId(utteranceId) ?: return
                        viewModelScope.launch(Dispatchers.Main) {
                            _uiState.update { 
                                it.copy(
                                    isPlaying = true, 
                                    isPaused = false,
                                    activeSentenceIndex = index,
                                    currentWordRange = null
                                ) 
                            }
                        }
                    }

                    override fun onDone(utteranceId: String?) {
                        val index = parseUtteranceId(utteranceId) ?: return
                        viewModelScope.launch(Dispatchers.Main) {
                            // Check if this was the sentence we was playing and if there are more
                            val sentences = _uiState.value.sentences
                            if (_uiState.value.isPlaying && index + 1 < sentences.size) {
                                _uiState.update { it.copy(activeSentenceIndex = index + 1) }
                                saveLastReadPosition(index + 1)
                                playCurrentSentence()
                            } else {
                                // Reached the end
                                stopPlayback()
                                saveLastReadPosition(0) // reset progress on full completion
                            }
                        }
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        viewModelScope.launch(Dispatchers.Main) {
                            stopPlayback()
                            Toast.makeText(getApplication(), "Errore durante la sintesi vocale", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
                        viewModelScope.launch(Dispatchers.Main) {
                            _uiState.update { 
                                it.copy(currentWordRange = Pair(start, end)) 
                            }
                        }
                    }
                })
            }

            _uiState.update {
                it.copy(
                    isTtsInitialized = true,
                    availableLanguages = availableLocales.distinct()
                )
            }
        } else {
            Log.e("MainViewModel", "TTS Initialization failed")
        }
    }

    private fun parseUtteranceId(utteranceId: String?): Int? {
        if (utteranceId == null) return null
        return if (utteranceId.startsWith("sentence_")) {
            utteranceId.substringAfter("sentence_").toIntOrNull()
        } else null
    }

    // Speech Control Interface
    fun loadDocument(document: Document) {
        val sentences = splitTextIntoSentences(document.content)
        _uiState.update {
            it.copy(
                activeDocument = document,
                sentences = sentences,
                activeSentenceIndex = document.lastReadPosition.coerceIn(0, sentences.size - 1).coerceAtLeast(0),
                speed = document.speed,
                pitch = document.pitch,
                isPlaying = false,
                isPaused = false,
                currentWordRange = null,
                isPlayerExpanded = true // Auto-expand when loading a doc to ready it!
            )
        }
        stopTts()
    }

    fun play() {
        val state = _uiState.value
        if (state.activeDocument == null || state.sentences.isEmpty()) return
        
        // If active index is invalid or at end, reset to 0
        val targetIndex = if (state.activeSentenceIndex < 0 || state.activeSentenceIndex >= state.sentences.size) {
            0
        } else {
            state.activeSentenceIndex
        }

        _uiState.update { 
            it.copy(
                isPlaying = true, 
                isPaused = false,
                activeSentenceIndex = targetIndex
            ) 
        }
        playCurrentSentence()
    }

    fun pause() {
        _uiState.update { it.copy(isPlaying = false, isPaused = true) }
        stopTts()
    }

    fun stopPlayback() {
        _uiState.update {
            it.copy(
                isPlaying = false,
                isPaused = false,
                activeSentenceIndex = -1,
                currentWordRange = null
            )
        }
        stopTts()
    }

    fun next() {
        val state = _uiState.value
        if (state.activeSentenceIndex < state.sentences.size - 1) {
            val nextIndex = state.activeSentenceIndex + 1
            _uiState.update { it.copy(activeSentenceIndex = nextIndex, currentWordRange = null) }
            saveLastReadPosition(nextIndex)
            if (state.isPlaying) {
                playCurrentSentence()
            }
        }
    }

    fun previous() {
        val state = _uiState.value
        if (state.activeSentenceIndex > 0) {
            val prevIndex = state.activeSentenceIndex - 1
            _uiState.update { it.copy(activeSentenceIndex = prevIndex, currentWordRange = null) }
            saveLastReadPosition(prevIndex)
            if (state.isPlaying) {
                playCurrentSentence()
            }
        }
    }

    fun jumpToSentence(index: Int) {
        val state = _uiState.value
        if (index in state.sentences.indices) {
            _uiState.update { it.copy(activeSentenceIndex = index, currentWordRange = null) }
            saveLastReadPosition(index)
            if (state.isPlaying) {
                playCurrentSentence()
            } else {
                // Play automatically when clicking on a sentence
                _uiState.update { it.copy(isPlaying = true, isPaused = false) }
                playCurrentSentence()
            }
        }
    }

    fun setSpeed(speed: Float) {
        val formattedSpeed = (speed * 10).toInt().toFloat() / 10f // Snap to 0.1 increments
        _uiState.update { it.copy(speed = formattedSpeed) }
        tts?.setSpeechRate(formattedSpeed)
        
        // Update document settings locally
        val activeDoc = _uiState.value.activeDocument ?: return
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateDocument(activeDoc.copy(speed = formattedSpeed))
        }
        
        // If playing, restart current sentence with new rate
        if (_uiState.value.isPlaying) {
            playCurrentSentence()
        }
    }

    fun setPitch(pitch: Float) {
        val formattedPitch = (pitch * 10).toInt().toFloat() / 10f
        _uiState.update { it.copy(pitch = formattedPitch) }
        tts?.setPitch(formattedPitch)
        
        // Update document settings locally
        val activeDoc = _uiState.value.activeDocument ?: return
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateDocument(activeDoc.copy(pitch = formattedPitch))
        }
        
        // If playing, restart current sentence with new pitch
        if (_uiState.value.isPlaying) {
            playCurrentSentence()
        }
    }

    fun setLanguage(locale: Locale) {
        _uiState.update { it.copy(selectedLanguage = locale) }
        tts?.setLanguage(locale)
        
        // If playing, restart to speak in the new language or accents
        if (_uiState.value.isPlaying) {
            playCurrentSentence()
        }
    }

    private fun playCurrentSentence() {
        val state = _uiState.value
        val index = state.activeSentenceIndex
        if (index !in state.sentences.indices) return

        val textToSpeak = state.sentences[index]
        tts?.apply {
            setSpeechRate(state.speed)
            setPitch(state.pitch)
            setLanguage(state.selectedLanguage)
            
            // To pass parameters (such as utterance ID) on higher APIs safely, we use Bundle or HashMap
            val params = android.os.Bundle().apply {
                putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "sentence_$index")
            }
            speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, params, "sentence_$index")
        }
    }

    private fun stopTts() {
        try {
            tts?.stop()
        } catch (e: Exception) {
            Log.e("MainViewModel", "Error stopping TTS", e)
        }
    }

    private fun saveLastReadPosition(index: Int) {
        val activeDoc = _uiState.value.activeDocument ?: return
        _uiState.update { 
            it.copy(activeDocument = activeDoc.copy(lastReadPosition = index)) 
        }
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateDocument(activeDoc.copy(lastReadPosition = index))
        }
    }

    // Helper to split document text into clean, pronounceable sections
    private fun splitTextIntoSentences(text: String): List<String> {
        if (text.isBlank()) return emptyList()
        // Split by punctuation (period, question mark, exclamation, semicolon, colon) or newlines, keeping lookbehinds
        val regex = "(?<=[.!?\\n])\\s+".toRegex()
        return text.split(regex)
            .map { it.trim() }
            .filter { s -> s.any { it.isLetterOrDigit() } }
    }

    // Document DB Operations
    fun saveNewDocument(title: String, content: String) {
        if (content.isBlank()) return
        
        val actualTitle = title.trim().ifBlank { 
            // Extract first 4 words of content or generic name
            val firstWords = content.trim().split("\\s+".toRegex()).take(3).joinToString(" ")
            if (firstWords.length > 25) firstWords.take(22) + "..." else firstWords.ifBlank { "Senza Titolo" }
        }

        val newDoc = Document(
            title = actualTitle,
            content = content
        )

        viewModelScope.launch(Dispatchers.IO) {
            val id = repository.insertDocument(newDoc)
            val createdDoc = newDoc.copy(id = id.toInt())
            viewModelScope.launch(Dispatchers.Main) {
                // Auto-load the newly created document for the user
                loadDocument(createdDoc)
                _uiState.update { it.copy(showAddDocumentSheet = false) }
                Toast.makeText(getApplication(), "Documento salvato ed evidenziato!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun deleteDocument(document: Document) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteDocumentById(document.id)
            viewModelScope.launch(Dispatchers.Main) {
                if (_uiState.value.activeDocument?.id == document.id) {
                    stopPlayback()
                    _uiState.update { 
                        it.copy(
                            activeDocument = null, 
                            sentences = emptyList(), 
                            activeSentenceIndex = -1, 
                            currentWordRange = null,
                            isPlayerExpanded = false
                        )
                    }
                }
                _uiState.update { it.copy(showDeleteDialogFor = null) }
                Toast.makeText(getApplication(), "Documento rimosso correttamente", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // File Import Action
    fun importTextFromUri(inputStream: InputStream?, fileName: String?) {
        if (inputStream == null) {
            Toast.makeText(getApplication(), "Errore d'importazione del file", Toast.LENGTH_SHORT).show()
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val reader = BufferedReader(InputStreamReader(inputStream))
                val stringBuilder = java.lang.StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    stringBuilder.append(line).append("\n")
                }
                reader.close()
                val fileContent = stringBuilder.toString().trim()
                
                if (fileContent.isEmpty()) {
                    viewModelScope.launch(Dispatchers.Main) {
                        Toast.makeText(getApplication(), "Il documento importato è vuoto!", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val computedTitle = fileName?.removeSuffix(".txt")?.replace("_", " ")?.trim() 
                        ?: "File Importato"

                    viewModelScope.launch(Dispatchers.Main) {
                        saveNewDocument(computedTitle, fileContent)
                    }
                }
            } catch (e: Exception) {
                Log.e("MainViewModel", "File read error", e)
                viewModelScope.launch(Dispatchers.Main) {
                    Toast.makeText(getApplication(), "Errore di lettura del file di testo", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Sheet/Dialog triggers
    fun showAddDocument(show: Boolean) {
        _uiState.update { it.copy(showAddDocumentSheet = show) }
    }

    fun setPlayerExpanded(expanded: Boolean) {
        _uiState.update { it.copy(isPlayerExpanded = expanded) }
    }

    fun triggerDeleteDialog(doc: Document?) {
        _uiState.update { it.copy(showDeleteDialogFor = doc) }
    }

    override fun onCleared() {
        super.onCleared()
        stopTts()
        try {
            tts?.shutdown()
        } catch (e: Exception) {
            Log.e("MainViewModel", "Error shutting down TTS", e)
        }
    }
}
