package com.mhurston.ascendant.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mhurston.ascendant.R
import com.mhurston.ascendant.ui.theme.AscendantBg
import com.mhurston.ascendant.ui.theme.AuraCyan
import com.mhurston.ascendant.ui.theme.ManaPurple
import com.mhurston.ascendant.ui.theme.Orbitron
import com.mhurston.ascendant.ui.theme.TextDim

/** Branded title screen: the app logo + name; a tap anywhere enters the main app. */
@Composable
fun TitleScreen(onEnter: () -> Unit) {
    val pulse = rememberInfiniteTransition(label = "tap-pulse")
    val tapAlpha by pulse.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "tap-alpha"
    )
    Box(
        Modifier
            .fillMaxSize()
            .background(AscendantBg)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onEnter() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(R.drawable.ic_launcher_img),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(148.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .border(2.dp, ManaPurple, RoundedCornerShape(32.dp))
            )
            Spacer(Modifier.height(28.dp))
            Text(
                "ASCENDANT",
                fontFamily = Orbitron,
                fontWeight = FontWeight.Black,
                fontSize = 40.sp,
                letterSpacing = 5.sp,
                color = ManaPurple
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "RISE · TRAIN · ASCEND",
                fontFamily = Orbitron,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                letterSpacing = 3.sp,
                color = AuraCyan
            )
        }
        Text(
            "TAP TO BEGIN",
            fontFamily = Orbitron,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            letterSpacing = 4.sp,
            color = TextDim,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 64.dp)
                .alpha(tapAlpha)
        )
    }
}
