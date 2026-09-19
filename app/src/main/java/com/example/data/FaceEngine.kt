package com.example.data

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.graphics.Rect
import android.util.Base64
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.google.mlkit.vision.face.FaceLandmark
import java.io.ByteArrayOutputStream
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Result of a face registration attempt.
 */
data class FaceRegistrationResponse(
    val success: Boolean,
    val status: String, // "SUCCESS", "NO_FACE_DETECTED", "MULTIPLE_FACES", "DECODE_ERROR"
    val message: String,
    val faceImageBase64: String? = null,
    val landmarkRatio: Float? = null,
    val boxRatio: Float? = null,
    val featureVector: List<Float> = emptyList(),
    val boundingBox: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Result of face verification against registered baseline.
 */
data class FaceVerificationResponse(
    val verified: Boolean,
    val confidence: Float,
    val status: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class ExtractedFaceMetrics(
    val boxRatio: Float,
    val landmarkRatio: Float?,
    val features: List<Float>
)

object FaceEngine {

    // Accurate detector for registration
    private val registrationDetector by lazy {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setMinFaceSize(0.12f)
            .build()
        FaceDetection.getClient(options)
    }

    /**
     * Decodes Base64 string to Bitmap with robust data-URI cleaning, URL-safe support,
     * and whitespace normalization.
     */
    fun decodeBase64ToBitmap(base64Str: String): Bitmap? {
        return try {
            val raw = if (base64Str.contains(",")) {
                base64Str.substringAfter(",")
            } else {
                base64Str
            }
            val sanitized = raw.trim()
                .replace("\n", "")
                .replace("\r", "")
                .replace(" ", "+")

            val decodedBytes = try {
                Base64.decode(sanitized, Base64.DEFAULT)
            } catch (e: Exception) {
                Base64.decode(sanitized, Base64.URL_SAFE)
            }
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Encodes a Bitmap to a clean Base64 JPEG string (with data URI prefix).
     */
    fun encodeBitmapToBase64(bitmap: Bitmap, quality: Int = 85): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        val bytes = outputStream.toByteArray()
        val base64Data = Base64.encodeToString(bytes, Base64.NO_WRAP)
        return "data:image/jpeg;base64,$base64Data"
    }

    /**
     * Crops the detected face bounding box safely from original frame with bounds protection.
     */
    fun cropFace(bitmap: Bitmap, box: Rect): Bitmap {
        return try {
            val padding = (max(box.width(), box.height()) * 0.15f).toInt()
            val left = max(0, box.left - padding)
            val top = max(0, box.top - padding)
            val right = min(bitmap.width, box.right + padding)
            val bottom = min(bitmap.height, box.bottom + padding)

            val width = right - left
            val height = bottom - top

            if (left >= bitmap.width || top >= bitmap.height || width <= 0 || height <= 0) {
                Bitmap.createScaledBitmap(bitmap, 160, 160, true)
            } else {
                val safeWidth = min(width, bitmap.width - left)
                val safeHeight = min(height, bitmap.height - top)
                val cropped = Bitmap.createBitmap(bitmap, left, top, safeWidth, safeHeight)
                Bitmap.createScaledBitmap(cropped, 160, 160, true)
            }
        } catch (e: Exception) {
            Bitmap.createScaledBitmap(bitmap, 160, 160, true)
        }
    }

    /**
     * Extracts multi-point geometric landmark vector and both box aspect ratio and landmark ratio.
     */
    fun extractMetrics(face: Face): ExtractedFaceMetrics {
        val box = face.boundingBox
        val boxRatio = if (box.height() > 0) {
            box.width().toFloat() / box.height().toFloat()
        } else {
            0.85f
        }

        val leftEye = face.getLandmark(FaceLandmark.LEFT_EYE)
        val rightEye = face.getLandmark(FaceLandmark.RIGHT_EYE)
        val noseBase = face.getLandmark(FaceLandmark.NOSE_BASE)
        val mouthLeft = face.getLandmark(FaceLandmark.MOUTH_LEFT)
        val mouthRight = face.getLandmark(FaceLandmark.MOUTH_RIGHT)

        val features = mutableListOf<Float>()
        features.add(boxRatio)

        var landmarkRatio: Float? = null
        if (leftEye != null && rightEye != null && noseBase != null) {
            val dxEyes = leftEye.position.x - rightEye.position.x
            val dyEyes = leftEye.position.y - rightEye.position.y
            val distEyes = sqrt(dxEyes * dxEyes + dyEyes * dyEyes)

            val dxNose = leftEye.position.x - noseBase.position.x
            val dyNose = leftEye.position.y - noseBase.position.y
            val distEyeNose = sqrt(dxNose * dxNose + dyNose * dyNose)

            if (distEyeNose > 0.001f) {
                val ratio = distEyes / distEyeNose
                landmarkRatio = ratio
                features.add(ratio)
            }

            if (mouthLeft != null && mouthRight != null && distEyes > 0.001f) {
                val dxMouth = mouthLeft.position.x - mouthRight.position.x
                val dyMouth = mouthLeft.position.y - mouthRight.position.y
                val distMouth = sqrt(dxMouth * dxMouth + dyMouth * dyMouth)
                features.add(distMouth / distEyes)
            }
        }

        // Add pose angles to feature vector
        features.add(face.headEulerAngleX)
        features.add(face.headEulerAngleY)
        features.add(face.headEulerAngleZ)

        return ExtractedFaceMetrics(boxRatio, landmarkRatio, features)
    }

    /**
     * Executes Face Registration pipeline:
     * - Validates image bitmap
     * - Runs face detection
     * - Selects dominant primary face
     * - Crops face and converts to Base64 image
     * - Extracts landmark metrics
     * - Returns structured response
     */
    fun registerFaceFromBitmap(
        bitmap: Bitmap,
        rotationDegrees: Int = 0,
        onComplete: (FaceRegistrationResponse) -> Unit
    ) {
        val orientedBitmap = if (rotationDegrees != 0) {
            val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } else {
            bitmap
        }

        val inputImage = InputImage.fromBitmap(orientedBitmap, 0)
        registrationDetector.process(inputImage)
            .addOnSuccessListener { faces ->
                if (faces.isEmpty()) {
                    onComplete(
                        FaceRegistrationResponse(
                            success = false,
                            status = "NO_FACE_DETECTED",
                            message = "No face detected in camera frame. Ensure good lighting and look directly into the camera."
                        )
                    )
                } else {
                    // Pick the dominant face (largest bounding box area)
                    val primaryFace = faces.maxByOrNull { it.boundingBox.width() * it.boundingBox.height() } ?: faces.first()

                    val metrics = extractMetrics(primaryFace)
                    val croppedFace = cropFace(orientedBitmap, primaryFace.boundingBox)
                    val base64Face = encodeBitmapToBase64(croppedFace)
                    val box = primaryFace.boundingBox
                    val boxStr = "${box.left},${box.top},${box.width()},${box.height()}"

                    onComplete(
                        FaceRegistrationResponse(
                            success = true,
                            status = "SUCCESS",
                            message = "Face captured, detected, and saved successfully.",
                            faceImageBase64 = base64Face,
                            landmarkRatio = metrics.landmarkRatio ?: metrics.boxRatio,
                            boxRatio = metrics.boxRatio,
                            featureVector = metrics.features,
                            boundingBox = boxStr
                        )
                    )
                }
            }
            .addOnFailureListener { error ->
                onComplete(
                    FaceRegistrationResponse(
                        success = false,
                        status = "DETECTION_ERROR",
                        message = "Face detection error: ${error.localizedMessage ?: "Unknown"}"
                    )
                )
            }
    }

    /**
     * Compares live face features against registered baseline.
     * Robust against minor head movement and lighting shifts.
     */
    fun verifyFace(
        liveFace: Face,
        registeredRatio: Float,
        registeredFeatures: List<Float> = emptyList()
    ): FaceVerificationResponse {
        val liveMetrics = extractMetrics(liveFace)

        // 1. Landmark or Box Ratio comparison
        val activeLiveRatio = liveMetrics.landmarkRatio ?: liveMetrics.boxRatio
        val ratioDiff = abs(activeLiveRatio - registeredRatio)

        // Tolerant matching: normal human facial landmark variance allows 0.35 diff
        val ratioConfidence = max(0f, 1.0f - (ratioDiff / 0.45f))

        // 2. Multi-feature vector comparison if available
        var featureConfidence = ratioConfidence
        if (registeredFeatures.isNotEmpty() && liveMetrics.features.size >= 2) {
            val count = min(liveMetrics.features.size, registeredFeatures.size)
            var diffSum = 0f
            for (i in 0 until count) {
                val f1 = liveMetrics.features[i]
                val f2 = registeredFeatures[i]
                val denom = max(0.05f, abs(f1) + abs(f2))
                diffSum += abs(f1 - f2) / denom
            }
            val avgDiff = diffSum / count
            featureConfidence = max(0f, 1.0f - avgDiff)
        }

        // Weighted total confidence
        val totalConfidence = (ratioConfidence * 0.65f) + (featureConfidence * 0.35f)

        // Verification threshold
        val isVerified = totalConfidence >= 0.45f

        return FaceVerificationResponse(
            verified = isVerified,
            confidence = totalConfidence,
            status = if (isVerified) "VERIFIED" else "MISMATCH",
            message = if (isVerified) "Face Verified: Access Granted" else "Unauthorized Face Detected"
        )
    }
}
