package org.caexplorer.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import org.caexplorer.analysis.AnalysisResult

/**
 * Dashboard that displays analysis results in a scrollable list of cards.
 */
@Composable
fun AnalysisDashboard(
    results: Map<String, List<AnalysisResult>>,
    modifier: Modifier = Modifier
) {
    val entries = results.entries.toList()
    LazyColumn(
        modifier = modifier.padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(entries, key = { it.key }) { (analysisName, resultList) ->
            AnalysisCard(analysisName, resultList)
        }
    }
}

@Composable
private fun AnalysisCard(name: String, results: List<AnalysisResult>) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(name, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(8.dp))
            results.forEach { result ->
                when (result) {
                    is AnalysisResult.SingleValue -> {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(result.label, style = MaterialTheme.typography.bodySmall)
                            Text(
                                "${formatNumber(result.value)} ${result.unit}".trim(),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    is AnalysisResult.TimeSeries -> {
                        Text(result.label, style = MaterialTheme.typography.bodySmall)
                        MiniChart(
                            result.values,
                            modifier = Modifier.fillMaxWidth().height(60.dp)
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                    is AnalysisResult.Histogram -> {
                        Text(result.label, style = MaterialTheme.typography.bodySmall)
                        MiniBarChart(
                            result.bins,
                            modifier = Modifier.fillMaxWidth().height(80.dp)
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                    is AnalysisResult.Grid -> { /* Grid rendering reserved for future use */ }
                }
            }
        }
    }
}

/**
 * Sparkline composable — draws a simple line chart.
 */
@Composable
fun MiniChart(values: List<Double>, modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.primary
    Canvas(modifier = modifier) {
        if (values.size < 2) return@Canvas
        val maxVal = values.max()
        val minVal = values.min()
        val range = (maxVal - minVal).coerceAtLeast(0.001)
        val stepX = size.width / (values.size - 1)

        val path = Path()
        values.forEachIndexed { i, v ->
            val x = i * stepX
            val y = size.height - ((v - minVal) / range * size.height).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color, style = Stroke(width = 2f))
    }
}

/**
 * Simple bar chart composable.
 */
@Composable
fun MiniBarChart(bins: Map<String, Double>, modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.primary
    Canvas(modifier = modifier) {
        val entries = bins.entries.toList()
        if (entries.isEmpty()) return@Canvas
        val maxVal = entries.maxOf { it.value }.coerceAtLeast(0.001)
        val barWidth = size.width / entries.size
        entries.forEachIndexed { i, (_, value) ->
            val barHeight = (value / maxVal * size.height).toFloat()
            drawRect(
                color = color,
                topLeft = Offset(i * barWidth, size.height - barHeight),
                size = Size(barWidth * 0.8f, barHeight)
            )
        }
    }
}

private fun formatNumber(value: Double): String {
    return when {
        value == 0.0 -> "0"
        value >= 1_000_000 -> "${(value / 1_000_000).let { "%.1f".format(it) }}M"
        value >= 1_000 -> "${(value / 1_000).let { "%.1f".format(it) }}K"
        value >= 100 -> "%.0f".format(value)
        value >= 1 -> "%.2f".format(value)
        else -> "%.4f".format(value)
    }
}
