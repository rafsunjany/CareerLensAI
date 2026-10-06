package com.careerlens.ai

import java.util.Locale

data class MatchResult(
    val score: Int,
    val matchedSkills: List<String>,
    val missingSkills: List<String>,
    val totalRequiredSkillsFound: Int
)

object ResumeMatcher {

    // Common tech & industry keywords to extract and compare
    private val SKILLS_TAXONOMY = listOf(
        "kotlin", "java", "python", "javascript", "typescript", "c++", "c#", "go", "swift",
        "android", "ios", "react", "node.js", "django", "spring boot", "flutter",
        "sql", "mysql", "postgresql", "mongodb", "firebase", "sqlite",
        "git", "github", "docker", "kubernetes", "aws", "gcp", "azure", "ci/cd",
        "rest api", "graphql", "agile", "scrum", "unit testing", "oop", "mvc", "mvvm",
        "html", "css", "machine learning", "deep learning", "nlp", "pandas", "numpy"
    )

    fun calculateMatch(resumeText: String, jobDescription: String): MatchResult {
        val resumeTokens = tokenize(resumeText)
        val jobTokens = tokenize(jobDescription)

        // Detect which known skills are required by the job posting
        val requiredSkillsInJob = SKILLS_TAXONOMY.filter { skill ->
            containsTerm(jobTokens, jobDescription.lowercase(Locale.ROOT), skill)
        }

        // If the job didn't match known taxonomy keywords, extract top recurring terms
        val evaluatedSkills = if (requiredSkillsInJob.isNotEmpty()) {
            requiredSkillsInJob
        } else {
            extractFallbackTerms(jobTokens)
        }

        val matched = evaluatedSkills.filter { skill ->
            containsTerm(resumeTokens, resumeText.lowercase(Locale.ROOT), skill)
        }

        val missing = evaluatedSkills.filter { !matched.contains(it) }

        val score = if (evaluatedSkills.isNotEmpty()) {
            ((matched.size.toDouble() / evaluatedSkills.size.toDouble()) * 100).toInt().coerceIn(0, 100)
        } else {
            50 // Neutral base if no clear keywords extracted
        }

        return MatchResult(
            score = score,
            matchedSkills = matched.sorted(),
            missingSkills = missing.sorted(),
            totalRequiredSkillsFound = evaluatedSkills.size
        )
    }

    private fun containsTerm(tokens: Set<String>, fullLowerText: String, term: String): Boolean {
        return if (term.contains(" ") || term.contains(".") || term.contains("/")) {
            fullLowerText.contains(term)
        } else {
            tokens.contains(term)
        }
    }

    private fun tokenize(text: String): Set<String> {
        return text.lowercase(Locale.ROOT)
            .split(Regex("[^a-zA-Z0-9#+.]"))
            .filter { it.length > 1 }
            .toSet()
    }

    private fun extractFallbackTerms(jobTokens: Set<String>): List<String> {
        val stopWords = setOf("with", "have", "this", "that", "from", "will", "your", "must", "work", "team")
        return jobTokens.filter { it.length > 3 && !stopWords.contains(it) }.take(8)
    }
}
