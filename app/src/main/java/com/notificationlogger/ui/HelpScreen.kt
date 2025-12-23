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
import dev.jeziellago.compose.markdowntext.MarkdownText
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
 * Custom markdown renderer that handles drawable image references.
 * Processes markdown and replaces image references like ![alt](drawable:help_placeholder) 
 * with Compose Image composables, then renders the rest with MarkdownText.
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
            val line = lines[i]
            
            // Handle images with drawable: scheme
            val imageMatch = Regex("!\\[.*?\\]\\(drawable:([\\w_]+)\\)").find(line)
            if (imageMatch != null) {
                val drawableName = imageMatch.groupValues[1]
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
                i++
                continue
            }
            
            // For other content, collect lines until next image or end
            val contentLines = mutableListOf<String>()
            while (i < lines.size) {
                val currentLine = lines[i]
                if (Regex("!\\[.*?\\]\\(drawable:([\\w_]+)\\)").containsMatchIn(currentLine)) {
                    break
                }
                contentLines.add(currentLine)
                i++
            }
            
            // Render collected content as markdown
            if (contentLines.isNotEmpty()) {
                val content = contentLines.joinToString("\n")
                if (content.trim().isNotEmpty()) {
                    MarkdownText(
                        markdown = content,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}

