package org.caexplorer.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.caexplorer.domain.rule.Rule
import org.caexplorer.domain.rule.RuleCategory
import kotlin.random.Random

/**
 * A Material 3 bottom sheet / dialog for selecting CA rules.
 * Groups rules by category with search, category filters, and descriptions.
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
    var selectedCategory by remember { mutableStateOf<RuleCategory?>(null) }

    val availableCategories = remember(rules) {
        rules.map { it.category }.distinct().sortedBy { it.displayName }
    }
    val categoryCounts = remember(rules) {
        rules.groupBy { it.category }.mapValues { it.value.size }
    }

    val filteredRules = remember(rules, searchQuery, selectedCategory) {
        rules.filter { rule ->
            val matchesSearch = searchQuery.isBlank() ||
                    rule.displayName.contains(searchQuery, ignoreCase = true) ||
                    rule.description.contains(searchQuery, ignoreCase = true)
            val matchesCategory = selectedCategory == null || rule.category == selectedCategory
            matchesSearch && matchesCategory
        }
    }

    val groupedRules = remember(filteredRules) {
        filteredRules.groupBy { it.category }
    }

    // Non-elementary rules for random selection
    val nonElementaryRules = remember(rules) {
        rules.filter { it.category != RuleCategory.ELEMENTARY }
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
            // Title row with Random button
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Select Rule",
                    style = MaterialTheme.typography.headlineSmall
                )
                FilledTonalButton(
                    onClick = {
                        if (nonElementaryRules.isNotEmpty()) {
                            val randomRule = nonElementaryRules[Random.nextInt(nonElementaryRules.size)]
                            onRuleSelected(randomRule)
                            onDismiss()
                        }
                    }
                ) {
                    Icon(
                        Icons.Default.Casino,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Random Rule")
                }
            }

            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search rules...") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            )

            // Category filter chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { selectedCategory = null },
                    label = { Text("All") }
                )
                availableCategories.forEach { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = {
                            selectedCategory = if (selectedCategory == category) null else category
                        },
                        label = {
                            Text("${category.displayName} (${categoryCounts[category] ?: 0})")
                        }
                    )
                }
            }

            // Rule list
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                groupedRules.forEach { (category, categoryRules) ->
                    item {
                        CategoryHeader(
                            category = category,
                            ruleCount = categoryRules.size
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
private fun CategoryHeader(
    category: RuleCategory,
    ruleCount: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            category.displayName,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
        Badge(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ) {
            Text(
                ruleCount.toString(),
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
    HorizontalDivider(
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
        modifier = Modifier.padding(bottom = 4.dp)
    )
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
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
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
