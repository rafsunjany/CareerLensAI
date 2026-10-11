package com.careerlens.ai

import android.content.res.ColorStateList
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.progressindicator.CircularProgressIndicator

class ReportActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report)

        val score = intent.getIntExtra("EXTRA_SCORE", 0)
        val matchedSkills = intent.getStringArrayListExtra("EXTRA_MATCHED") ?: arrayListOf()
        val missingSkills = intent.getStringArrayListExtra("EXTRA_MISSING") ?: arrayListOf()
        val hasEmailOrPhone = intent.getBooleanExtra("EXTRA_HAS_CONTACT", true)
        val hasMetrics = intent.getBooleanExtra("EXTRA_HAS_METRICS", false)

        val tvScoreNumber: TextView = findViewById(R.id.tvScoreNumber)
        val tvMatchLabel: TextView = findViewById(R.id.tvMatchLabel)
        val tvCirclePercent: TextView = findViewById(R.id.tvCirclePercent)
        val progressCircle: CircularProgressIndicator = findViewById(R.id.progressCircle)
        val chipGroup: ChipGroup = findViewById(R.id.chipGroupSkills)
        val badgeContact: TextView = findViewById(R.id.badgeContact)
        val badgeAchievements: TextView = findViewById(R.id.badgeAchievements)
        val badgeAts: TextView = findViewById(R.id.badgeAts)
        val tvRecommendations: TextView = findViewById(R.id.tvRecommendations)
        val btnBack: Button = findViewById(R.id.btnBack)

        // Set Scores
        tvScoreNumber.text = "$score%"
        tvCirclePercent.text = "$score%"
        progressCircle.progress = score

        tvMatchLabel.text = when {
            score >= 70 -> "High match • Strong alignment"
            score >= 40 -> "Moderate match • Needs tuning"
            else -> "Low match • Key requirements missing"
        }

        // Add Green Matched Chips
        matchedSkills.forEach { skill ->
            val chip = Chip(this).apply {
                text = "✓ $skill"
                chipBackgroundColor = ColorStateList.valueOf(ContextCompat.getColor(this@ReportActivity, R.color.badge_green_bg))
                setTextColor(ContextCompat.getColor(this@ReportActivity, R.color.badge_green_text))
                chipStrokeWidth = 0f
                isClickable = false
            }
            chipGroup.addView(chip)
        }

        // Add Red Missing Chips
        missingSkills.forEach { skill ->
            val chip = Chip(this).apply {
                text = "✕ $skill"
                chipBackgroundColor = ColorStateList.valueOf(ContextCompat.getColor(this@ReportActivity, R.color.badge_red_bg))
                setTextColor(ContextCompat.getColor(this@ReportActivity, R.color.badge_red_text))
                chipStrokeWidth = 0f
                isClickable = false
            }
            chipGroup.addView(chip)
        }

        // Resume Quality Statuses
        if (hasEmailOrPhone) {
            badgeContact.text = "Found"
            badgeContact.setBackgroundResource(R.drawable.badge_bg_green)
            badgeContact.setTextColor(ContextCompat.getColor(this, R.color.badge_green_text))
        } else {
            badgeContact.text = "Missing"
            badgeContact.setBackgroundResource(R.drawable.badge_bg_red)
            badgeContact.setTextColor(ContextCompat.getColor(this, R.color.badge_red_text))
        }

        if (hasMetrics) {
            badgeAchievements.text = "Good"
            badgeAchievements.setBackgroundResource(R.drawable.badge_bg_green)
            badgeAchievements.setTextColor(ContextCompat.getColor(this, R.color.badge_green_text))
        } else {
            badgeAchievements.text = "Needs review"
            badgeAchievements.setBackgroundResource(R.drawable.badge_bg_orange)
            badgeAchievements.setTextColor(ContextCompat.getColor(this, R.color.badge_orange_text))
        }

        if (missingSkills.isEmpty()) {
            badgeAts.text = "Optimal"
            badgeAts.setBackgroundResource(R.drawable.badge_bg_green)
            badgeAts.setTextColor(ContextCompat.getColor(this, R.color.badge_green_text))
        } else {
            badgeAts.text = "Some missing"
            badgeAts.setBackgroundResource(R.drawable.badge_bg_orange)
            badgeAts.setTextColor(ContextCompat.getColor(this, R.color.badge_orange_text))
        }

        // Dynamic Recommendations
        tvRecommendations.text = buildString {
            append("1. Add relevant projects that clearly demonstrate the requested skills.\n\n")
            append("2. Improve experience bullet points with specific, quantified outcomes (e.g., percentages, scale, speed).\n\n")
            if (missingSkills.isNotEmpty()) {
                val topMissing = missingSkills.take(3).joinToString(", ")
                append("3. Incorporate relevant job keywords like ($topMissing) into your resume where truthful.")
            } else {
                append("3. Highlight your leadership or problem-solving approaches to stand out.")
            }
        }

        btnBack.setOnClickListener {
            finish()
        }
    }
}
