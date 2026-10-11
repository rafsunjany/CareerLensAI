package com.careerlens.ai

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.ArrayList

class MainActivity : AppCompatActivity() {

    private var selectedPdfUri: Uri? = null
    private var selectedPdfName: String? = null
    private var extractedResumeText: String = ""

    private lateinit var tvResumeStatus: TextView
    private lateinit var etJobDescription: TextInputEditText
    private lateinit var btnAnalyze: Button

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

            // Perform matching
            val result = ResumeMatcher.calculateMatch(extractedResumeText, jobDesc)

            // Check contact details (email or phone)
            val hasContact = extractedResumeText.contains("@") || Regex("""\+?\d[\d\s-]{8,}\d""").containsMatchIn(extractedResumeText)

            // Check measurable achievement keywords (%, $, increased, decreased, saved, led)
            val hasMetrics = Regex("""\b(\d+%|\$\d+|increased|improved|reduced|led|built)\b""", RegexOption.IGNORE_CASE).containsMatchIn(extractedResumeText)

            // Open Report Activity
            val intent = Intent(this, ReportActivity::class.java).apply {
                putExtra("EXTRA_SCORE", result.score)
                putStringArrayListExtra("EXTRA_MATCHED", ArrayList(result.matchedSkills))
                putStringArrayListExtra("EXTRA_MISSING", ArrayList(result.missingSkills))
                putExtra("EXTRA_HAS_CONTACT", hasContact)
                putExtra("EXTRA_HAS_METRICS", hasMetrics)
            }
            startActivity(intent)
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
