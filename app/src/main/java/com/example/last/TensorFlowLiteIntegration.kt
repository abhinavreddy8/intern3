package com.example.last.viewmodels

import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.util.Locale

class TFLiteModelManager(private val context: Context) {
    private var interpreter: Interpreter? = null
    private val modelName = "cluster_model.tflite"
    private val TAG = "TFLiteModelManager"

    fun initialize() {
        try {
            interpreter = Interpreter(loadModelFile())
            Log.d(TAG, "TensorFlow Lite Model initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing TensorFlow Lite model: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun loadModelFile(): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(modelName)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    fun close() {
        interpreter?.close()
        interpreter = null
        Log.d(TAG, "TensorFlow Lite Model closed")
    }

    fun clusterDonors(donorFeatures: List<Float>): Int {
        if (interpreter == null) {
            Log.e(TAG, "Interpreter is not initialized")
            return -1
        }

        try {
            // Prepare input data
            val inputBuffer = ByteBuffer.allocateDirect(donorFeatures.size * 4) // 4 bytes per float
            inputBuffer.order(ByteOrder.nativeOrder())

            donorFeatures.forEach { feature ->
                inputBuffer.putFloat(feature)
            }

            // Prepare output buffer
            val outputBuffer = ByteBuffer.allocateDirect(4) // Assuming single integer output
            outputBuffer.order(ByteOrder.nativeOrder())

            // Run inference
            interpreter?.run(inputBuffer, outputBuffer)

            outputBuffer.rewind()
            val clusterResult = outputBuffer.getInt(0)

            Log.d(TAG, "Clustering result: $clusterResult")
            return clusterResult
        } catch (e: Exception) {
            Log.e(TAG, "Error during inference: ${e.message}")
            e.printStackTrace()
            return -1
        }
    }
}

// Extension function to extract features from donor data for clustering
fun DonorData.extractFeaturesForClustering(): List<Float> {
    val features = mutableListOf<Float>()

    // Blood type conversion (simple one-hot encoding)
    when (bloodType.trim().uppercase(Locale.getDefault())) {
        "A+" -> features.addAll(listOf(1f, 0f, 0f, 0f, 0f, 0f, 0f, 0f))
        "A-" -> features.addAll(listOf(0f, 1f, 0f, 0f, 0f, 0f, 0f, 0f))
        "B+" -> features.addAll(listOf(0f, 0f, 1f, 0f, 0f, 0f, 0f, 0f))
        "B-" -> features.addAll(listOf(0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f))
        "AB+" -> features.addAll(listOf(0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f))
        "AB-" -> features.addAll(listOf(0f, 0f, 0f, 0f, 0f, 1f, 0f, 0f))
        "O+" -> features.addAll(listOf(0f, 0f, 0f, 0f, 0f, 0f, 1f, 0f))
        "O-" -> features.addAll(listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 1f))
        else -> features.addAll(listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)) // Unknown blood type
    }

    // Organ type conversion (simple one-hot encoding)
    val organType = when (organDonating.trim().lowercase(Locale.getDefault())) {
        "kidney" -> listOf(1f, 0f, 0f, 0f, 0f)
        "liver" -> listOf(0f, 1f, 0f, 0f, 0f)
        "heart" -> listOf(0f, 0f, 1f, 0f, 0f)
        "lung" -> listOf(0f, 0f, 0f, 1f, 0f)
        "cornea" -> listOf(0f, 0f, 0f, 0f, 1f)
        else -> listOf(0f, 0f, 0f, 0f, 0f) // Unknown organ
    }
    features.addAll(organType)

    // Location coordinates if available
    features.add(latitude?.toFloat() ?: 0f)
    features.add(longitude?.toFloat() ?: 0f)

    return features
}