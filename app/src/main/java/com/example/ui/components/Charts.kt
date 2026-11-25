package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.CategoryBreakdown
import com.example.domain.model.DailyPoint
import com.example.domain.model.MonthlyTrendPoint
import com.example.ui.theme.ChartPalette
import com.example.ui.theme.TabularAmountStyle
import com.example.util.CurrencyFormatter
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.log10
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

@Composable
fun CategoryDonutChart(
    items: List<CategoryBreakdown>,
    totalAmount: Long,
    selectedIndex: Int?,
    onSelectIndex: (Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val animProgress = remember(items) { Animatable(0f) }
    LaunchedEffect(items) {
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 550, easing = FastOutSlowInEasing),
        )
    }

    val selected = selectedIndex?.let { items.getOrNull(it) }
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    Box(
        modifier = modifier.size(220.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(items) {
                    detectTapGestures { offset ->
                        if (items.isEmpty()) return@detectTapGestures
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val dx = offset.x - cx
                        val dy = offset.y - cy
                        val dist = hypot(dx, dy)
                        val radius = min(size.width, size.height) / 2f
                        if (dist < radius * 0.45f || dist > radius) {
                            onSelectIndex(null)
                            return@detectTapGestures
                        }
                        var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat() + 90f
                        if (angle < 0f) angle += 360f
                        var cumulative = 0f
                        items.forEachIndexed { idx, item ->
                            val sweep = item.percentage * 360f
                            if (angle in cumulative..(cumulative + sweep)) {
                                onSelectIndex(if (selectedIndex == idx) null else idx)
                                return@detectTapGestures
                            }
                            cumulative += sweep
                        }
                    }
                },
        ) {
            val strokeBase = 28.dp.toPx()
            val diameter = min(size.width, size.height) - strokeBase - 12.dp.toPx()
            val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
            val arcSize = Size(diameter, diameter)

            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeBase),
            )

            var startAngle = -90f
            val gapDeg = if (items.size > 1) 2.5f else 0f
            items.forEachIndexed { idx, item ->
                val rawSweep = item.percentage * 360f * animProgress.value
                val sweep = (rawSweep - gapDeg).coerceAtLeast(1f)
                val isSelected = selectedIndex == idx
                val color = ChartPalette[item.colorIndex % ChartPalette.size]
                drawArc(
                    color = if (selectedIndex == null || isSelected) color else color.copy(alpha = 0.35f),
                    startAngle = startAngle,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(
                        width = if (isSelected) strokeBase + 6.dp.toPx() else strokeBase,
                        cap = StrokeCap.Butt,
                    ),
                )
                startAngle += rawSweep
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 32.dp),
        ) {
            Text(
                text = selected?.category ?: "Total",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = CurrencyFormatter.formatCompactRupiah(selected?.amount ?: totalAmount),
                style = TabularAmountStyle.copy(
                    fontSize = MaterialTheme.typography.titleMedium.fontSize,
                    fontWeight = FontWeight.Bold,
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
            )
            if (selected != null) {
                Text(
                    text = "${(selected.percentage * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = ChartPalette[selected.colorIndex % ChartPalette.size],
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
fun DailyBarChart(
    points: List<DailyPoint>,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    var selectedIdx by remember(points) {
        val maxIdx = points.indices.maxByOrNull { points[it].amount } ?: -1
        mutableIntStateOf(if (maxIdx >= 0 && points[maxIdx].amount > 0L) maxIdx else -1)
    }

    val animProgress = remember(points) { Animatable(0f) }
    LaunchedEffect(points) {
        animProgress.animateTo(1f, tween(500, easing = FastOutSlowInEasing))
    }

    val maxAmount = (points.maxOfOrNull { it.amount } ?: 0L).coerceAtLeast(1L)
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val mutedBarColor = accentColor.copy(alpha = 0.35f)
    val zeroBarColor = MaterialTheme.colorScheme.surfaceVariant

    Column(modifier = modifier.fillMaxWidth()) {
        val selectedPoint = points.getOrNull(selectedIdx)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (selectedPoint != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                ) {
                    Text(
                        text = "${selectedPoint.fullDateLabel}: ${CurrencyFormatter.formatRupiah(selectedPoint.amount)}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                    )
                }
            } else {
                Text(
                    text = "Ketuk batang untuk detail harian",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = "Maks: ${CurrencyFormatter.formatCompactRupiah(maxAmount)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(176.dp),
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(points) {
                        detectTapGestures { offset ->
                            if (points.isEmpty()) return@detectTapGestures
                            val slotWidth = size.width / points.size.toFloat()
                            val idx = (offset.x / slotWidth).toInt().coerceIn(0, points.lastIndex)
                            selectedIdx = idx
                        }
                    },
            ) {
                val count = points.size.coerceAtLeast(1)
                val chartHeight = size.height - 16.dp.toPx()
                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)

                listOf(0.25f, 0.5f, 0.75f, 1f).forEach { fraction ->
                    val y = chartHeight * (1f - fraction)
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = dashEffect,
                    )
                }

                val slotWidth = size.width / count.toFloat()
                val barWidth = (slotWidth * 0.62f).coerceIn(4.dp.toPx(), 18.dp.toPx())

                points.forEachIndexed { idx, pt ->
                    val ratio = (pt.amount.toFloat() / maxAmount.toFloat()).coerceIn(0f, 1f)
                    val barHeight = if (pt.amount > 0L) {
                        (chartHeight * ratio * animProgress.value).coerceAtLeast(4.dp.toPx())
                    } else {
                        3.dp.toPx()
                    }
                    val left = idx * slotWidth + (slotWidth - barWidth) / 2f
                    val top = chartHeight - barHeight
                    val barColor = when {
                        pt.amount == 0L -> zeroBarColor
                        selectedIdx == idx || selectedIdx == -1 -> accentColor
                        else -> mutedBarColor
                    }
                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(left, top),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                    )
                }
            }
        }

        if (points.isNotEmpty()) {
            val step = (points.size / 5).coerceAtLeast(1)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                points.forEachIndexed { idx, pt ->
                    if (idx == 0 || idx == points.lastIndex || idx % step == 0) {
                        Text(
                            text = pt.dayLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selectedIdx == idx) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (selectedIdx == idx) FontWeight.Bold else FontWeight.Normal,
                        )
                    }
                }
            }
        }
    }
}

/**
 * D3-inspired 6-month trend line chart implemented natively in Compose Canvas:
 * - Nice rounded linear Y-scale domain (`niceDomainMax`) & left-gutter axis ticks
 * - Smooth monotone cubic spline (`curveMonotoneX`) with vertical gradient area fill
 * - 6-month average dashed reference line
 * - Interactive tap & horizontal drag crosshair scrubbing
 */
@Composable
fun SpendingTrendsLineChart(
    points: List<MonthlyTrendPoint>,
    showExpense: Boolean,
    lineColor: Color,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val values = remember(points, showExpense) {
        points.map { if (showExpense) it.totalExpense else it.totalIncome }
    }
    val animProgress = remember(points, showExpense) { Animatable(0f) }
    LaunchedEffect(points, showExpense) {
        animProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        )
    }

    val rawMax = (values.maxOrNull() ?: 0L).coerceAtLeast(100_000L)
    val domainMax = remember(rawMax) { niceDomainMax(rawMax) }
    val avgValue = remember(values) {
        if (values.isEmpty()) 0L else values.sum() / values.size
    }

    val textMeasurer = rememberTextMeasurer()
    val axisLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val surfaceColor = MaterialTheme.colorScheme.surface
    val avgLineColor = MaterialTheme.colorScheme.tertiary

    val selectedPoint = points.getOrNull(selectedIndex)
    val selectedValue = values.getOrNull(selectedIndex) ?: 0L

    Column(modifier = modifier.fillMaxWidth()) {
        // Active scrub tooltip bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (selectedPoint != null) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = lineColor.copy(alpha = 0.12f),
                ) {
                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                        Text(
                            text = selectedPoint.cycleLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = lineColor,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            text = CurrencyFormatter.formatRupiah(selectedValue),
                            style = TabularAmountStyle.copy(
                                fontSize = MaterialTheme.typography.titleSmall.fontSize,
                                fontWeight = FontWeight.Bold,
                            ),
                            color = lineColor,
                        )
                    }
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Rata-rata 6 bln",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = CurrencyFormatter.formatCompactRupiah(avgValue),
                    style = TabularAmountStyle.copy(
                        fontSize = MaterialTheme.typography.labelLarge.fontSize,
                        fontWeight = FontWeight.SemiBold,
                    ),
                    color = avgLineColor,
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(224.dp),
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("spending_trends_line_canvas")
                    .pointerInput(points) {
                        if (points.isEmpty()) return@pointerInput
                        val leftGutter = 56.dp.toPx()
                        val rightPad = 14.dp.toPx()

                        fun pickNearestIndex(xPos: Float) {
                            val plotW = (size.width - leftGutter - rightPad).coerceAtLeast(1f)
                            val step = if (points.size > 1) plotW / (points.size - 1) else plotW
                            val clampedX = (xPos - leftGutter).coerceIn(0f, plotW)
                            val nearest = (clampedX / step).roundToInt().coerceIn(0, points.lastIndex)
                            onSelectIndex(nearest)
                        }

                        detectTapGestures { offset -> pickNearestIndex(offset.x) }
                    }
                    .pointerInput(points) {
                        if (points.isEmpty()) return@pointerInput
                        val leftGutter = 56.dp.toPx()
                        val rightPad = 14.dp.toPx()
                        detectHorizontalDragGestures { change, _ ->
                            change.consume()
                            val plotW = (size.width - leftGutter - rightPad).coerceAtLeast(1f)
                            val step = if (points.size > 1) plotW / (points.size - 1) else plotW
                            val clampedX = (change.position.x - leftGutter).coerceIn(0f, plotW)
                            val nearest = (clampedX / step).roundToInt().coerceIn(0, points.lastIndex)
                            onSelectIndex(nearest)
                        }
                    },
            ) {
                val leftGutter = 56.dp.toPx()
                val rightPad = 14.dp.toPx()
                val topPad = 14.dp.toPx()
                val bottomGutter = 28.dp.toPx()

                val plotWidth = (size.width - leftGutter - rightPad).coerceAtLeast(1f)
                val plotHeight = (size.height - topPad - bottomGutter).coerceAtLeast(1f)
                val plotBottom = topPad + plotHeight

                val tickCount = 4
                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                val tickTextStyle = TextStyle(
                    fontSize = 10.sp,
                    color = axisLabelColor,
                    fontWeight = FontWeight.Medium,
                )

                // Y-axis ticks & dashed gridlines (D3 axisLeft style)
                for (i in 0..tickCount) {
                    val fraction = i.toFloat() / tickCount.toFloat()
                    val y = plotBottom - (plotHeight * fraction)
                    val tickVal = (domainMax * fraction).toLong()
                    drawLine(
                        color = gridColor,
                        start = Offset(leftGutter, y),
                        end = Offset(size.width - rightPad, y),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = if (i == 0) null else dashEffect,
                    )
                    val label = if (tickVal == 0L) "0" else CurrencyFormatter.formatCompactRupiah(tickVal)
                    val layout = textMeasurer.measure(label, style = tickTextStyle)
                    drawText(
                        textLayoutResult = layout,
                        topLeft = Offset(
                            x = (leftGutter - layout.size.width - 8.dp.toPx()).coerceAtLeast(0f),
                            y = (y - layout.size.height / 2f).coerceIn(0f, plotBottom - layout.size.height),
                        ),
                    )
                }

                if (values.isEmpty()) return@Canvas

                // Map data coordinates via D3 linear/point scales
                val stepX = if (values.size > 1) plotWidth / (values.size - 1) else 0f
                val coords = values.mapIndexed { idx, amount ->
                    val x = if (values.size == 1) leftGutter + plotWidth / 2f else leftGutter + idx * stepX
                    val yRatio = (amount.toFloat() / domainMax.toFloat()).coerceIn(0f, 1f)
                    val y = plotBottom - (plotHeight * yRatio)
                    Offset(x, y)
                }

                // 6-month average reference line
                if (avgValue > 0L) {
                    val avgY = plotBottom - (plotHeight * (avgValue.toFloat() / domainMax.toFloat()).coerceIn(0f, 1f))
                    drawLine(
                        color = avgLineColor.copy(alpha = 0.7f),
                        start = Offset(leftGutter, avgY),
                        end = Offset(size.width - rightPad, avgY),
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f),
                    )
                }

                // Build smooth monotone cubic spline path (d3.curveMonotoneX)
                val linePath = buildMonotoneSplinePath(coords)
                val areaPath = Path().apply {
                    addPath(linePath)
                    lineTo(coords.last().x, plotBottom)
                    lineTo(coords.first().x, plotBottom)
                    close()
                }

                val revealRight = leftGutter + plotWidth * animProgress.value + rightPad
                clipRect(left = leftGutter - 6.dp.toPx(), top = 0f, right = revealRight, bottom = plotBottom + 6.dp.toPx()) {
                    drawPath(
                        path = areaPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                lineColor.copy(alpha = 0.28f),
                                lineColor.copy(alpha = 0.02f),
                            ),
                            startY = topPad,
                            endY = plotBottom,
                        ),
                    )
                    drawPath(
                        path = linePath,
                        color = lineColor,
                        style = Stroke(
                            width = 3.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                        ),
                    )
                }

                // Vertical crosshair for selected point
                val activeCoord = coords.getOrNull(selectedIndex)
                if (activeCoord != null) {
                    drawLine(
                        color = lineColor.copy(alpha = 0.45f),
                        start = Offset(activeCoord.x, topPad),
                        end = Offset(activeCoord.x, plotBottom),
                        strokeWidth = 1.2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f),
                    )
                }

                // Draw data nodes & X-axis month labels
                coords.forEachIndexed { idx, pt ->
                    val isSelected = idx == selectedIndex
                    if (isSelected) {
                        drawCircle(
                            color = lineColor.copy(alpha = 0.22f),
                            radius = 11.dp.toPx(),
                            center = pt,
                        )
                    }
                    drawCircle(
                        color = surfaceColor,
                        radius = if (isSelected) 6.dp.toPx() else 4.5.dp.toPx(),
                        center = pt,
                    )
                    drawCircle(
                        color = lineColor,
                        radius = if (isSelected) 6.dp.toPx() else 4.5.dp.toPx(),
                        center = pt,
                        style = Stroke(width = if (isSelected) 3.dp.toPx() else 2.2.dp.toPx()),
                    )

                    val xLabelStyle = TextStyle(
                        fontSize = 11.sp,
                        color = if (isSelected) lineColor else axisLabelColor,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    )
                    val xLayout = textMeasurer.measure(points[idx].shortMonthLabel, style = xLabelStyle)
                    drawText(
                        textLayoutResult = xLayout,
                        topLeft = Offset(
                            x = (pt.x - xLayout.size.width / 2f).coerceIn(leftGutter, size.width - xLayout.size.width),
                            y = plotBottom + 8.dp.toPx(),
                        ),
                    )
                }
            }
        }
    }
}

/**
 * Computes D3-like Fritsch-Carlson monotone cubic spline through ordered points
 * so the curve never overshoots below zero or creates artificial ripples.
 */
private fun buildMonotoneSplinePath(points: List<Offset>): Path {
    val path = Path()
    if (points.isEmpty()) return path
    path.moveTo(points.first().x, points.first().y)
    if (points.size == 1) return path
    if (points.size == 2) {
        path.lineTo(points[1].x, points[1].y)
        return path
    }

    val n = points.size
    val dx = FloatArray(n - 1)
    val dy = FloatArray(n - 1)
    val secant = FloatArray(n - 1)
    for (i in 0 until n - 1) {
        dx[i] = (points[i + 1].x - points[i].x).coerceAtLeast(0.001f)
        dy[i] = points[i + 1].y - points[i].y
        secant[i] = dy[i] / dx[i]
    }

    val tangent = FloatArray(n)
    tangent[0] = secant[0]
    tangent[n - 1] = secant[n - 2]
    for (i in 1 until n - 1) {
        if (secant[i - 1] * secant[i] <= 0f) {
            tangent[i] = 0f
        } else {
            tangent[i] = (secant[i - 1] + secant[i]) * 0.5f
        }
    }

    // Enforce monotonicity constraints
    for (i in 0 until n - 1) {
        if (secant[i] == 0f) {
            tangent[i] = 0f
            tangent[i + 1] = 0f
        } else {
            val a = tangent[i] / secant[i]
            val b = tangent[i + 1] / secant[i]
            val sumSq = a * a + b * b
            if (sumSq > 9f) {
                val tau = 3f / kotlin.math.sqrt(sumSq)
                tangent[i] = tau * a * secant[i]
                tangent[i + 1] = tau * b * secant[i]
            }
        }
    }

    for (i in 0 until n - 1) {
        val h = dx[i] / 3f
        val cp1x = points[i].x + h
        val cp1y = points[i].y + h * tangent[i]
        val cp2x = points[i + 1].x - h
        val cp2y = points[i + 1].y - h * tangent[i + 1]
        path.cubicTo(cp1x, cp1y, cp2x, cp2y, points[i + 1].x, points[i + 1].y)
    }
    return path
}

private fun niceDomainMax(rawMax: Long): Long {
    if (rawMax <= 0L) return 100_000L
    val exponent = log10(rawMax.toDouble()).toInt()
    val magnitude = 10.0.pow(exponent)
    val fraction = rawMax / magnitude
    val niceFraction = when {
        fraction <= 1.0 -> 1.0
        fraction <= 1.5 -> 1.5
        fraction <= 2.0 -> 2.0
        fraction <= 2.5 -> 2.5
        fraction <= 3.0 -> 3.0
        fraction <= 4.0 -> 4.0
        fraction <= 5.0 -> 5.0
        fraction <= 6.0 -> 6.0
        fraction <= 8.0 -> 8.0
        else -> 10.0
    }
    return (niceFraction * magnitude).toLong().coerceAtLeast(rawMax)
}

