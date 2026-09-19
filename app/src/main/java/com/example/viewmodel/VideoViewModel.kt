package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.google.mlkit.vision.face.Face
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class LockState {
    SETUP_PASSWORD,
    SETUP_RECOVERY,
    ENTER_PASSWORD,
    UNLOCKED,
    RECOVERY_RESET
}

class VideoViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: AppRepository

    val allVideos: StateFlow<List<VideoEntity>>

    private val _lockState = MutableStateFlow(LockState.ENTER_PASSWORD)
    val lockState: StateFlow<LockState> = _lockState.asStateFlow()

    private val _passcode = MutableStateFlow("")
    val passcode: StateFlow<String> = _passcode.asStateFlow()

    private val _tempPasscode = MutableStateFlow("")
    val tempPasscode: StateFlow<String> = _tempPasscode.asStateFlow()

    private val _recoveryQuestion = MutableStateFlow("What is the name of your first pet?")
    val recoveryQuestion: StateFlow<String> = _recoveryQuestion.asStateFlow()

    private val _recoveryAnswer = MutableStateFlow("")
    val recoveryAnswer: StateFlow<String> = _recoveryAnswer.asStateFlow()

    private val _recoveryAnswerInput = MutableStateFlow("")
    val recoveryAnswerInput: StateFlow<String> = _recoveryAnswerInput.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _activeVideo = MutableStateFlow<VideoEntity?>(null)
    val activeVideo: StateFlow<VideoEntity?> = _activeVideo.asStateFlow()

    // Represents if the secure RED button is currently held down
    private val _videoPlaying = MutableStateFlow(false)
    val videoPlaying: StateFlow<Boolean> = _videoPlaying.asStateFlow()

    private val _faceSecurityEnabled = MutableStateFlow(false)
    val faceSecurityEnabled: StateFlow<Boolean> = _faceSecurityEnabled.asStateFlow()

    private val _registeredFaceRatio = MutableStateFlow<Float?>(null)
    val registeredFaceRatio: StateFlow<Float?> = _registeredFaceRatio.asStateFlow()

    private val _registeredFaceImage = MutableStateFlow<String?>(null)
    val registeredFaceImage: StateFlow<String?> = _registeredFaceImage.asStateFlow()

    private val _registeredFaceFeatures = MutableStateFlow<List<Float>>(emptyList())
    val registeredFaceFeatures: StateFlow<List<Float>> = _registeredFaceFeatures.asStateFlow()

    private val _registeredFaceTimestamp = MutableStateFlow<Long?>(null)
    val registeredFaceTimestamp: StateFlow<Long?> = _registeredFaceTimestamp.asStateFlow()

    private val _isFaceVerified = MutableStateFlow(false)
    val isFaceVerified: StateFlow<Boolean> = _isFaceVerified.asStateFlow()

    private val _faceVerificationConfidence = MutableStateFlow(0f)
    val faceVerificationConfidence: StateFlow<Float> = _faceVerificationConfidence.asStateFlow()

    private val _faceDetectionStatus = MutableStateFlow("Inactive")
    val faceDetectionStatus: StateFlow<String> = _faceDetectionStatus.asStateFlow()

    private val _lastRegistrationResponse = MutableStateFlow<FaceRegistrationResponse?>(null)
    val lastRegistrationResponse: StateFlow<FaceRegistrationResponse?> = _lastRegistrationResponse.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(application)
        repository = AppRepository(database)
        allVideos = repository.allVideos.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        checkSecurityConfig()
    }

    private fun checkSecurityConfig() {
        viewModelScope.launch {
            val savedPassword = repository.getConfig("password")
            val savedQuestion = repository.getConfig("recovery_question")
            val savedAnswer = repository.getConfig("recovery_answer")

            if (savedPassword.isNullOrEmpty()) {
                _lockState.value = LockState.SETUP_PASSWORD
            } else {
                _lockState.value = LockState.ENTER_PASSWORD
                _recoveryQuestion.value = savedQuestion ?: "What is the name of your first pet?"
                _recoveryAnswer.value = savedAnswer ?: ""
            }

            val savedFaceEnabled = repository.getConfig("face_security_enabled") == "true"
            val savedFaceRatio = repository.getConfig("registered_face_ratio")?.toFloatOrNull()
            val savedFaceImage = repository.getConfig("registered_face_image")
            val savedFaceFeatures = repository.getConfig("registered_face_features")
            val savedFaceTimestamp = repository.getConfig("registered_face_timestamp")?.toLongOrNull()

            _faceSecurityEnabled.value = savedFaceEnabled
            _registeredFaceRatio.value = savedFaceRatio
            _registeredFaceImage.value = savedFaceImage
            _registeredFaceTimestamp.value = savedFaceTimestamp
            if (!savedFaceFeatures.isNullOrEmpty()) {
                _registeredFaceFeatures.value = savedFaceFeatures.split(",").mapNotNull { it.toFloatOrNull() }
            }

            // Prep defaults if video library is empty
            viewModelScope.launch {
                repository.allVideos.collect { videos ->
                    if (videos.isEmpty()) {
                        insertDefaultVideos()
                    } else if (_activeVideo.value == null) {
                        _activeVideo.value = videos.firstOrNull()
                    }
                }
            }
        }
    }

    private suspend fun insertDefaultVideos() {
        val defaults = listOf(
            VideoEntity(
                videoId = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                title = "Firebase Cloud Video - Big Buck Bunny (HTML5 Stream)",
                durationText = "09:56"
            ),
            VideoEntity(
                videoId = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4",
                title = "Firebase Storage Sample - Elephants Dream (HD Stream)",
                durationText = "10:53"
            ),
            VideoEntity(
                videoId = "Ke90Tje7VS0",
                title = "Majestic Nature - 4K Drone Footage",
                durationText = "03:15"
            ),
            VideoEntity(
                videoId = "tVixyIyeAnY",
                title = "Lofi Hip Hop Radio - Beats to Study/Relax",
                durationText = "24:00"
            )
        )
        for (video in defaults) {
            repository.insertVideo(video)
        }
    }

    fun handleKeyPress(digit: String) {
        _authError.value = null
        if (_passcode.value.length < 6) {
            _passcode.value += digit
        }
    }

    fun handleDeletePress() {
        _authError.value = null
        if (_passcode.value.isNotEmpty()) {
            _passcode.value = _passcode.value.dropLast(1)
        }
    }

    fun handleClearPress() {
        _passcode.value = ""
        _authError.value = null
    }

    fun submitPasscode() {
        viewModelScope.launch {
            val savedPassword = repository.getConfig("password") ?: ""
            if (_passcode.value == savedPassword) {
                _lockState.value = LockState.UNLOCKED
                _passcode.value = ""
                _authError.value = null
            } else {
                _authError.value = "Incorrect passcode. Please try again."
                _passcode.value = ""
            }
        }
    }

    fun submitSetupPassword() {
        if (_passcode.value.length < 4) {
            _authError.value = "Password must be at least 4 digits"
            return
        }
        _tempPasscode.value = _passcode.value
        _passcode.value = ""
        _lockState.value = LockState.SETUP_RECOVERY
        _authError.value = null
    }

    fun submitSecuritySetup(question: String, answer: String) {
        if (answer.trim().isEmpty()) {
            _authError.value = "Answer cannot be empty"
            return
        }
        viewModelScope.launch {
            repository.setConfig("password", _tempPasscode.value)
            repository.setConfig("recovery_question", question)
            repository.setConfig("recovery_answer", answer.trim().lowercase())

            _recoveryQuestion.value = question
            _recoveryAnswer.value = answer.trim().lowercase()
            _passcode.value = ""
            _tempPasscode.value = ""
            _lockState.value = LockState.UNLOCKED
            _authError.value = null
        }
    }

    fun onAnswerInputChanged(input: String) {
        _recoveryAnswerInput.value = input
        _authError.value = null
    }

    fun submitRecoveryAnswer() {
        if (_recoveryAnswerInput.value.trim().lowercase() == _recoveryAnswer.value.trim().lowercase()) {
            _lockState.value = LockState.SETUP_PASSWORD
            _recoveryAnswerInput.value = ""
            _passcode.value = ""
            _authError.value = null
        } else {
            _authError.value = "Incorrect security recovery answer."
        }
    }

    fun navigateToRecovery() {
        _lockState.value = LockState.RECOVERY_RESET
        _authError.value = null
        _passcode.value = ""
    }

    fun navigateToEnterPassword() {
        _lockState.value = LockState.ENTER_PASSWORD
        _authError.value = null
        _passcode.value = ""
    }

    fun resetLock() {
        viewModelScope.launch {
            repository.deleteConfig("password")
            repository.deleteConfig("recovery_question")
            repository.deleteConfig("recovery_answer")
            _lockState.value = LockState.SETUP_PASSWORD
            _passcode.value = ""
            _tempPasscode.value = ""
            _authError.value = null
        }
    }

    fun selectVideo(video: VideoEntity) {
        _activeVideo.value = video
    }

    /**
     * Parses the video input, supporting Firebase Storage URLs, direct MP4/WebM URLs, and YouTube links.
     * Returns Pair(sourceUrlOrId, isDirectVideo)
     */
    fun parseVideoSource(input: String): Pair<String, Boolean> {
        val trimmed = input.trim()
        val ytId = extractYoutubeVideoId(trimmed)
        if (ytId != null) {
            return Pair(ytId, false)
        }
        // Direct stream or Firebase Storage URL
        return Pair(trimmed, true)
    }

    fun addVideo(urlOrId: String, title: String) {
        val trimmed = urlOrId.trim()
        if (trimmed.isEmpty()) return

        val (videoId, isDirect) = parseVideoSource(trimmed)
        val cleanTitle = title.trim().ifBlank {
            if (isDirect) {
                if (trimmed.contains("firebasestorage")) "Firebase Cloud Video" else "Secure Video Stream"
            } else {
                "YouTube Video ($videoId)"
            }
        }
        val newVideo = VideoEntity(
            videoId = videoId,
            title = cleanTitle
        )
        _activeVideo.value = newVideo
        viewModelScope.launch {
            repository.insertVideo(newVideo)
        }
    }

    fun removeVideo(video: VideoEntity) {
        viewModelScope.launch {
            repository.deleteVideo(video)
            if (_activeVideo.value?.id == video.id) {
                _activeVideo.value = allVideos.value.firstOrNull { it.id != video.id }
            }
        }
    }

    fun setVideoPlaying(playing: Boolean) {
        _videoPlaying.value = playing
        if (!playing) {
            _isFaceVerified.value = false
            _faceDetectionStatus.value = "Button Released • Shield Active"
        }
    }

    fun setFaceSecurityEnabled(enabled: Boolean) {
        viewModelScope.launch {
            _faceSecurityEnabled.value = enabled
            repository.setConfig("face_security_enabled", enabled.toString())
            if (!enabled) {
                _isFaceVerified.value = false
            }
        }
    }

    /**
     * Registers and saves face profile from FaceEngine response.
     * Persists Base64 image, landmark ratio, and geometric feature vector.
     */
    fun saveFaceRegistration(response: FaceRegistrationResponse) {
        _lastRegistrationResponse.value = response
        if (!response.success) {
            _faceDetectionStatus.value = response.message
            return
        }

        viewModelScope.launch {
            _registeredFaceRatio.value = response.landmarkRatio
            _registeredFaceImage.value = response.faceImageBase64
            _registeredFaceFeatures.value = response.featureVector
            _registeredFaceTimestamp.value = response.timestamp
            _faceSecurityEnabled.value = true
            _isFaceVerified.value = true
            _faceVerificationConfidence.value = 1.0f

            response.landmarkRatio?.let {
                repository.setConfig("registered_face_ratio", it.toString())
            }
            response.faceImageBase64?.let {
                repository.setConfig("registered_face_image", it)
            }
            if (response.featureVector.isNotEmpty()) {
                repository.setConfig("registered_face_features", response.featureVector.joinToString(","))
            }
            repository.setConfig("registered_face_timestamp", response.timestamp.toString())
            repository.setConfig("face_security_enabled", "true")

            _faceDetectionStatus.value = "Face Saved Successfully • Shield Active"
        }
    }

    fun registerFace(ratio: Float) {
        viewModelScope.launch {
            _registeredFaceRatio.value = ratio
            repository.setConfig("registered_face_ratio", ratio.toString())
            _faceSecurityEnabled.value = true
            repository.setConfig("face_security_enabled", "true")
        }
    }

    fun clearRegisteredFace() {
        viewModelScope.launch {
            _registeredFaceRatio.value = null
            _registeredFaceImage.value = null
            _registeredFaceFeatures.value = emptyList()
            _registeredFaceTimestamp.value = null
            _faceSecurityEnabled.value = false
            _isFaceVerified.value = false
            _faceVerificationConfidence.value = 0f
            _lastRegistrationResponse.value = null

            repository.deleteConfig("registered_face_ratio")
            repository.deleteConfig("registered_face_image")
            repository.deleteConfig("registered_face_features")
            repository.deleteConfig("registered_face_timestamp")
            repository.setConfig("face_security_enabled", "false")

            _faceDetectionStatus.value = "Face Registration Cleared"
        }
    }

    private var lastVerificationTimeMs = 0L

    fun updateFaceVerification(face: Face?) {
        if (face == null) {
            if (_isFaceVerified.value) {
                _isFaceVerified.value = false
                _faceVerificationConfidence.value = 0f
                _faceDetectionStatus.value = "No Face Detected"
            }
            return
        }

        val regRatio = _registeredFaceRatio.value
        if (regRatio == null) {
            // Face is actively showing in camera; baseline not set, so active presence qualifies
            if (!_isFaceVerified.value) {
                _isFaceVerified.value = true
                _faceVerificationConfidence.value = 1.0f
                _faceDetectionStatus.value = "Face Detected • Streaming Active"
            }
            return
        }

        val now = System.currentTimeMillis()
        if (now - lastVerificationTimeMs < 80L && _isFaceVerified.value) {
            return
        }
        lastVerificationTimeMs = now

        val result = FaceEngine.verifyFace(
            liveFace = face,
            registeredRatio = regRatio,
            registeredFeatures = _registeredFaceFeatures.value
        )

        if (_isFaceVerified.value != result.verified || Math.abs(_faceVerificationConfidence.value - result.confidence) > 0.05f) {
            _isFaceVerified.value = result.verified
            _faceVerificationConfidence.value = result.confidence
            _faceDetectionStatus.value = if (result.verified) {
                "Face Verified (${(result.confidence * 100).toInt()}%)"
            } else {
                "Shield: Face Mismatch (${(result.confidence * 100).toInt()}%)"
            }
        }
    }

    fun setFaceDetectionStatus(status: String) {
        _faceDetectionStatus.value = status
    }

    fun extractYoutubeVideoId(url: String): String? {
        val cleanUrl = url.trim()
        if (cleanUrl.isEmpty()) return null

        // 1. Direct 11-character ID
        if (cleanUrl.matches(Regex("^[a-zA-Z0-9_-]{11}$"))) {
            return cleanUrl
        }

        // 2. youtu.be/<id>
        val youtuBeRegex = Regex("""youtu\.be/([a-zA-Z0-9_-]{11})""", RegexOption.IGNORE_CASE)
        youtuBeRegex.find(cleanUrl)?.let {
            return it.groupValues[1]
        }

        // 3. /shorts/<id> or /live/<id> or /embed/<id> or /v/<id>
        val pathRegex = Regex("""(?:shorts|live|embed|v)/([a-zA-Z0-9_-]{11})""", RegexOption.IGNORE_CASE)
        pathRegex.find(cleanUrl)?.let {
            return it.groupValues[1]
        }

        // 4. v=<id> query parameter (e.g. ?v=..., &v=..., on www, m, music, or plain youtube.com)
        val vParamRegex = Regex("""[?&]v=([a-zA-Z0-9_-]{11})""", RegexOption.IGNORE_CASE)
        vParamRegex.find(cleanUrl)?.let {
            return it.groupValues[1]
        }

        // 5. General fallback: if 'youtu' is present, search for 11-char alphanumeric segment
        if (cleanUrl.contains("youtu", ignoreCase = true)) {
            val generalRegex = Regex("""(?<![a-zA-Z0-9_-])([a-zA-Z0-9_-]{11})(?![a-zA-Z0-9_-])""")
            for (match in generalRegex.findAll(cleanUrl)) {
                val candidate = match.groupValues[1]
                val lower = candidate.lowercase()
                if (!lower.contains("youtu") && lower != "channel" && lower != "watch" && !lower.startsWith("http")) {
                    return candidate
                }
            }
        }

        return null
    }
}
