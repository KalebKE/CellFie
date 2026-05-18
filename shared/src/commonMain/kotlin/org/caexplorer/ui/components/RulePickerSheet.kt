package org.caexplorer.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.caexplorer.domain.rule.Rule
import org.caexplorer.domain.rule.RuleCategory

/**
 * A Material 3 bottom sheet / dialog for selecting CA rules.
 * Groups rules by category with search and descriptions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RulePickerSheet(
    rules: List<Rule>,
    selectedRule: Rule?,
    onRuleSelected: (Rule) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredRules = remember(rules, searchQuery) {
        if (searchQuery.isBlank()) rules
        else rules.filter {
            it.displayName.contains(searchQuery, ignoreCase = true) ||
                    it.description.contains(searchQuery, ignoreCase = true)
        }
    }

    val groupedRules = remember(filteredRules) {
        filteredRules.groupBy { it.category }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            // Title
            Text(
                "Select Rule",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search rules...") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            )

            // Rule list
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                groupedRules.forEach { (category, categoryRules) ->
                    item {
                        Text(
                            category.displayName,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    items(categoryRules) { rule ->
                        RuleListItem(
                            rule = rule,
                            isSelected = rule == selectedRule,
                            onClick = {
                                onRuleSelected(rule)
                                onDismiss()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RuleListItem(
    rule: Rule,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = {
            Text(
                rule.displayName,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Text(
                rule.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        },
        trailingContent = {
            if (isSelected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        },
        modifier = Modifier.clickable(onClick = onClick),
        colors = ListItemDefaults.colors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.surface
        )
    )
}
