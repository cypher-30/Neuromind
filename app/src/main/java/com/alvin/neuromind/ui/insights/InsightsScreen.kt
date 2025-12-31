package com.alvin.neuromind.ui.insights

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.axis.horizontal.bottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.startAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.entryOf
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(viewModel: InsightsViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Insights") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { innerPadding ->
            LazyColumn(
                modifier = Modifier.padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    WellnessCard(score = uiState.wellnessScore)
                }
                item {
                    WeeklyCompletionChartCard(
                        completionData = uiState.weeklyCompletionData,
                        dayLabels = uiState.weekDayLabels
                    )
                }
            }
        }
    }

@Composable
private fun WellnessCard(score: Float) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Recent Wellness", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                LinearProgressIndicator(
                    progress = { score },
                    modifier = Modifier.weight(1f).height(12.dp)
                )
                Text(
                    text = "${(score * 100).toInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun WeeklyCompletionChartCard(
    completionData: List<Float>,
    dayLabels: List<String>
) {
    val chartModelProducer = remember { ChartEntryModelProducer() }

    LaunchedEffect(completionData) {
        chartModelProducer.setEntries(completionData.mapIndexed { index, value -> entryOf(index.toFloat(), value) })
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Tasks Completed This Week", style = MaterialTheme.typography.titleLarge)
            Spacer(modifier = Modifier.height(16.dp))
            if (dayLabels.isNotEmpty()) {
                Chart(
                    chart = columnChart(),
                    chartModelProducer = chartModelProducer,
                    startAxis = startAxis(),
                    bottomAxis = bottomAxis(
                        valueFormatter = { value, _ ->
                            dayLabels.getOrNull(value.toInt()) ?: ""
                        }
                    ),
                )
            } else {
                Text("No data available for this week yet.")
            }
        }
    }
}