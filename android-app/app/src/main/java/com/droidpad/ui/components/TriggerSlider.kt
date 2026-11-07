package com.droidpad.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TriggerSlider(
    label: String,
    modifier: Modifier = Modifier,
    onValueChange: (Float) -> Unit
) {
    var triggerValue by remember { mutableFloatStateOf(0f) }

    Column(
        modifier = modifier.width(60.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = label, color = Color.White, fontSize = 12.sp)
        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .width(48.dp)
                .height(80.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF2D2D2D))
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val v = (offset.y / size.height).coerceIn(0f, 1f)
                            triggerValue = v
                            onValueChange(v)
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val v = (change.position.y / size.height).coerceIn(0f, 1f)
                            triggerValue = v
                            onValueChange(v)
                        },
                        onDragEnd = {
                            triggerValue = 0f
                            onValueChange(0f)
                        },
                        onDragCancel = {
                            triggerValue = 0f
                            onValueChange(0f)
                        }
                    )
                },
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(fraction = triggerValue.coerceAtLeast(0.05f))
                    .background(Color(0xFF107C10))
            )
        }
    }
}
