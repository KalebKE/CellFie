package org.caexplorer.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.caexplorer.domain.rule.IntegerRule
import org.caexplorer.domain.rule.Rule
import org.caexplorer.domain.rule.RuleProperty
import org.caexplorer.domain.rule.implementations.WolframRule
import androidx.compose.ui.text.font.FontWeight
import kotlin.math.roundToInt

/**
 * Panel displaying configurable rule properties with appropriate controls.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RulePropertiesPanel(
    rule: Rule,
    onRuleChanged: (Rule) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Rule header
        OutlinedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Prominent rule number for Wolfram and OuterTotalistic rules
                when (rule) {
                    is WolframRule -> {
                        Text(
                            "Rule #${rule.ruleNumber}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    else -> {
                        Text(
                            rule.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                // Description
                if (rule.description.isNotBlank()) {
                    Text(
                        rule.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // Category badge and state count
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SuggestionChip(
                        onClick = {},
                        label = { Text(rule.category.displayName) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            labelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.height(28.dp)
                    )
                    if (rule is IntegerRule) {
                        AssistChip(
                            onClick = {},
                            label = { Text("${rule.numStates} states") },
                            modifier = Modifier.height(28.dp)
                        )
                    }
                }
            }
        }

        // Properties
        val properties = rule.properties
        if (properties.isEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "This rule has no configurable properties",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            // Configure section header
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Tune,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    "Configure",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            HorizontalDivider(
                modifier = Modifier.padding(bottom = 4.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
            )
            properties.forEach { property ->
                when (property) {
                    is RuleProperty.IntProperty -> IntPropertyControl(
                        property = property,
                        onValueChanged = { onRuleChanged(rule.withProperty(property.key, it)) }
                    )
                    is RuleProperty.FloatProperty -> FloatPropertyControl(
                        property = property,
                        onValueChanged = { onRuleChanged(rule.withProperty(property.key, it)) }
                    )
                    is RuleProperty.BooleanProperty -> BooleanPropertyControl(
                        property = property,
                        onValueChanged = { onRuleChanged(rule.withProperty(property.key, it)) }
                    )
                    is RuleProperty.ChoiceProperty -> ChoicePropertyControl(
                        property = property,
                        onValueChanged = { onRuleChanged(rule.withProperty(property.key, it)) }
                    )
                }
            }
        }
    }
}

@Composable
private fun IntPropertyControl(
    property: RuleProperty.IntProperty,
    onValueChanged: (Int) -> Unit
) {
    var sliderValue by remember(property.value) { mutableStateOf(property.value.toFloat()) }
    var textValue by remember(property.value) { mutableStateOf(property.value.toString()) }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            property.label,
            style = MaterialTheme.typography.labelMedium
        )
        if (property.description.isNotEmpty()) {
            Text(
                property.description,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Slider(
                value = sliderValue,
                onValueChange = {
                    sliderValue = it
                    textValue = it.roundToInt().toString()
                },
                onValueChangeFinished = {
                    onValueChanged(sliderValue.roundToInt())
                },
                valueRange = property.min.toFloat()..property.max.toFloat(),
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = textValue,
                onValueChange = { input ->
                    textValue = input
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        val parsed = textValue.toIntOrNull()?.coerceIn(property.min, property.max)
                        if (parsed != null) {
                            sliderValue = parsed.toFloat()
                            onValueChanged(parsed)
                        } else {
                            textValue = property.value.toString()
                        }
                    }
                ),
                singleLine = true,
                modifier = Modifier.width(64.dp),
                textStyle = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun FloatPropertyControl(
    property: RuleProperty.FloatProperty,
    onValueChanged: (Float) -> Unit
) {
    var sliderValue by remember(property.value) { mutableStateOf(property.value) }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(property.label, style = MaterialTheme.typography.labelMedium)
            Text(
                "%.2f".format(sliderValue),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (property.description.isNotEmpty()) {
            Text(
                property.description,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Slider(
            value = sliderValue,
            onValueChange = { sliderValue = it },
            onValueChangeFinished = { onValueChanged(sliderValue) },
            valueRange = property.min..property.max
        )
    }
}

@Composable
private fun BooleanPropertyControl(
    property: RuleProperty.BooleanProperty,
    onValueChanged: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(property.label, style = MaterialTheme.typography.labelMedium)
            if (property.description.isNotEmpty()) {
                Text(
                    property.description,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(
            checked = property.value,
            onCheckedChange = onValueChanged
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChoicePropertyControl(
    property: RuleProperty.ChoiceProperty,
    onValueChanged: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(property.label, style = MaterialTheme.typography.labelMedium)
        if (property.description.isNotEmpty()) {
            Text(
                property.description,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(property.value, modifier = Modifier.weight(1f))
                Icon(Icons.Default.ArrowDropDown, null)
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                property.choices.forEach { choice ->
                    DropdownMenuItem(
                        text = { Text(choice) },
                        onClick = {
                            onValueChanged(choice)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
