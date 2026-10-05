package com.careerlens.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            CareerLensAI()
        }
    }
}

@Composable
fun CareerLensAI() {

    MaterialTheme {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),

            horizontalAlignment = Alignment.CenterHorizontally,

            verticalArrangement = Arrangement.Center
        ) {

            Text(
                text = "CareerLens AI",
                style = MaterialTheme.typography.headlineLarge
            )

            Text(
                text = "AI-Powered Resume Analyzer",
                modifier = Modifier.padding(top = 8.dp)
            )

            Button(
                onClick = {
                    // Resume upload will be added later
                },
                modifier = Modifier.padding(top = 24.dp)
            ) {

                Text("Upload Resume")

            }
        }
    }
}
