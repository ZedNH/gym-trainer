package com.gymtrainer.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gymtrainer.app.domain.Difficulty
import com.gymtrainer.app.ui.theme.AppTheme
import com.gymtrainer.app.ui.theme.Brand

/** Horizontal page gutter used on every screen. */
val Gutter = 20.dp

fun levelColor(level: Difficulty): Color = when (level) {
    Difficulty.BEGINNER -> Brand.Green
    Difficulty.INTERMEDIATE -> Brand.Amber
    Difficulty.ADVANCED -> Brand.Red
}

// ---------------- Headers ----------------

/** Large title at the top of a bottom-nav tab. */
@Composable
fun TabHeader(title: String, subtitle: String? = null, hint: String? = null, trailing: (@Composable RowScope.() -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().padding(start = Gutter, end = Gutter, top = 12.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.labelMedium, color = AppTheme.colors.textMuted)
            Text(title, style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onBackground)
            // One plain sentence saying what this tab is for.
            if (hint != null) Text(hint, style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.textMuted)
        }
        trailing?.invoke(this)
    }
}

/** Compact bar for pushed screens: back arrow, centered-left title, optional actions. */
@Composable
fun DetailTopBar(title: String = "", onBack: () -> Unit, actions: @Composable RowScope.() -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().statusBarsPadding().height(56.dp).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircleIconButton(Icons.AutoMirrored.Filled.ArrowBack, "Back", onBack)
        Text(
            title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
        )
        actions()
    }
}

@Composable
fun CircleIconButton(icon: ImageVector, description: String, onClick: () -> Unit, tint: Color = MaterialTheme.colorScheme.onSurface, background: Color = AppTheme.colors.card) {
    Box(
        Modifier.size(42.dp).clip(CircleShape).background(background)
            .border(1.dp, AppTheme.colors.border, CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) { Icon(icon, contentDescription = description, tint = tint, modifier = Modifier.size(22.dp)) }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(modifier.fillMaxWidth().padding(horizontal = Gutter), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.weight(1f))
        if (action != null && onAction != null) {
            Text(
                action, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onAction).padding(6.dp)
            )
        }
    }
}

// ---------------- Cards ----------------

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    padding: PaddingValues = PaddingValues(16.dp),
    color: Color = AppTheme.colors.card,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier.fillMaxWidth().clip(shape).background(color)
            .border(1.dp, AppTheme.colors.border, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(padding),
        content = content
    )
}

/** Backwards-compatible alias. */
@Composable
fun SectionCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) = AppCard(modifier) { content() }

/** Gradient hero card with a big faded icon. */
@Composable
fun HeroCard(
    colors: List<Color>,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val shape = RoundedCornerShape(24.dp)
    Box(
        modifier.clip(shape).background(Brush.linearGradient(colors))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        if (icon != null) {
            // Decorative watermark, pushed partly off the card so it never fights the text.
            Icon(
                icon, contentDescription = null, tint = Color.White.copy(alpha = 0.12f),
                modifier = Modifier.size(120.dp).align(Alignment.BottomEnd).offset(x = 28.dp, y = 28.dp)
            )
        }
        Box(Modifier.fillMaxWidth().padding(20.dp), content = content)
    }
}

// ---------------- Buttons ----------------

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Button(
        onClick = onClick, enabled = enabled, modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = Color.White,
            disabledContainerColor = AppTheme.colors.border, disabledContentColor = AppTheme.colors.textFaint),
        elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp)
    ) {
        if (icon != null) { Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)) }
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun SecondaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null, enabled: Boolean = true) {
    Button(
        onClick = onClick, enabled = enabled, modifier = modifier.height(56.dp), shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.primary),
        elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp)
    ) {
        if (icon != null) { Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)) }
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

/** Small pill button, e.g. "Start" on a list card. */
@Composable
fun PillButton(text: String, onClick: () -> Unit, icon: ImageVector? = null, color: Color = MaterialTheme.colorScheme.primary, enabled: Boolean = true) {
    Row(
        Modifier.height(38.dp).clip(RoundedCornerShape(50))
            .background(if (enabled) color else AppTheme.colors.border)
            .clickable(enabled = enabled, onClick = onClick).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) { Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(4.dp)) }
        Text(text, style = MaterialTheme.typography.labelMedium, color = Color.White)
    }
}

// ---------------- Small pieces ----------------

@Composable
fun Tag(text: String, color: Color = MaterialTheme.colorScheme.primary, modifier: Modifier = Modifier) {
    Text(
        text, style = MaterialTheme.typography.labelSmall, color = color,
        modifier = modifier.clip(RoundedCornerShape(50)).background(color.copy(alpha = 0.12f)).padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

@Composable
fun LevelBadge(level: Difficulty, modifier: Modifier = Modifier) = Tag(level.label, levelColor(level), modifier)

/** Backwards-compatible neutral pill. */
@Composable
fun Pill(text: String, modifier: Modifier = Modifier) = Tag(text, AppTheme.colors.textMuted, modifier.padding(end = 6.dp, bottom = 6.dp))

@Composable
fun IconBadge(icon: ImageVector, tint: Color, size: Dp = 44.dp, modifier: Modifier = Modifier) {
    Box(
        modifier.size(size).clip(RoundedCornerShape(size / 3.2f)).background(tint.copy(alpha = 0.13f)),
        contentAlignment = Alignment.Center
    ) { Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.52f)) }
}

@Composable
fun StatBlock(value: String, label: String, modifier: Modifier = Modifier, icon: ImageVector? = null, tint: Color = MaterialTheme.colorScheme.primary) {
    AppCard(modifier, padding = PaddingValues(14.dp)) {
        if (icon != null) { IconBadge(icon, tint, 34.dp); Spacer(Modifier.height(10.dp)) }
        Text(value, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
        Text(label, style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.textMuted, maxLines = 1)
    }
}

/** Backwards-compatible alias for older screens. */
@Composable
fun StatTile(label: String, value: String, modifier: Modifier = Modifier, accent: Color = MaterialTheme.colorScheme.primary) =
    StatBlock(value, label, modifier, tint = accent)

@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    stroke: Dp = 7.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    track: Color = AppTheme.colors.cardAlt,
    content: @Composable BoxScope.() -> Unit = {}
) {
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val s = stroke.toPx()
            val arc = Size(this.size.width - s, this.size.height - s)
            val tl = Offset(s / 2, s / 2)
            drawArc(track, 0f, 360f, false, tl, arc, style = Stroke(s))
            drawArc(color, -90f, 360f * progress.coerceIn(0f, 1f), false, tl, arc, style = Stroke(s, cap = StrokeCap.Round))
        }
        content()
    }
}

@Composable
fun LinearMeter(progress: Float, modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.primary, track: Color = AppTheme.colors.cardAlt, height: Dp = 8.dp) {
    Box(modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(50)).background(track)) {
        Box(Modifier.fillMaxWidth(progress.coerceIn(0f, 1f)).height(height).clip(RoundedCornerShape(50)).background(color))
    }
}

@Composable
fun EmptyState(icon: ImageVector, title: String, message: String, modifier: Modifier = Modifier, action: String? = null, onAction: (() -> Unit)? = null) {
    AppCard(modifier, padding = PaddingValues(24.dp)) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            IconBadge(icon, MaterialTheme.colorScheme.primary, 56.dp)
            Spacer(Modifier.height(12.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(4.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.textMuted, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            if (action != null && onAction != null) {
                Spacer(Modifier.height(14.dp))
                PillButton(action, onAction)
            }
        }
    }
}

/** iOS-style segmented control. */
@Composable
fun <T> SegmentedTabs(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(AppTheme.colors.cardAlt).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEach { (value, label) ->
            val isSel = value == selected
            val bg by animateColorAsState(if (isSel) AppTheme.colors.card else Color.Transparent, label = "seg")
            Box(
                Modifier.weight(1f).height(42.dp).clip(RoundedCornerShape(12.dp)).background(bg)
                    .clickable { onSelect(value) },
                contentAlignment = Alignment.Center
            ) {
                Text(label, style = MaterialTheme.typography.labelLarge,
                    color = if (isSel) MaterialTheme.colorScheme.onSurface else AppTheme.colors.textMuted)
            }
        }
    }
}

/** Rounded selectable chip used for filters. */
@Composable
fun FilterPill(text: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.clip(shape)
            .background(if (selected) MaterialTheme.colorScheme.onSurface else AppTheme.colors.card)
            .border(1.dp, if (selected) Color.Transparent else AppTheme.colors.border, shape)
            .clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 9.dp)
    )
}

/** Big selectable option card (onboarding, settings). */
@Composable
fun ChoiceCard(
    title: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    tint: Color = MaterialTheme.colorScheme.primary,
    emoji: String? = null
) {
    val shape = RoundedCornerShape(20.dp)
    val border by animateColorAsState(if (selected) MaterialTheme.colorScheme.primary else AppTheme.colors.border, label = "cb")
    Row(
        modifier.fillMaxWidth().clip(shape)
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = if (AppTheme.colors.isDark) 0.5f else 0.6f) else AppTheme.colors.card)
            .border(if (selected) 2.dp else 1.dp, border, shape)
            .clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when {
            emoji != null -> Box(Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(AppTheme.colors.cardAlt), contentAlignment = Alignment.Center) {
                Text(emoji, style = MaterialTheme.typography.headlineSmall)
            }
            icon != null -> IconBadge(icon, tint, 46.dp)
        }
        Column(Modifier.weight(1f).padding(horizontal = if (icon != null || emoji != null) 14.dp else 0.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            if (subtitle != null) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.textMuted)
        }
        Box(
            Modifier.size(24.dp).clip(CircleShape)
                .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
                .border(2.dp, if (selected) MaterialTheme.colorScheme.primary else AppTheme.colors.border, CircleShape),
            contentAlignment = Alignment.Center
        ) { if (selected) Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp)) }
    }
}

@Composable
fun SettingRow(icon: ImageVector, tint: Color, title: String, value: String? = null, onClick: (() -> Unit)? = null, trailing: (@Composable () -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBadge(icon, tint, 38.dp)
        Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f).padding(horizontal = 14.dp))
        if (value != null) Text(value, style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.textMuted)
        if (trailing != null) trailing()
        else if (onClick != null) Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = AppTheme.colors.textFaint)
    }
}

@Composable
fun AppSwitch(checked: Boolean, onChange: (Boolean) -> Unit) = Switch(
    checked = checked, onCheckedChange = onChange,
    colors = SwitchDefaults.colors(
        checkedThumbColor = Color.White, checkedTrackColor = MaterialTheme.colorScheme.primary,
        uncheckedThumbColor = Color.White, uncheckedTrackColor = AppTheme.colors.border, uncheckedBorderColor = Color.Transparent
    )
)

@Composable
fun RowDivider() = Box(Modifier.fillMaxWidth().padding(start = 68.dp).height(1.dp).background(AppTheme.colors.border))

/** Standard list row: leading slot, title + meta, trailing chevron. */
@Composable
fun ListRow(
    title: String,
    meta: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: @Composable () -> Unit,
    badge: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    AppCard(modifier, onClick = onClick, padding = PaddingValues(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            leading()
            Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                Text(meta, style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.textMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (badge != null) { Spacer(Modifier.height(6.dp)); badge() }
            }
            if (trailing != null) trailing() else Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = AppTheme.colors.textFaint)
        }
    }
}

// ---------------- Charts ----------------

@Composable
fun LineChart(values: List<Float>, modifier: Modifier = Modifier, lineColor: Color = MaterialTheme.colorScheme.primary) {
    if (values.size < 2) {
        Box(modifier.fillMaxWidth().height(110.dp), contentAlignment = Alignment.Center) {
            Text("Log your weight a few times to see the trend", style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.textMuted)
        }
        return
    }
    val grid = AppTheme.colors.border
    Canvas(modifier.fillMaxWidth().height(140.dp).padding(top = 8.dp)) {
        val maxV = values.max(); val minV = values.min()
        val range = (maxV - minV).takeIf { it > 0f } ?: 1f
        val stepX = size.width / (values.size - 1)
        fun y(v: Float) = size.height - ((v - minV) / range) * size.height * 0.85f - size.height * 0.075f
        for (i in 0..3) { val gy = size.height * i / 3f; drawLine(grid, Offset(0f, gy), Offset(size.width, gy), 1.5f) }
        val path = Path(); val fill = Path()
        values.forEachIndexed { i, v ->
            val x = stepX * i
            if (i == 0) { path.moveTo(x, y(v)); fill.moveTo(x, size.height); fill.lineTo(x, y(v)) } else { path.lineTo(x, y(v)); fill.lineTo(x, y(v)) }
        }
        fill.lineTo(size.width, size.height); fill.close()
        drawPath(fill, Brush.verticalGradient(listOf(lineColor.copy(alpha = 0.25f), Color.Transparent)))
        drawPath(path, lineColor, style = Stroke(width = 6f, cap = StrokeCap.Round))
        drawCircle(lineColor, 9f, Offset(stepX * (values.size - 1), y(values.last())))
    }
}

@Composable
fun BarChart(data: List<Pair<String, Float>>, modifier: Modifier = Modifier, barColor: Color = MaterialTheme.colorScheme.primary) {
    if (data.isEmpty()) {
        Box(modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
            Text("Log gym sets to see your weekly volume", style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.textMuted)
        }
        return
    }
    val maxV = data.maxOf { it.second }.takeIf { it > 0f } ?: 1f
    val track = AppTheme.colors.cardAlt
    Column(modifier.fillMaxWidth()) {
        Canvas(Modifier.fillMaxWidth().height(130.dp)) {
            val slot = size.width / data.size
            val w = (slot * 0.6f).coerceAtMost(44.dp.toPx())
            data.forEachIndexed { i, (_, v) ->
                val x = i * slot + (slot - w) / 2
                drawRoundRect(track, Offset(x, 0f), Size(w, size.height), CornerRadius(14f, 14f))
                val h = (v / maxV) * size.height
                drawRoundRect(barColor, Offset(x, size.height - h), Size(w, h), CornerRadius(14f, 14f))
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
            data.forEach { (label, v) ->
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(v.toInt().toString(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
                    Text(label, style = MaterialTheme.typography.labelSmall, color = AppTheme.colors.textMuted, maxLines = 1)
                }
            }
        }
    }
}

/** Surface for bottom-pinned actions. */
@Composable
fun BottomActionBar(content: @Composable RowScope.() -> Unit) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = Gutter, vertical = 12.dp)
                .then(Modifier.padding(bottom = 0.dp)),
            horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically, content = content
        )
    }
}

@Suppress("unused")
private val unusedBorder = BorderStroke(1.dp, Color.Transparent)
