package org.screenlite.sdm

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class SetupActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SetupScreen(
                onSetupComplete = { serverUrl, screenName ->
                    saveConfig(serverUrl, screenName)
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
            )
        }
    }

    private fun saveConfig(serverUrl: String, screenName: String) {
        val prefs = getSharedPreferences("screenlite_provisioning", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString("screenlite_server_url", serverUrl.trimEnd('/'))
            putString("screen_name", screenName)
            putBoolean("setup_complete", true)
            apply()
        }
    }
}

@Composable
fun SetupScreen(onSetupComplete: (String, String) -> Unit) {
    var serverUrl by remember { mutableStateOf("") }
    var screenName by remember { mutableStateOf("") }
    var urlError by remember { mutableStateOf(false) }
    var nameError by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Screenlite Setup",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Configure this screen to connect to your Screenlite server.",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(40.dp))

            OutlinedTextField(
                value = serverUrl,
                onValueChange = {
                    serverUrl = it
                    urlError = false
                },
                label = { Text("Server URL") },
                placeholder = { Text("http://192.168.1.100:3005") },
                isError = urlError,
                supportingText = {
                    if (urlError) Text("Please enter a valid server URL")
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = screenName,
                onValueChange = {
                    screenName = it
                    nameError = false
                },
                label = { Text("Screen Name") },
                placeholder = { Text("Reception") },
                isError = nameError,
                supportingText = {
                    if (nameError) Text("Please enter a screen name")
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    urlError = serverUrl.isBlank() || !serverUrl.startsWith("http")
                    nameError = screenName.isBlank()
                    if (!urlError && !nameError) {
                        onSetupComplete(serverUrl.trim(), screenName.trim())
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text("Connect", fontSize = 16.sp)
            }
        }
    }
}