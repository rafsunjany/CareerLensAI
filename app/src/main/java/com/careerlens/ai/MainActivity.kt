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
import java.io.InputStream

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
                Toast.makeText(this, "Please select and load a valid resume PDF first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (jobDesc.isEmpty()) {
                Toast.makeText(this, "Please paste a job description", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            cardResult.visibility = View.VISIBLE
            val samplePreview = if (extractedResumeText.length > 250) {
                extractedResumeText.substring(0, 250) + "..."
            } else {
                extractedResumeText
            }

            tvResults.text = "Resume: $selectedPdfName\n" +
                    "Extracted Characters: ${extractedResumeText.length}\n" +
                    "Job Description Characters: ${jobDesc.length}\n\n" +
                    "Preview of Resume Text:\n\"$samplePreview\"\n\n" +
                    "Step 3 complete! Ready for Step 4 matching."
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
                            tvResumeStatus.text = "Loaded: $selectedPdfName (Warning: No text found. Might be scanned image)"
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
