package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SubtitleConfig

@Composable
fun CustomSubtitleOverlay(
    subtitleText: String,
    config: SubtitleConfig,
    modifier: Modifier = Modifier
) {
    if (!config.isEnabled || subtitleText.isBlank()) return

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        AnimatedVisibility(
            visible = subtitleText.isNotBlank(),
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.padding(
                bottom = (80 * (config.verticalOffsetPercent / 0.08f)).coerceIn(30f, 220f).dp,
                start = 24.dp,
                end = 24.dp
            )
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = config.backgroundOpacity))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = subtitleText,
                    style = TextStyle(
                        color = Color(config.textColorHex),
                        fontSize = config.fontSizeSp.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        shadow = Shadow(
                            color = Color.Black,
                            offset = Offset(2f, 2f),
                            blurRadius = 4f
                        )
                    )
                )
            }
        }
    }
}
