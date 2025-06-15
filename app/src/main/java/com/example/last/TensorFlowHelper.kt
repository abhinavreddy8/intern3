package com.example.last.utils

import android.content.Context
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class TensorFlowHelper {
    companion object {
        private var interpreter: Interpreter? = null

        fun loadModelFromAsset(context: Context, modelPath: String = "cluster_model.tflite"): Interpreter? {
            try {
                val assetManager = context.assets
                val modelFile = assetManager.openFd(modelPath)
                val fileDescriptor = modelFile.fileDescriptor
                val startOffset = modelFile.startOffset
                val declaredLength = modelFile.declaredLength
                val fileChannel = FileInputStream(fileDescriptor).channel
                val mappedByteBuffer = fileChannel.map(
                    FileChannel.MapMode.READ_ONLY,
                    startOffset,
                    declaredLength
                )

                interpreter = Interpreter(mappedByteBuffer)
                return interpreter
            } catch (e: Exception) {
                e.printStackTrace()
                return null
            }
        }

        /**
         * Predicts the cluster for a given location point
         * @param latitude The latitude
         * @param longitude The longitude
         * @return The predicted cluster (0-based index)
         */
        fun predictCluster(latitude: Double, longitude: Double): Int {
            if (interpreter == null) {
                return -1 // Model not loaded
            }

            // Prepare input data
            val inputArray = floatArrayOf(latitude.toFloat(), longitude.toFloat())
            val inputBuffer = ByteBuffer.allocateDirect(4 * 2) // 4 bytes per float, 2 values
            inputBuffer.order(ByteOrder.nativeOrder())
            inputBuffer.putFloat(latitude.toFloat())
            inputBuffer.putFloat(longitude.toFloat())
            inputBuffer.rewind()

            // Prepare output array
            val outputBuffer = ByteBuffer.allocateDirect(4) // 4 bytes for single float output
            outputBuffer.order(ByteOrder.nativeOrder())

            // Run inference
            interpreter?.run(inputBuffer, outputBuffer)

            // Get result
            outputBuffer.rewind()
            val result = outputBuffer.float.toInt()

            return result
        }

        /**
         * Calculate similarity score between two locations
         * This can be used to rank nearby donors
         */
        fun calculateSimilarity(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
            val cluster1 = predictCluster(lat1, lon1)
            val cluster2 = predictCluster(lat2, lon2)

            // Same cluster = higher similarity
            return if (cluster1 == cluster2) 1.0f else 0.5f
        }

        fun closeInterpreter() {
            interpreter?.close()
            interpreter = null
        }
    }
}