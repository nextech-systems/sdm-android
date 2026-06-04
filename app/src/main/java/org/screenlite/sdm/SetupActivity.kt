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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

class SetupActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SetupScreen(
                onSetupComplete = { serverUrl, screenName, screenId, token ->
                    saveConfig(serverUrl, screenName, screenId, token)
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
            )
        }
    }

    private fun saveConfig(serverUrl: String, screenName: String, screenId: String, token: String) {
        val prefs = getSharedPreferences("screenlite_provisioning", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putString("screenlite_server_url", serverUrl.trimEnd('/'))
            putString("screen_name", screenName)
            putString("screen_id", screenId)
            putString("player_token", token)
            putBoolean("setup_complete", true)
            commit() // synchronous — ensures data survives an immediate reboot
        }
    }
}

@Composable
fun SetupScreen(onSetupComplete: (String, String, String, String) -> Unit) {
    var serverUrl by remember { mutableStateOf("") }
    var screenName by remember { mutableStateOf("") }
    var urlError by remember { mutableStateOf(false) }
    var nameError by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var connectionCode by remember { mutableStateOf("") }
    var isPolling by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    // Poll for device connection once we have a connection code
    LaunchedEffect(isPolling) {
        if (!isPolling || connectionCode.isBlank()) return@LaunchedEffect
        while (isPolling) {
            delay(3000)
            try {
                val result = withContext(Dispatchers.IO) {
                    val client = OkHttpClient()
                    val request = Request.Builder()
                        .url("${serverUrl.trimEnd('/')}/api/devices/status/$connectionCode")
                        .get()
                        .build()
                    val response = client.newCall(request).execute()
                    val json = JSONObject(response.body.string())
                    if (json.optBoolean("connected", false)) {
                        val screenId = json.optString("screenId", "")
                        val token = json.optString("token", "")
                        if (screenId.isNotBlank() && token.isNotBlank()) {
                            Pair(screenId, token)
                        } else null
                    } else null
                }
                if (result != null) {
                    isPolling = false
                    onSetupComplete(serverUrl.trim(), screenName.trim(), result.first, result.second)
                }
            } catch (e: Exception) {
                // Network error during poll — silently retry
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (connectionCode.isNotBlank()) {
            // Waiting for CMS connection screen
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Screenlite",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Enter this code in the Screenlite CMS to connect this screen.",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(40.dp))

                Text(
                    text = "Connection Code",
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = connectionCode,
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 8.sp
                )

                Spacer(modifier = Modifier.height(40.dp))

                if (isPolling) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Waiting for CMS connection...",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    Button(
                        onClick = { isPolling = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Text("Wait for Connection", fontSize = 16.sp)
                    }
                }
            }
        } else {
            // Setup form
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(40.dp))

                OutlinedTextField(
                    value = serverUrl,
                    onValueChange = {
                        serverUrl = it
                        urlError = false
                        errorMessage = ""
                    },
                    label = { Text("Server URL") },
                    placeholder = { Text("https://screenlite.example.com") },
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
                        errorMessage = ""
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

                if (errorMessage.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 14.sp
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        urlError = serverUrl.isBlank() || !serverUrl.startsWith("http")
                        nameError = screenName.isBlank()
                        if (!urlError && !nameError) {
                            isLoading = true
                            CoroutineScope(Dispatchers.IO).launch {
                                try {
                                    val client = OkHttpClient()
                                    val body = "{}".toRequestBody("application/json".toMediaType())
                                    val request = Request.Builder()
                                        .url("${serverUrl.trimEnd('/')}/api/devices/register")
                                        .post(body)
                                        .build()
                                    val response = client.newCall(request).execute()
                                    val responseBody = response.body.string()
                                    val json = JSONObject(responseBody)
                                    val code = json.getJSONObject("device").getString("connectionCode")
                                    withContext(Dispatchers.Main) {
                                        connectionCode = code
                                        isLoading = false
                                        isPolling = true  // start polling immediately
                                    }
                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        errorMessage = "Could not connect to server: ${e.message}"
                                        isLoading = false
                                    }
                                }
                            }
                        }
                    },
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Text("Connect", fontSize = 16.sp)
                    }
                }
            }
        }
    }
}