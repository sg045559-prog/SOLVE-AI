package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

data class LeafParticle(
    var xRatio: Float,
    var yRatio: Float,
    val size: Float,
    val speed: Float,
    val swayAmplitude: Float,
    val swayFrequency: Float,
    var rotation: Float,
    val rotationSpeed: Float,
    val color: Color,
    val initialPhase: Float
)

@Composable
fun FallingLeavesCanvas(
    modifier: Modifier = Modifier,
    leafCount: Int = 18,
    enabled: Boolean = true
) {
    if (!enabled) return

    val leaves = remember {
        val leafColors = listOf(
            Color(0x3552B788), // Soft mint leaf
            Color(0x302D6A4F), // Forest green
            Color(0x2874C69D), // Sage green
            Color(0x2A95D5B2), // Light emerald
            Color(0x25B7E4C7)  // Translucent spring leaf
        )
        List(leafCount) { i ->
            LeafParticle(
                xRatio = Random.nextFloat(),
                yRatio = Random.nextFloat(),
                size = Random.nextFloat() * 14f + 12f,
                speed = Random.nextFloat() * 0.00035f + 0.0002f,
                swayAmplitude = Random.nextFloat() * 25f + 15f,
                swayFrequency = Random.nextFloat() * 1.5f + 0.8f,
                rotation = Random.nextFloat() * 360f,
                rotationSpeed = (Random.nextFloat() - 0.5f) * 0.8f,
                color = leafColors[i % leafColors.size],
                initialPhase = Random.nextFloat() * (2 * PI.toFloat())
            )
        }
    }

    var timeSeconds by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(Unit) {
        var lastNano = System.nanoTime()
        while (true) {
            withFrameNanos { currentNano ->
                val dt = (currentNano - lastNano) / 1_000_000_000f
                lastNano = currentNano
                timeSeconds += dt

                for (leaf in leaves) {
                    leaf.yRatio += leaf.speed * (dt * 60f)
                    leaf.rotation += leaf.rotationSpeed * (dt * 60f)
                    if (leaf.yRatio > 1.05f) {
                        leaf.yRatio = -0.05f
                        leaf.xRatio = Random.nextFloat()
                    }
                }
            }
        }
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        if (width <= 0 || height <= 0) return@Canvas

        for (leaf in leaves) {
            val swayOffset = sin(timeSeconds * leaf.swayFrequency + leaf.initialPhase) * leaf.swayAmplitude
            val px = (leaf.xRatio * width) + swayOffset
            val py = leaf.yRatio * height

            drawLeafShape(
                center = Offset(px, py),
                size = leaf.size,
                rotationDegrees = leaf.rotation,
                color = leaf.color
            )
        }
    }
}

private fun DrawScope.drawLeafShape(
    center: Offset,
    size: Float,
    rotationDegrees: Float,
    color: Color
) {
    rotate(degrees = rotationDegrees, pivot = center) {
        val path = Path().apply {
            val halfW = size * 0.55f
            val halfH = size

            moveTo(center.x, center.y - halfH)
            cubicTo(
                center.x + halfW * 1.4f, center.y - halfH * 0.4f,
                center.x + halfW * 1.2f, center.y + halfH * 0.5f,
                center.x, center.y + halfH
            )
            cubicTo(
                center.x - halfW * 1.2f, center.y + halfH * 0.5f,
                center.x - halfW * 1.4f, center.y - halfH * 0.4f,
                center.x, center.y - halfH
            )
            close()
        }
        drawPath(path = path, color = color)
    }
}
