package tn.mizan.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import tn.mizan.app.ui.components.MizanLogo
import tn.mizan.app.ui.theme.Cream
import tn.mizan.app.ui.theme.Emerald
import tn.mizan.app.ui.theme.Gold

/**
 * Splash animé, comme dans le prototype Figma :
 * 1. la balance apparaît, grandit et se redresse avec un rebond, le halo s'élargit ;
 * 2. elle remonte, puis le nom et le slogan apparaissent en fondu ;
 * 3. après une courte pause, on passe à l'accueil.
 */
@Composable
fun AnimatedSplash(onFinished: () -> Unit) {
    val logoScale = remember { Animatable(0.4f) }
    val logoAlpha = remember { Animatable(0f) }
    val rotation = remember { Animatable(-12f) }
    val halo = remember { Animatable(0.3f) }
    val lift = remember { Animatable(0f) }
    val textAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch { logoAlpha.animateTo(1f, tween(400)) }
        launch { rotation.animateTo(0f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow)) }
        launch { halo.animateTo(1f, tween(700, easing = FastOutSlowInEasing)) }
        logoScale.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow))
        launch { lift.animateTo(1f, tween(500, easing = FastOutSlowInEasing)) }
        delay(150)
        textAlpha.animateTo(1f, tween(500))
        delay(1_000)
        onFinished()
    }

    val liftPx = with(LocalDensity.current) { 70.dp.toPx() }
    val textShiftPx = with(LocalDensity.current) { 24.dp.toPx() }

    Box(
        Modifier
            .fillMaxSize()
            .background(Emerald),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.graphicsLayer { translationY = -liftPx * lift.value }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .size(220.dp)
                        .graphicsLayer {
                            scaleX = halo.value
                            scaleY = halo.value
                            alpha = 0.12f * logoAlpha.value
                        }
                        .background(Gold, CircleShape)
                )
                MizanLogo(
                    size = 132.dp,
                    circle = Cream,
                    lines = Emerald,
                    modifier = Modifier.graphicsLayer {
                        scaleX = logoScale.value
                        scaleY = logoScale.value
                        rotationZ = rotation.value
                        alpha = logoAlpha.value
                    }
                )
            }
            Spacer(Modifier.height(8.dp))
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer {
                    alpha = textAlpha.value
                    translationY = (1f - textAlpha.value) * textShiftPx
                }
            ) {
                Text("ميـزان", color = Cream, fontSize = 56.sp, fontWeight = FontWeight.Bold)
                Text("Mizan", color = Gold, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Text(
            "Équilibrer ses dépenses, simplement.",
            color = Cream,
            fontSize = 14.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 48.dp)
                .graphicsLayer { alpha = textAlpha.value }
        )
    }
}
