package com.example.last.utils

import android.content.Context
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.min

/**
 * Utility class for TensorFlow Lite model operations
 */
class SentimentAnalyzer(private val context: Context) {
    private val maxLength = 100 // Maximum sequence length for input
    private var interpreter: Interpreter? = null

    init {
        try {
            val modelFile = FileUtil.loadMappedFile(context, "model.tflite")
            interpreter = Interpreter(modelFile)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Classifies the sentiment of the given text using the TFLite model
     *
     * @param text The review text to classify
     * @return "Positive" or "Negative" sentiment classification
     */
    fun classifySentiment(text: String): String {
        if (text.isEmpty()) return "Neutral"

        try {
            // Preprocess the text
            val inputBuffer = preProcessText(text)

            // Set up the input tensor
            val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, maxLength), org.tensorflow.lite.DataType.FLOAT32)
            inputFeature0.loadBuffer(inputBuffer)

            // Set up the output tensor
            val outputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, 2), org.tensorflow.lite.DataType.FLOAT32)

            // Run inference
            interpreter?.run(inputFeature0.buffer, outputFeature0.buffer)

            // Process the result
            val probabilities = outputFeature0.floatArray
            return if (probabilities[0] > probabilities[1]) "Negative" else "Positive"
        } catch (e: Exception) {
            e.printStackTrace()
            return fallbackClassification(text)
        }
    }

    /**
     * Fallback classification method when the model fails
     */
    private fun fallbackClassification(text: String): String {
        val lowercaseText = text.lowercase()

        // Simple sentiment analysis based on keyword presence
        val positiveWords = listOf("good", "great", "excellent", "wonderful", "amazing", "love", "helpful",
            "satisfied", "best", "recommend", "happy", "professional", "perfect")

        val negativeWords = listOf("bad", "poor", "terrible", "horrible", "awful", "worst", "rude",
            "disappointed", "disappointing", "inefficient", "slow", "unprofessional")

        var positiveScore = 0
        var negativeScore = 0

        // Count positive and negative words
        for (word in positiveWords) {
            if (lowercaseText.contains(word)) {
                positiveScore++
            }
        }

        for (word in negativeWords) {
            if (lowercaseText.contains(word)) {
                negativeScore++
            }
        }

        // Determine sentiment based on scores
        return when {
            positiveScore > negativeScore -> "Positive"
            negativeScore > positiveScore -> "Negative"
            else -> {
                // If tied, use rating as heuristic (if star rating >= 3.5, consider positive)
                if (text.length > 20) "Positive" else "Negative"
            }
        }
    }

    /**
     * Preprocesses text for input to the TFLite model
     */
    private fun preProcessText(text: String): ByteBuffer {
        // Create a buffer to hold the preprocessed text
        val buffer = ByteBuffer.allocateDirect(4 * maxLength)
        buffer.order(ByteOrder.nativeOrder())

        // Simple text preprocessing - just split by spaces and convert to lowercase
        val words = text.lowercase().trim().split("\\s+".toRegex())

        // Fill the buffer with word indices (simplified)
        for (i in 0 until min(words.size, maxLength)) {
            // Here we use a simple hash function as a placeholder for actual word embeddings
            // In a real implementation, you would use a proper tokenizer with vocabulary
            val wordValue = (words[i].hashCode() % 10000) / 10000f
            buffer.putFloat(wordValue)
        }

        // Pad with zeros if needed
        for (i in words.size until maxLength) {
            buffer.putFloat(0f)
        }

        buffer.rewind()
        return buffer
    }

    /**
     * Close the interpreter when no longer needed
     */
    fun close() {
        interpreter?.close()
    }
}