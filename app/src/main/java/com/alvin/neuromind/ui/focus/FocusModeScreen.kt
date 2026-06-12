package com.alvin.neuromind.ui

@Composable
fun FocusModeScreen(task: Task, onFinish: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Currently Focusing On:", style = MaterialTheme.typography.labelLarge)
            Text(text = task.title, style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(32.dp))
            // Placeholder for Pomodoro Timer
            Text(text = "25:00", style = MaterialTheme.typography.displayLarge)
            Spacer(modifier = Modifier.height(64.dp))
            Button(onClick = onFinish) { Text("Finish Session") }
        }
    }
}