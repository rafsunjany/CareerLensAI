package com.careerlens.ai

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.careerlens.ai.R
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private var selectedPdfUri: Uri? = null
    private var selectedPdfName: String? = null
    private var extractedResumeText: String = ""

    private lateinit var etApiKey: TextInputEditText
    private lateinit var tvResumeStatus: TextView
    private lateinit var etJobDescription: TextInputEditText
    private lateinit var btnAnalyze: Button
    private lateinit var cardResult: MaterialCardView
    private lateinit var tvResults: TextView

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val selectPdfLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedPdfUri = uri
            selectedPdfName = getFileName(uri)
            tvResumeStatus.text = "Reading: $selectedPdfName..."
            extractTextFromPdf(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        PDFBoxResourceLoader.init(applicationContext)

        val btnUploadResume: Button = findViewById(R.id.btnUploadResume)
        etApiKey = findViewById(R.id.etApiKey)
        btnAnalyze = findViewById(R.id.btnAnalyze)
        tvResumeStatus = findViewById(R.id.tvResumeStatus)
        etJobDescription = findViewById(R.id.etJobDescription)
        cardResult = findViewById(R.id.cardResult)
        tvResults = findViewById(R.id.tvResults)

        btnUploadResume.setOnClickListener {
            selectPdfLauncher.launch("application/pdf")
        }

        btnAnalyze.setOnClickListener {
            val apiKey = etApiKey.text.toString().trim()
            val jobDesc = etJobDescription.text.toString().trim()

            if (selectedPdfUri == null || extractedResumeText.isBlank()) {
                Toast.makeText(this, "Please select and load a resume PDF", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (jobDesc.isEmpty()) {
                Toast.makeText(this, "Please paste a job description", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Quick offline baseline match
            val localMatch = ResumeMatcher.calculateMatch(extractedResumeText, jobDesc)

            cardResult.visibility = View.VISIBLE
            val localSummary = "📊 Baseline Match: ${localMatch.score}%\n" +
                    "• Matched: ${if (localMatch.matchedSkills.isEmpty()) "None" else localMatch.matchedSkills.joinToString(", ")}\n" +
                    "• Missing: ${if (localMatch.missingSkills.isEmpty()) "None" else localMatch.missingSkills.joinToString(", ")}\n\n"

            if (apiKey.isEmpty()) {
                tvResults.text = localSummary + "💡 Note: Provide a Gemini API Key above to unlock in-depth AI suggestions and career coaching."
            } else {
                tvResults.text = localSummary + "🤖 Consulting Gemini AI for deeper insights..."
                analyzeWithGemini(apiKey, extractedResumeText, jobDesc, localSummary)
            }
        }
    }

    private fun analyzeWithGemini(apiKey: String, resume: String, job: String, localSummary: String) {
        btnAnalyze.isEnabled = false

        lifecycleScope.launch(Dispatchers.IO) {
            val prompt = """
                You are CareerLens AI, an expert tech recruiter and resume evaluator.
                Compare the following Resume and Job Description. Provide a concise, structured review with:
                1. Match Strengths (2-3 bullet points)
                2. Critical Skill Gaps & Weaknesses (2-3 bullet points)
                3. High-Impact Action Items to Improve Resume for This Role (3 bullet points)

                Resume:
                $resume

                Job Description:
                $job
            """.trimIndent()

            try {
                val jsonPayload = JSONObject().apply {
                    val contentsArray = JSONArray().apply {
                        val part = JSONObject().apply {
                            put("parts", JSONArray().apply {
                                put(JSONObject().apply {
                                    put("text", prompt)
                                })
                            })
                        }
                        put(part)
                    }
                    put("contents", contentsArray)
                }

                val body = jsonPayload.toString().toRequestBody("application/json".toMediaType())
                val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey"

                val request = Request.Builder()
                    .url(url)
                    .post(body)
                    .build()

                val response = httpClient.newCall(request).execute()
                val responseBody = response.body?.string().orEmpty()

                withContext(Dispatchers.Main) {
                    btnAnalyze.isEnabled = true
                    if (response.isSuccessful) {
                        val parsedText = parseGeminiResponse(responseBody)
                        tvResults.text = localSummary + "🤖 Gemini AI Evaluation:\n\n$parsedText"
                    } else {
                        tvResults.text = localSummary + "⚠️ Gemini API Error (${response.code}):\n$responseBody"
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    btnAnalyze.isEnabled = true
                    tvResults.text = localSummary + "⚠️ Failed to connect to Gemini: ${e.localizedMessage}"
                }
            }
        }
    }

    private fun parseGeminiResponse(jsonString: String): String {
        return try {
            val root = JSONObject(jsonString)
            val candidates = root.getJSONArray("candidates")
            val content = candidates.getJSONObject(0).getJSONObject("content")
            val parts = content.getJSONArray("parts")
            parts.getJSONObject(0).getString("text")
        } catch (e: Exception) {
            "Could not parse AI response: ${e.localizedMessage}"
        }
    }

    private fun extractTextFromPdf(uri: Uri) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    val document = PDDocument.load(inputStream)
                    val stripper = PDFTextStripper()
                    val text = stripper.getText(document)
                    document.close()

                    withContext(Dispatchers.Main) {
                        extractedResumeText = text.trim()
                        if (extractedResumeText.isNotEmpty()) {
                            tvResumeStatus.text = "Loaded: $selectedPdfName (${extractedResumeText.length} chars)"
                        } else {
                            tvResumeStatus.text = "Loaded: $selectedPdfName (Warning: Empty text or scanned image)"
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    tvResumeStatus.text = "Failed to parse PDF: ${e.localizedMessage}"
                }
            }
        }
    }

    private fun getFileName(uri: Uri): String {
        var name = "resume.pdf"
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex != -1 && cursor.moveToFirst()) {
                name = cursor.getString(nameIndex)
            }
        }
        return name
    }
}
