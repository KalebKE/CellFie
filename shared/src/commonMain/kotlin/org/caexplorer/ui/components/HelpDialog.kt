package org.caexplorer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.caexplorer.ui.help.HelpContent
import org.caexplorer.ui.help.HelpTopic

/**
 * Full-screen help dialog with side navigation and scrollable content area.
 */
@Composable
fun HelpDialog(onDismiss: () -> Unit) {
    val topics = HelpContent.topics
    var selectedIndex by remember { mutableStateOf(0) }
    val selectedTopic = topics[selectedIndex]

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                HelpHeader(onDismiss = onDismiss)

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Body: sidebar + content
                Row(modifier = Modifier.fillMaxSize().weight(1f)) {
                    // Left sidebar
                    HelpSidebar(
                        topics = topics,
                        selectedIndex = selectedIndex,
                        onSelect = { selectedIndex = it },
                        modifier = Modifier.width(220.dp).fillMaxHeight()
                    )

                    VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Right content
                    HelpContentArea(
                        topic = selectedTopic,
                        modifier = Modifier.weight(1f).fillMaxHeight()
                    )
                }
            }
        }
    }
}

@Composable
private fun HelpHeader(onDismiss: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "User Guide",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        IconButton(onClick = onDismiss) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun HelpSidebar(
    topics: List<HelpTopic>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .verticalScroll(rememberScrollState())
            .padding(vertical = 8.dp)
    ) {
        topics.forEachIndexed { index, topic ->
            val isSelected = index == selectedIndex
            val bgColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            }
            val textColor = if (isSelected) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 2.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(bgColor)
                    .clickable { onSelect(index) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = topic.icon,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(end = 10.dp)
                )
                Text(
                    text = topic.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = textColor
                )
            }
        }
    }
}

@Composable
private fun HelpContentArea(
    topic: HelpTopic,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    // Reset scroll when topic changes
    LaunchedEffect(topic.title) {
        scrollState.scrollTo(0)
    }

    Column(
        modifier = modifier
            .verticalScroll(scrollState)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Topic header
        Text(
            text = "${topic.icon}  ${topic.title}",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(Modifier.height(4.dp))

        // Sections
        topic.sections.forEach { section ->
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.outlinedCardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                border = CardDefaults.outlinedCardBorder().let {
                    androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = section.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = formatHelpText(section.content),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

/**
 * Parses simple markdown-like formatting: **bold** markers become bold spans.
 */
private fun formatHelpText(text: String): AnnotatedString {
    return buildAnnotatedString {
        var remaining = text
        while (remaining.isNotEmpty()) {
            val boldStart = remaining.indexOf("**")
            if (boldStart == -1) {
                append(remaining)
                break
            }
            // Append text before the bold marker
            append(remaining.substring(0, boldStart))
            remaining = remaining.substring(boldStart + 2)

            val boldEnd = remaining.indexOf("**")
            if (boldEnd == -1) {
                // No closing marker — append rest as-is
                append("**")
                append(remaining)
                break
            }
            // Append bold text
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                append(remaining.substring(0, boldEnd))
            }
            remaining = remaining.substring(boldEnd + 2)
        }
    }
}
