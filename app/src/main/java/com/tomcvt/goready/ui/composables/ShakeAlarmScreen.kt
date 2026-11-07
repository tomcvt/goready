package com.tomcvt.goready.ui.composables

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import kotlin.math.sqrt

class ShakeDetector(
    private val thresholdG: Float = 2.2f,   // tune: 1.8 easy, 2.7 hard
    private val minGapMs: Long = 300,
    private val onShake: () -> Unit
) : SensorEventListener {
    private var lastMs = 0L
    override fun onSensorChanged(e: SensorEvent) {
        val (x, y, z) = e.values
        val g = sqrt(x * x + y * y + z * z) / SensorManager.GRAVITY_EARTH
        if (g > thresholdG) {
            val now = e.timestamp / 1_000_000   // ns since boot -> ms
            if (now - lastMs > minGapMs) { lastMs = now; onShake() }
        }
    }
    override fun onAccuracyChanged(s: Sensor?, a: Int) {}
}



@Composable
fun ShakeTaskScreen(
    target: Int,
    onFinish: () -> Unit,
    onInteraction: () -> Unit,
    dismissable: Boolean = false
) {
    val ctx = LocalContext.current
    val sm = remember { ctx.getSystemService(SensorManager::class.java) }
    val sensor = remember { sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) }
    if (sensor == null) {
        SwipeGate(onFinish);
        return
    }   // no accelerometer: fall back, never trap the user

    var count by rememberSaveable { mutableIntStateOf(0) }
    val done by rememberUpdatedState(onFinish)

    val debounceTime = 1000L
    var interactionKey by remember { mutableLongStateOf(0L) }
    var lastInteraction by remember { mutableLongStateOf(0L) }
    LaunchedEffect(interactionKey) {
        if (System.currentTimeMillis() - lastInteraction > debounceTime) {
            onInteraction()
            lastInteraction = System.currentTimeMillis()
        }
    }

    DisposableEffect(sm) {
        var finished = false
        val l = ShakeDetector {
            if (finished) return@ShakeDetector
            count++
            interactionKey++;
            if (count >= target) { finished = true; done() }
        }
        sm.registerListener(l, sensor, SensorManager.SENSOR_DELAY_GAME)
        onDispose { sm.unregisterListener(l) }
    }

    ShakeProgressContent(count = count, target = target)
}

@Composable
private fun ShakeProgressContent(
    count: Int,
    target: Int,
    modifier: Modifier = Modifier
) {
    val progress by animateFloatAsState(
        targetValue = (count / target.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f),
        label = "shakeProgress"
    )
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            "Shake your phone",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 48.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$count/$target",
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp)
                .clip(RoundedCornerShape(8.dp)),
            strokeCap = StrokeCap.Round
        )
    }
}

@Composable
fun SwipeGate(
    onFinish: () -> Unit
) {
    Button(onClick = onFinish) {
        Text("Finish")
    }
}

@Composable
fun DismissableWrapper(
    onDismissed: () -> Unit,
    dismissable: Boolean = false,
    content: @Composable () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        content()
        if (dismissable) {
            SimpleDeleteButton(onDelete = onDismissed, modifier = Modifier.align(Alignment.TopEnd))
        }
    }
}