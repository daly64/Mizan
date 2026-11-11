package tn.mizan.app.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import tn.mizan.app.R
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import tn.mizan.app.ui.components.MizanLogo
import tn.mizan.app.ui.theme.BrandCream as Cream
import tn.mizan.app.ui.theme.BrandEmerald as Emerald
import tn.mizan.app.ui.theme.BrandGold as Gold

/**
 * Splash animé, comme dans le prototype Figma :
 * 1. la balance apparaît, grandit et se redresse avec un rebond, le halo s'élargit ;
 * 2. elle remonte, puis ميزان s'écrit en calligraphie Ruqaa, de droite à gauche ;
 *    « Mizan » et le slogan apparaissent ensuite en fondu ;
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
    val writing = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch { logoAlpha.animateTo(1f, tween(400)) }
        launch { rotation.animateTo(0f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow)) }
        launch { halo.animateTo(1f, tween(700, easing = FastOutSlowInEasing)) }
        logoScale.animateTo(1f, spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessLow))
        launch { lift.animateTo(1f, tween(500, easing = FastOutSlowInEasing)) }
        delay(150)
        // ميزان s'écrit de droite à gauche, comme à la plume
        writing.animateTo(1f, tween(1_400, easing = LinearEasing))
        textAlpha.animateTo(1f, tween(450))
        delay(900)
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
            Spacer(Modifier.height(4.dp))
            CalligraphyName(progress = writing.value)
            Text(
                "Mizan",
                color = Gold,
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.graphicsLayer {
                    alpha = textAlpha.value
                    translationY = (1f - textAlpha.value) * textShiftPx
                }
            )
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

/** Police calligraphique Ruqaa (Aref Ruqaa, licence OFL), embarquée dans l'app : aucun téléchargement. */
private val Ruqaa = FontFamily(Font(R.font.aref_ruqaa_bold, FontWeight.Bold))

/**
 * Le nom en arabe, révélé de droite à gauche au rythme de [progress] (0 → 1),
 * avec un point doré qui suit la « plume ».
 */
@Composable
private fun CalligraphyName(progress: Float) {
    Text(
        "ميزان",
        color = Cream,
        fontSize = 72.sp,
        fontFamily = Ruqaa,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .drawWithContent {
                val shown = size.width * progress
                val edge = size.width - shown
                clipRect(left = edge, top = 0f, right = size.width, bottom = size.height) {
                    this@drawWithContent.drawContent()
                }
                if (progress in 0.01f..0.99f) {
                    drawCircle(Gold, radius = 5.dp.toPx(), center = Offset(edge, size.height * 0.55f))
                }
            }
    )
}
