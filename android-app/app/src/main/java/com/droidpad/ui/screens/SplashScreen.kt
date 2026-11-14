package com.droidpad.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.droidpad.R
import com.droidpad.ui.theme.AccentMintGreen
import com.droidpad.ui.theme.DarkSlateBackground
import kotlinx.coroutines.delay

/**
 * Animated Splash Screen on app launch.
 * Displays brand logo with scale + fade animation and transitions to Home.
 */
@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    val scale = remember { Animatable(0.7f) }
    val alpha = remember { Animatable(0.0f) }

    LaunchedEffect(Unit) {
        // Run scale and fade in animation
        scale.animateTo(
            targetValue = 1.0f,
            animationSpec = tween(
                durationMillis = 800,
                easing = FastOutSlowInEasing
            )
        )
        alpha.animateTo(
            targetValue = 1.0f,
            animationSpec = tween(durationMillis = 600)
        )
        delay(600) // Brief hold time
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkSlateBackground),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.droidpad_logo),
                contentDescription = "DroidPad Logo",
                modifier = Modifier
                    .size(110.dp)
                    .scale(scale.value)
                    .alpha(alpha.value.coerceIn(0.1f, 1.0f))
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "DroidPad",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = AccentMintGreen,
                modifier = Modifier.alpha(alpha.value.coerceIn(0.1f, 1.0f))
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Virtual Controller & Gamepad",
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = Color.LightGray,
                modifier = Modifier.alpha(alpha.value.coerceIn(0.1f, 1.0f))
            )
        }
    }
}
