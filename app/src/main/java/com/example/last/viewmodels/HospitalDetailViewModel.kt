package com.example.last.viewmodels

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID
import java.util.regex.Pattern

class HospitalDetailViewModel : ViewModel() {
    var hospital by mutableStateOf<HospitalData?>(null)
    var isLoading by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)
    var reviews by mutableStateOf<List<ReviewData>>(emptyList())
    var positiveReviews by mutableStateOf<List<ReviewData>>(emptyList())
    var negativeReviews by mutableStateOf<List<ReviewData>>(emptyList())

    private val database = FirebaseDatabase.getInstance().reference
    private val auth = FirebaseAuth.getInstance()

    // Vocabulary map for sentiment analysis
    private var vocabularyMap: Map<String, Int> = emptyMap()
    private val maxSequenceLength = 100
    private val maxWords = 5000 // Match training vocabulary size
    private val stopWords: Set<String> by lazy { loadStopWords() }

    // Compile regex pattern once
    private val nonAlphaPattern = Pattern.compile("[^a-zA-Z\\s]")

    fun fetchHospitalDetails(hospitalId: String) {
        isLoading = true
        error = null

        database.child("hospital").child(hospitalId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val id = snapshot.key ?: ""
                    val name = snapshot.child("name").getValue(String::class.java) ?: ""
                    val address = snapshot.child("address").getValue(String::class.java) ?: ""
                    val contact = snapshot.child("phoneNumber").getValue(String::class.java) ?: ""
                    val imageUrl = snapshot.child("logoUrl").getValue(String::class.java) ?: ""
                    val specialties = snapshot.child("specializations").getValue(String::class.java) ?: ""

                    hospital = HospitalData(id, name, address, contact, imageUrl, specialties)
                    isLoading = false

                    // Fetch reviews after hospital details are loaded
                    fetchHospitalReviews(id)
                } else {
                    error = "Hospital not found"
                    isLoading = false
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                error = databaseError.message
                isLoading = false
            }
        })
    }

    fun sendHospitalRequest(description: String) {
        val currentUser = auth.currentUser ?: return
        val requestId = UUID.randomUUID().toString()

        val request = mapOf(
            "requestId" to requestId,
            "recipientId" to currentUser.uid,
            "recipientName" to (currentUser.displayName ?: "Unknown User"),
            "hospitalId" to (hospital?.id ?: ""),
            "hospitalName" to (hospital?.name ?: ""),
            "description" to description,
            "status" to "pending",
            "timestamp" to System.currentTimeMillis()
        )

        // Save in hospital's requests node
        database.child("hospital_requests").child(hospital?.id ?: "").child(requestId).setValue(request)

        // Save in recipient's requests node for tracking
        database.child("recipient_requests").child(currentUser.uid).child(requestId).setValue(request)
    }

    fun addReview(rating: Float, comment: String) {
        val currentUser = auth.currentUser ?: return
        val reviewId = UUID.randomUUID().toString()

        val review = mapOf(
            "userId" to currentUser.uid,
            "userName" to (currentUser.displayName ?: "Anonymous"),
            "rating" to rating,
            "comment" to comment,
            "timestamp" to System.currentTimeMillis()
        )

        database.child("hospital_reviews").child(hospital?.id ?: "").child(reviewId).setValue(review)
            .addOnSuccessListener {
                // Refresh reviews
                fetchHospitalReviews(hospital?.id ?: "")
            }
    }

    fun deleteReview(reviewId: String) {
        val currentUser = auth.currentUser ?: return

        // Check if the review belongs to the current user
        database.child("hospital_reviews").child(hospital?.id ?: "").child(reviewId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val userId = snapshot.child("userId").getValue(String::class.java)

                    if (userId == currentUser.uid) {
                        // Delete the review
                        database.child("hospital_reviews").child(hospital?.id ?: "").child(reviewId)
                            .removeValue()
                            .addOnSuccessListener {
                                // Refresh reviews
                                fetchHospitalReviews(hospital?.id ?: "")
                            }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                }
            })
    }

    private fun fetchHospitalReviews(hospitalId: String) {
        database.child("hospital_reviews").child(hospitalId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val reviewsList = mutableListOf<ReviewData>()

                    for (reviewSnapshot in snapshot.children) {
                        val reviewId = reviewSnapshot.key ?: ""
                        val userId = reviewSnapshot.child("userId").getValue(String::class.java) ?: ""
                        val userName = reviewSnapshot.child("userName").getValue(String::class.java) ?: ""
                        val rating = reviewSnapshot.child("rating").getValue(Float::class.java) ?: 0f
                        val comment = reviewSnapshot.child("comment").getValue(String::class.java) ?: ""
                        val timestamp = reviewSnapshot.child("timestamp").getValue(Long::class.java) ?: 0L

                        val review = ReviewData(
                            id = reviewId,
                            userId = userId,
                            userName = userName,
                            rating = rating,
                            comment = comment,
                            timestamp = timestamp
                        )

                        reviewsList.add(review)
                    }

                    reviews = reviewsList
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                }
            })
    }

    fun classifyReviews() {
        val positiveList = mutableListOf<ReviewData>()
        val negativeList = mutableListOf<ReviewData>()

        for (review in reviews) {
            // Classify based on rating (assuming 1-5 scale)
            when {
                review.rating >= 3 -> positiveList.add(review) // 4-5 stars are positive
                review.rating < 3 -> negativeList.add(review)  // 1-2 stars are negative
                // 3 stars could be considered neutral and excluded or added to negative
                // Here I'm adding them to negative for your use case
                else -> negativeList.add(review)
            }
        }

        positiveReviews = positiveList
        negativeReviews = negativeList
    }

    private fun loadVocabulary(context: Context) {
        try {
            val vocabMap = mutableMapOf<String, Int>()
            context.assets.open("vocabulary.txt").use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        line?.trim()?.let { word ->
                            if (word.isNotEmpty()) {
                                // Assign index based on tokenizer.word_index (1-based indexing)
                                val index = vocabMap.size + 1
                                vocabMap[word] = index
                            }
                        }
                    }
                }
            }
            vocabularyMap = vocabMap
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback vocabulary (minimal to avoid crashes)
            vocabularyMap = mapOf(
                "good" to 1, "bad" to 2, "great" to 3, "terrible" to 4,
                "excellent" to 5, "poor" to 6, "amazing" to 7, "awful" to 8
            )
        }
    }

    private fun loadStopWords(): Set<String> {
        // Load a basic set of stopwords (since NLTK isn't available in Android)
        return setOf(
            "a", "an", "and", "are", "as", "at", "be", "by", "for", "from", "has",
            "he", "in", "is", "it", "its", "of", "on", "that", "the", "to", "was",
            "were", "will", "with"
        )
    }

    private fun classifyText(context: Context, text: String): FloatArray {
        try {
            // Load model
            val model = com.example.last.ml.Model.newInstance(context)

            // Preprocess text to match training pipeline
            val sequence = preprocessText(text)

            // Create input buffer (integers for tokenized sequence)
            val byteBuffer = ByteBuffer.allocateDirect(4 * maxSequenceLength) // 4 bytes per int
            byteBuffer.order(ByteOrder.nativeOrder())
            for (value in sequence) {
                byteBuffer.putInt(value)
            }

            // Create input tensor
            val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, maxSequenceLength), DataType.UINT8)
            inputFeature0.loadBuffer(byteBuffer)

            // Run inference
            val outputs = model.process(inputFeature0)
            val outputFeature0 = outputs.outputFeature0AsTensorBuffer
            val sentiment = outputFeature0.floatArray

            println("Review text: ${text.take(30)}... - Sentiment scores: ${sentiment.joinToString()}")

            model.close()
            return sentiment
        } catch (e: Exception) {
            e.printStackTrace()
            return floatArrayOf(0.33f, 0.33f, 0.33f) // Neutral fallback on error
        }
    }

    private fun preprocessText(text: String): IntArray {
        // Clean text to match training pipeline
        var cleanedText = text.lowercase()
        cleanedText = nonAlphaPattern.matcher(cleanedText).replaceAll("")
        val tokens = cleanedText.split("\\s+".toRegex()).filter { it.isNotEmpty() && it !in stopWords }

        // Convert to sequence
        val sequence = mutableListOf<Int>()
        for (token in tokens) {
            val index = vocabularyMap[token] ?: 0 // 0 for unknown words
            if (index < maxWords) { // Respect max_words from training
                sequence.add(index)
            }
        }

        // Pad sequence
        val padded = IntArray(maxSequenceLength) { 0 }
        val length = minOf(sequence.size, maxSequenceLength)
        for (i in 0 until length) {
            padded[i] = sequence[i]
        }

        return padded
    }

    // Helper function to find index of max value in FloatArray
    private fun FloatArray.indexOfMax(): Int {
        var maxIndex = 0
        var maxValue = this[0]
        for (i in 1 until size) {
            if (this[i] > maxValue) {
                maxValue = this[i]
                maxIndex = i
            }
        }
        return maxIndex
    }
}



data class ReviewData(
    val id: String,
    val userId: String,
    val userName: String,
    val rating: Float,
    val comment: String,
    val timestamp: Long
)