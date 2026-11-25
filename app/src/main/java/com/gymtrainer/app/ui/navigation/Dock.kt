package com.gymtrainer.app.ui.navigation

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import kotlin.math.abs
import kotlin.math.roundToInt
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.gymtrainer.app.ui.components.Gutter
import com.gymtrainer.app.ui.components.IconBadge
import com.gymtrainer.app.ui.theme.AppTheme
import com.gymtrainer.app.ui.theme.Brand

data class DockTab(val route: String, val label: String, val icon: ImageVector)

/**
 * Floating pill dock with a raised centre action button.
 *
 * [position] is the pager's continuous position (e.g. 1.4 while swiping from tab 1 to 2): the
 * highlight pill slides with the finger and icon colours cross-fade, so tab changes feel physical.
 */
@Composable
fun FloatingDock(tabs: List<DockTab>, position: Float, onTab: (Int) -> Unit, onAction: () -> Unit) {
    val shape = RoundedCornerShape(28.dp)
    val density = LocalDensity.current
    // Where each tab's icon sits inside the dock (px): centre x and top y.
    val centers = remember(tabs.size) { mutableStateListOf<Float>().apply { repeat(tabs.size) { add(Float.NaN) } } }
    var iconTop by remember { mutableFloatStateOf(Float.NaN) }
    var dockOrigin by remember { mutableStateOf(Offset.Zero) }
    val pillW = 52.dp; val pillH = 30.dp

    Box(
        Modifier.fillMaxWidth()
            // Soft fade so content scrolling under the floating dock doesn't clash with it.
            .background(Brush.verticalGradient(listOf(Color.Transparent, MaterialTheme.colorScheme.background, MaterialTheme.colorScheme.background)))
            .navigationBarsPadding().padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 10.dp)
    ) {
        Box(
            Modifier.fillMaxWidth().height(68.dp)
                .shadow(18.dp, shape, ambientColor = Color(0x33101828), spotColor = Color(0x33101828))
                .clip(shape).background(AppTheme.colors.card).border(1.dp, AppTheme.colors.border, shape)
                .onGloballyPositioned { dockOrigin = it.positionInRoot() }
        ) {
            // Sliding highlight, drawn under the icons.
            val ready = centers.none { it.isNaN() } && !iconTop.isNaN()
            if (ready) {
                val p = position.coerceIn(0f, (tabs.size - 1).toFloat())
                val i = p.toInt().coerceAtMost(tabs.size - 2)
                val f = p - i
                val cx = centers[i] + (centers[i + 1] - centers[i]) * f
                val halfW = with(density) { pillW.toPx() } / 2
                Box(
                    Modifier.offset { IntOffset((cx - halfW).roundToInt(), iconTop.roundToInt()) }
                        .size(pillW, pillH).clip(RoundedCornerShape(15.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                )
            }
            Row(Modifier.fillMaxSize().padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                val half = tabs.size / 2
                tabs.forEachIndexed { index, tab ->
                    if (index == half) Spacer(Modifier.width(72.dp))
                    val t = (1f - abs(position - index)).coerceIn(0f, 1f)
                    DockItem(tab, t, Modifier.weight(1f), onPlaced = { cx, top ->
                        centers[index] = cx - dockOrigin.x
                        iconTop = top - dockOrigin.y
                    }) { onTab(index) }
                }
            }
        }
        val press = remember { MutableInteractionSource() }
        val pressed by press.collectIsPressedAsState()
        val scale by animateFloatAsState(if (pressed) 0.88f else 1f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessMedium), label = "fabScale")
        Box(
            Modifier.align(Alignment.TopCenter).offset(y = (-16).dp).size(62.dp)
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .shadow(14.dp, CircleShape, spotColor = Brand.Blue)
                .clip(CircleShape).background(Brush.linearGradient(listOf(Brand.Blue, Brand.Purple)))
                .border(4.dp, MaterialTheme.colorScheme.background, CircleShape)
                .clickable(interactionSource = press, indication = null, onClick = onAction),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Quick actions", tint = Color.White, modifier = Modifier.size(30.dp))
        }
    }
}

/** One dock tab. [t] is 1 when fully selected, 0 when not, and in between while swiping. */
@Composable
private fun DockItem(tab: DockTab, t: Float, modifier: Modifier, onPlaced: (centerX: Float, top: Float) -> Unit, onClick: () -> Unit) {
    val tint = lerp(AppTheme.colors.textFaint, MaterialTheme.colorScheme.primary, t)
    // Small spring "pop" when a tab becomes selected.
    val pop by animateFloatAsState(if (t > 0.5f) 1f else 0f, spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessMediumLow), label = "dockPop")
    Column(
        modifier.clip(RoundedCornerShape(20.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier.width(52.dp).height(30.dp).onGloballyPositioned {
                val p = it.positionInRoot()
                onPlaced(p.x + it.size.width / 2f, p.y)
            },
            contentAlignment = Alignment.Center
        ) {
            Icon(tab.icon, contentDescription = tab.label, tint = tint,
                modifier = Modifier.size(22.dp).graphicsLayer { val s = 1f + 0.12f * pop; scaleX = s; scaleY = s })
        }
        Spacer(Modifier.height(2.dp))
        Text(tab.label, style = MaterialTheme.typography.labelSmall, color = tint, maxLines = 1)
    }
}

data class QuickAction(val label: String, val sub: String, val icon: ImageVector, val tint: Color, val onClick: () -> Unit)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickActionSheet(onDismiss: () -> Unit, actions: List<QuickAction>) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = AppTheme.colors.card) {
        Column(Modifier.padding(horizontal = Gutter).navigationBarsPadding()) {
            Text("Quick actions", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(14.dp))
            actions.chunked(2).forEach { row ->
                Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { a ->
                        Row(
                            Modifier.weight(1f).clip(RoundedCornerShape(18.dp)).background(AppTheme.colors.cardAlt)
                                .clickable { onDismiss(); a.onClick() }.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconBadge(a.icon, a.tint, 40.dp)
                            Column(Modifier.padding(start = 10.dp)) {
                                Text(a.label, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
                                Text(a.sub, style = MaterialTheme.typography.bodySmall, color = AppTheme.colors.textMuted, maxLines = 1)
                            }
                        }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

/** Icons used by the default quick actions. */
object QuickIcons {
    val workout = Icons.Filled.Timer
    val meal = Icons.Filled.Restaurant
    val water = Icons.Filled.WaterDrop
    val weight = Icons.Filled.MonitorWeight
    val library = Icons.Filled.MenuBook
    val routine = Icons.Filled.Build
}
