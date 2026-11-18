package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.BrandSkyBlue
import kotlinx.coroutines.delay

private val BrandSplashBlack = Color(0xFF02060D)

@Composable
fun SplashSequenceScreen(
  onSplashFinished: () -> Unit,
) {
  var stage by remember { mutableIntStateOf(0) }

  LaunchedEffect(Unit) {
    delay(1150L)
    stage = 1
    delay(2600L)
    onSplashFinished()
  }

  AnimatedContent(
    targetState = stage,
    transitionSpec = {
      fadeIn(tween(420)) togetherWith fadeOut(tween(320))
    },
    label = "splash_stage_transition",
  ) { currentStage ->
    if (currentStage == 0) {
      PrimaryBrandSplash()
    } else {
      SecondaryWelcomeSplash(onContinue = onSplashFinished)
    }
  }
}

@Composable
private fun PrimaryBrandSplash() {
  val scaleAnim = remember { Animatable(0.88f) }
  val alphaAnim = remember { Animatable(0f) }

  LaunchedEffect(Unit) {
    alphaAnim.animateTo(1f, animationSpec = tween(550, easing = FastOutSlowInEasing))
    scaleAnim.animateTo(1f, animationSpec = tween(650, easing = FastOutSlowInEasing))
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(BrandSplashBlack)
      .statusBarsPadding()
      .navigationBarsPadding(),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(18.dp),
      modifier = Modifier
        .alpha(alphaAnim.value)
        .scale(scaleAnim.value),
    ) {
      Image(
        painter = painterResource(id = R.drawable.img_splash_brand),
        contentDescription = "Logo de Mi Vuelta",
        contentScale = ContentScale.Fit,
        modifier = Modifier
          .widthIn(max = 260.dp)
          .height(92.dp),
      )
      Text(
        text = "Tus pagos y diligencias al día",
        style = MaterialTheme.typography.bodyMedium,
        color = Color(0xFF9FB2CC),
      )
    }
  }
}

@Composable
private fun SecondaryWelcomeSplash(
  onContinue: () -> Unit,
) {
  val progressAnim = remember { Animatable(0f) }
  LaunchedEffect(Unit) {
    progressAnim.animateTo(1f, animationSpec = tween(2500))
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color.Black)
      .statusBarsPadding()
      .navigationBarsPadding(),
    contentAlignment = Alignment.Center,
  ) {
    Column(
      modifier = Modifier
        .widthIn(max = 520.dp)
        .fillMaxSize()
        .padding(horizontal = 28.dp, vertical = 28.dp),
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      // Top brand bar using uploaded logo
      Image(
        painter = painterResource(id = R.drawable.img_splash_brand),
        contentDescription = "Mi Vuelta",
        contentScale = ContentScale.Fit,
        modifier = Modifier
          .height(52.dp)
          .clip(RoundedCornerShape(10.dp)),
      )

      Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Text(
          text = "No pagues\nmás moras.",
          style = MaterialTheme.typography.displaySmall,
          color = Color.White,
          fontWeight = FontWeight.Bold,
        )
        Text(
          text = "Anota cualquier vuelta en 15 segundos y ten siempre a mano el recibo y los avisos.",
          style = MaterialTheme.typography.bodyLarge,
          color = Color(0xFFB4C0D2),
        )

        Spacer(modifier = Modifier.height(4.dp))

        SplashFeatureItem(
          icon = Icons.Rounded.CameraAlt,
          title = "Foto del papel",
          subtitle = "Tírale foto al recibo y guárdalo con tu vuelta.",
        )
        SplashFeatureItem(
          icon = Icons.Rounded.Bolt,
          title = "Plantillas dominicanas",
          subtitle = "Luz, internet, marbete, pasaporte o colegio en 1 toque.",
        )
        SplashFeatureItem(
          icon = Icons.Rounded.NotificationsActive,
          title = "Avisos que sí se sienten",
          subtitle = "Marca \"Ya lo pagué\" directo desde la notificación.",
        )
      }

      Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        LinearProgressIndicator(
          progress = { progressAnim.value },
          modifier = Modifier
            .fillMaxWidth()
            .height(3.dp)
            .clip(RoundedCornerShape(50)),
          color = BrandSkyBlue,
          trackColor = Color(0xFF1A2230),
        )

        Button(
          onClick = onContinue,
          shape = MaterialTheme.shapes.medium,
          colors = ButtonDefaults.buttonColors(
            containerColor = BrandSkyBlue,
            contentColor = Color(0xFF061B36),
          ),
          modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .testTag("splash_continue_button"),
        ) {
          Text(
            text = "Comenzar",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
          )
          Spacer(modifier = Modifier.size(8.dp))
          Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
        }
      }
    }
  }
}

@Composable
private fun SplashFeatureItem(
  icon: ImageVector,
  title: String,
  subtitle: String,
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    Surface(
      color = Color(0xFF101827),
      shape = RoundedCornerShape(14.dp),
      modifier = Modifier.size(46.dp),
    ) {
      Box(contentAlignment = Alignment.Center) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = BrandSkyBlue,
          modifier = Modifier.size(22.dp),
        )
      }
    }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = Color.White,
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = Color(0xFF98A5B8),
      )
    }
  }
}
