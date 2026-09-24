package com.alvin.neuromind.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.alvin.neuromind.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Matches Neuromind.dc.html's splash: the three-circle mark scales in on a
// spring-like ease, "Neuromind" fades up at 450ms and the subtitle at 550ms,
// ~1800ms total. No gradient "bloom" transition — the splash ground and the
// app's own background use the active theme background, so there is nothing to
// cross-fade into.
@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val scale = remember { Animatable(0.6f) }
    val logoAlpha = remember { Animatable(0f) }
    val titleAlpha = remember { Animatable(0f) }
    val titleOffset = remember { Animatable(6f) }
    val subtitleAlpha = remember { Animatable(0f) }
    val subtitleOffset = remember { Animatable(6f) }

    LaunchedEffect(Unit) {
        launch {
            scale.animateTo(1f, animationSpec = tween(700, easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)))
        }
        launch {
            logoAlpha.animateTo(1f, animationSpec = tween(700, easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)))
        }
        delay(450)
        launch { titleAlpha.animateTo(1f, animationSpec = tween(500, easing = FastOutSlowInEasing)) }
        launch { titleOffset.animateTo(0f, animationSpec = tween(500, easing = FastOutSlowInEasing)) }
        delay(100) // subtitle starts at 550ms total
        launch { subtitleAlpha.animateTo(1f, animationSpec = tween(500, easing = FastOutSlowInEasing)) }
        launch { subtitleOffset.animateTo(0f, animationSpec = tween(500, easing = FastOutSlowInEasing)) }
        delay(1250) // ~1800ms total
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(id = R.drawable.ic_neuromind_logo),
                contentDescription = "Neuromind",
                modifier = Modifier
                    .size(96.dp)
                    .graphicsLayer {
                        scaleX = scale.value
                        scaleY = scale.value
                        alpha = logoAlpha.value
                    }
            )
            Spacer(modifier = Modifier.height(22.dp))
            Text(
                text = "Neuromind",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.graphicsLayer {
                    alpha = titleAlpha.value
                    translationY = titleOffset.value
                }
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Your calm study partner",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.graphicsLayer {
                    alpha = subtitleAlpha.value
                    translationY = subtitleOffset.value
                }
            )
        }
    }
}
