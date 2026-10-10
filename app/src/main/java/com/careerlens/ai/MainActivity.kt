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

class MainActivity : AppCompatActivity() {

    private var selectedPdfUri: Uri? = null
    private var selectedPdfName: String? = null
    private var extractedResumeText: String = ""

    private lateinit var tvResumeStatus: TextView
    private lateinit var etJobDescription: TextInputEditText
    private lateinit var btnAnalyze: Button
    private lateinit var cardResult: MaterialCardView
    private lateinit var tvResults: TextView

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
        btnAnalyze = findViewById(R.id.btnAnalyze)
        tvResumeStatus = findViewById(R.id.tvResumeStatus)
        etJobDescription = findViewById(R.id.etJobDescription)
        cardResult = findViewById(R.id.cardResult)
        tvResults = findViewById(R.id.tvResults)

        btnUploadResume.setOnClickListener {
            selectPdfLauncher.launch("application/pdf")
        }

        btnAnalyze.setOnClickListener {
            val jobDesc = etJobDescription.text.toString().trim()

            if (selectedPdfUri == null || extractedResumeText.isBlank()) {
                Toast.makeText(this, "Please select and load a resume PDF", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (jobDesc.isEmpty()) {
                Toast.makeText(this, "Please paste a job description", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Local Heuristic Matching Engine
            val result = ResumeMatcher.calculateMatch(extractedResumeText, jobDesc)

            cardResult.visibility = View.VISIBLE
            tvResults.text = buildString {
                append("📊 Overall Match Score: ${result.score}%\n\n")

                append("✅ Matched Skills & Keywords (${result.matchedSkills.size}):\n")
                if (result.matchedSkills.isNotEmpty()) {
                    append(result.matchedSkills.joinToString("\n") { "  • $it" })
                } else {
                    append("  • No direct matching keywords found")
                }
                append("\n\n")

                append("❌ Missing Skills / Requirements (${result.missingSkills.size}):\n")
                if (result.missingSkills.isNotEmpty()) {
                    append(result.missingSkills.joinToString("\n") { "  • $it" })
                } else {
                    append("  • No major missing requirements detected!")
                }
                append("\n\n")

                append("💡 Recommendations:\n")
                if (result.score >= 70) {
                    append("• Strong alignment! Your resume closely reflects the job requirements.\n")
                    append("• Highlight specific achievements and impact metrics for your matched skills.")
                } else if (result.score >= 40) {
                    append("• Moderate alignment. Review the missing keywords above and incorporate relevant ones into your experience or project sections.\n")
                    append("• Tailor your resume summary specifically to this role.")
                } else {
                    append("• Low alignment. Consider adding projects, certifications, or coursework covering the missing skills before applying.")
                }
            }
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
                            tvResumeStatus.text = "Loaded: $selectedPdfName (Warning: Scanned image or empty)"
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
