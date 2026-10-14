package com.squishey.stoolsentry.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import com.squishey.stoolsentry.domain.BristolType
import com.squishey.stoolsentry.ui.theme.Throne

/**
 * Original geometric drawings of the seven Bristol types, drawn on a canvas about 3:2.
 * Positions are fractions of the canvas width and height.
 */
@Composable
fun BristolGlyph(
    type: BristolType,
    modifier: Modifier = Modifier,
    tint: Color = Throne.GlyphBrown,
) {
    Canvas(modifier) {
        GlyphPainter(this, tint).draw(type)
    }
}

private class GlyphPainter(private val scope: DrawScope, tint: Color) {
    private val w = scope.size.width
    private val h = scope.size.height
    private val light = lerp(tint, Color.White, 0.24f)
    private val base = tint
    private val dark = lerp(tint, Color.Black, 0.32f)
    private val crease = lerp(tint, Color.Black, 0.5f)
    private val shine = Color.White.copy(alpha = 0.16f)

    fun draw(type: BristolType) = when (type) {
        BristolType.TYPE_1 -> hardLumps()
        BristolType.TYPE_2 -> lumpySausage()
        BristolType.TYPE_3 -> crackedSausage()
        BristolType.TYPE_4 -> smoothSnake()
        BristolType.TYPE_5 -> softBlobs()
        BristolType.TYPE_6 -> mushyPile()
        BristolType.TYPE_7 -> puddle()
    }

    private fun lump(x: Float, y: Float, r: Float) {
        val center = Offset(x * w, y * h)
        val radius = r * h
        scope.drawCircle(
            brush = Brush.radialGradient(
                listOf(light, base, dark),
                center = center + Offset(-radius * 0.35f, -radius * 0.35f),
                radius = radius * 1.5f,
            ),
            radius = radius,
            center = center,
        )
        scope.drawCircle(
            color = shine,
            radius = radius * 0.28f,
            center = center + Offset(-radius * 0.38f, -radius * 0.38f),
        )
    }

    private fun verticalBody(top: Float, bottom: Float): Brush =
        Brush.verticalGradient(listOf(light, base, dark), startY = top * h, endY = bottom * h)

    private fun hardLumps() {
        lump(0.16f, 0.62f, 0.17f)
        lump(0.36f, 0.36f, 0.15f)
        lump(0.40f, 0.74f, 0.13f)
        lump(0.61f, 0.55f, 0.19f)
        lump(0.82f, 0.34f, 0.13f)
        lump(0.85f, 0.73f, 0.12f)
    }

    private fun lumpySausage() {
        scope.drawRoundRect(
            brush = verticalBody(0.3f, 0.7f),
            topLeft = Offset(0.14f * w, 0.3f * h),
            size = Size(0.72f * w, 0.4f * h),
            cornerRadius = CornerRadius(0.2f * h),
        )
        val count = 6
        for (i in 0 until count) {
            val x = 0.17f + i * (0.66f / (count - 1))
            val y = 0.5f + if (i % 2 == 0) -0.035f else 0.035f
            val r = 0.2f + (i % 3) * 0.018f
            lump(x, y, r)
        }
    }

    private fun crackedSausage() {
        scope.drawRoundRect(
            brush = verticalBody(0.27f, 0.73f),
            topLeft = Offset(0.07f * w, 0.27f * h),
            size = Size(0.86f * w, 0.46f * h),
            cornerRadius = CornerRadius(0.23f * h),
        )
        scope.drawRoundRect(
            color = shine,
            topLeft = Offset(0.16f * w, 0.34f * h),
            size = Size(0.58f * w, 0.06f * h),
            cornerRadius = CornerRadius(0.03f * h),
        )
        val stroke = 0.035f * h
        listOf(0.27f, 0.43f, 0.59f, 0.75f).forEach { x ->
            scope.drawLine(
                color = crease,
                start = Offset(x * w, 0.28f * h),
                end = Offset((x - 0.035f) * w, 0.45f * h),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
        }
        listOf(0.35f, 0.52f, 0.68f).forEach { x ->
            scope.drawLine(
                color = crease,
                start = Offset(x * w, 0.72f * h),
                end = Offset((x + 0.03f) * w, 0.58f * h),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
        }
    }

    private fun smoothSnake() {
        val body = Path().apply {
            moveTo(0.13f * w, 0.6f * h)
            cubicTo(0.33f * w, 0.1f * h, 0.56f * w, 0.92f * h, 0.87f * w, 0.4f * h)
        }
        scope.drawPath(
            path = body,
            brush = verticalBody(0.22f, 0.8f),
            style = Stroke(width = 0.34f * h, cap = StrokeCap.Round),
        )
        val highlight = Path().apply {
            moveTo(0.16f * w, 0.5f * h)
            cubicTo(0.33f * w, 0.02f * h, 0.56f * w, 0.82f * h, 0.84f * w, 0.32f * h)
        }
        scope.drawPath(
            path = highlight,
            color = shine,
            style = Stroke(width = 0.06f * h, cap = StrokeCap.Round),
        )
    }

    private fun blob(left: Float, top: Float, width: Float, height: Float) {
        val topLeft = Offset(left * w, top * h)
        val size = Size(width * w, height * h)
        scope.drawOval(
            brush = Brush.radialGradient(
                listOf(light, base, dark),
                center = topLeft + Offset(size.width * 0.35f, size.height * 0.3f),
                radius = maxOf(size.width, size.height) * 0.9f,
            ),
            topLeft = topLeft,
            size = size,
        )
        scope.drawOval(
            color = shine,
            topLeft = topLeft + Offset(size.width * 0.18f, size.height * 0.14f),
            size = Size(size.width * 0.34f, size.height * 0.22f),
        )
    }

    private fun softBlobs() {
        blob(0.06f, 0.36f, 0.3f, 0.36f)
        blob(0.37f, 0.2f, 0.27f, 0.34f)
        blob(0.6f, 0.46f, 0.32f, 0.34f)
    }

    private fun mushyPile() {
        lump(0.26f, 0.62f, 0.19f)
        lump(0.38f, 0.44f, 0.21f)
        lump(0.36f, 0.71f, 0.16f)
        lump(0.52f, 0.56f, 0.24f)
        lump(0.66f, 0.42f, 0.19f)
        lump(0.5f, 0.72f, 0.18f)
        lump(0.63f, 0.7f, 0.17f)
        lump(0.76f, 0.62f, 0.19f)
        lump(0.13f, 0.46f, 0.05f)
        lump(0.87f, 0.4f, 0.05f)
        lump(0.91f, 0.74f, 0.04f)
        lump(0.09f, 0.76f, 0.04f)
        lump(0.52f, 0.2f, 0.045f)
    }

    private fun puddle() {
        val body = verticalBody(0.48f, 0.86f)
        scope.drawOval(body, topLeft = Offset(0.06f * w, 0.5f * h), size = Size(0.6f * w, 0.32f * h))
        scope.drawOval(body, topLeft = Offset(0.36f * w, 0.55f * h), size = Size(0.56f * w, 0.28f * h))
        val ripple = Stroke(width = 0.028f * h)
        scope.drawOval(
            color = shine,
            topLeft = Offset(0.17f * w, 0.58f * h),
            size = Size(0.3f * w, 0.12f * h),
            style = ripple,
        )
        scope.drawOval(
            color = shine,
            topLeft = Offset(0.53f * w, 0.63f * h),
            size = Size(0.24f * w, 0.1f * h),
            style = ripple,
        )
        lump(0.24f, 0.3f, 0.065f)
        lump(0.72f, 0.27f, 0.05f)
        lump(0.5f, 0.16f, 0.04f)
    }
}
