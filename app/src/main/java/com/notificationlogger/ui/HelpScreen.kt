package com.notificationlogger.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Help screen displaying user manual with setup and operation instructions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    var markdownContent by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    // Load markdown file from assets
    LaunchedEffect(Unit) {
        try {
            val content = withContext(Dispatchers.IO) {
                try {
                    context.assets.open("help_content.md").bufferedReader().use { it.readText() }
                } catch (e: IOException) {
                    null
                }
            }
            markdownContent = content
            isLoading = false
            if (content == null) {
                error = "Could not load help content"
            }
        } catch (e: Exception) {
            error = "Error loading help: ${e.message}"
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("User Manual") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            error != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = error ?: "Unknown error",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            markdownContent != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 16.dp)
                ) {
                    // Render markdown with custom image handler
                    MarkdownContent(
                        markdown = markdownContent!!,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

/**
 * Custom markdown renderer that handles basic markdown syntax and drawable image references.
 */
@Composable
private fun MarkdownContent(
    markdown: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lines = markdown.lines()
    
    Column(modifier = modifier) {
        var i = 0
        while (i < lines.size) {
            val line = lines[i].trim()
            
            when {
                // Handle images with drawable: scheme
                line.matches(Regex("!\\[.*?\\]\\(drawable:([\\w_]+)\\)")) -> {
                    val imageMatch = Regex("drawable:([\\w_]+)").find(line)
                    val drawableName = imageMatch?.groupValues?.get(1)
                    if (drawableName != null) {
                        val resourceId = context.resources.getIdentifier(
                            drawableName,
                            "drawable",
                            context.packageName
                        )
                        if (resourceId != 0) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Image(
                                painter = painterResource(id = resourceId),
                                contentDescription = null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                contentScale = ContentScale.Fit
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                    i++
                }
                // Handle H1 headers
                line.startsWith("# ") -> {
                    Text(
                        text = line.removePrefix("# "),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                    i++
                }
                // Handle H2 headers
                line.startsWith("## ") -> {
                    Text(
                        text = line.removePrefix("## "),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
                    i++
                }
                // Handle H3 headers
                line.startsWith("### ") -> {
                    Text(
                        text = line.removePrefix("### "),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    i++
                }
                // Handle italic text (markdown style with *)
                line.startsWith("*") && line.endsWith("*") && !line.startsWith("**") && line.length > 2 -> {
                    Text(
                        text = line.removeSurrounding("*"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    i++
                }
                // Handle numbered lists
                line.matches(Regex("^\\d+\\.\\s+.*")) -> {
                    val content = line.replaceFirst(Regex("^\\d+\\.\\s+"), "")
                    Row(
                        modifier = Modifier.padding(vertical = 2.dp, horizontal = 8.dp)
                    ) {
                        Text(
                            text = "• ",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = content,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    i++
                }
                // Handle bullet lists
                line.startsWith("- ") -> {
                    Text(
                        text = "• ${line.removePrefix("- ")}",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(vertical = 2.dp, horizontal = 8.dp)
                    )
                    i++
                }
                // Handle empty lines
                line.isBlank() -> {
                    Spacer(modifier = Modifier.height(8.dp))
                    i++
                }
                // Regular text paragraphs
                else -> {
                    // Process bold text (**text**)
                    val processedText = line.replace(Regex("\\*\\*(.*?)\\*\\*")) { matchResult ->
                        matchResult.groupValues[1]
                    }
                    Text(
                        text = processedText,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    i++
                }
            }
        }
    }
}

