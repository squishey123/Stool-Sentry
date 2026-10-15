package com.squishey.stoolsentry.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.squishey.stoolsentry.BuildConfig
import com.squishey.stoolsentry.R
import com.squishey.stoolsentry.domain.StoolColor
import com.squishey.stoolsentry.ui.theme.EyebrowStyle
import com.squishey.stoolsentry.ui.theme.Throne

/** Espresso backdrop with a warm glow from the top edge. */
fun Modifier.throneBackground(): Modifier = drawBehind {
    drawRect(Throne.Espresso)
    drawRect(
        Brush.radialGradient(
            colors = listOf(Throne.EspressoGlow, Color.Transparent),
            center = Offset(size.width * 0.5f, -size.width * 0.2f),
            radius = size.width * 1.25f,
        ),
    )
}

val GoldGradient: Brush
    get() = Brush.verticalGradient(listOf(Throne.GoldLight, Throne.Gold, Throne.GoldDim))

@Composable
fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Throne.Gold,
) {
    Text(
        text = text.uppercase(),
        style = EyebrowStyle,
        color = color,
        modifier = modifier.semantics { heading() },
    )
}

@Composable
fun ThroneCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    selected: Boolean = false,
    selectable: Boolean = false,
    shape: Shape = MaterialTheme.shapes.large,
    color: Color = Throne.Card,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    onClickLabel: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val borderColor by animateColorAsState(
        if (selected) Throne.Gold else Throne.OutlineSoft,
        label = "cardBorder",
    )
    val borderWidth by animateDpAsState(if (selected) 2.dp else 1.dp, label = "cardBorderWidth")
    val fill = remember(color, selected) {
        val base = if (selected) lerp(color, Throne.Gold, 0.08f) else color
        Brush.verticalGradient(listOf(lerp(base, Color.White, 0.035f), base))
    }
    val interaction = when {
        onClick == null -> Modifier
        selectable -> Modifier.selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
        else -> Modifier.clickable(onClickLabel = onClickLabel, role = Role.Button, onClick = onClick)
    }
    Column(
        modifier = modifier
            .clip(shape)
            .background(fill)
            .border(borderWidth, borderColor, shape)
            .then(interaction)
            .padding(contentPadding),
        content = content,
    )
}

@Composable
fun IconAction(
    @DrawableRes icon: Int,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = Throne.Porcelain,
    enabled: Boolean = true,
) {
    IconButton(onClick = onClick, enabled = enabled, modifier = modifier) {
        Icon(
            painter = painterResource(icon),
            contentDescription = contentDescription,
            tint = if (enabled) tint else tint.copy(alpha = 0.28f),
        )
    }
}

@Composable
fun GoldButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    @DrawableRes icon: Int? = null,
    height: Dp = 52.dp,
) {
    val background = if (enabled) GoldGradient else SolidColor(Throne.CardRaised)
    val content = if (enabled) Throne.OnGold else Throne.TextFaint
    Row(
        modifier = modifier
            .heightIn(min = height)
            .clip(CircleShape)
            .background(background)
            .clickable(enabled = enabled, role = Role.Button) { if (!loading) onClick() }
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompositionLocalProvider(LocalContentColor provides content) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = content,
                )
            } else {
                if (icon != null) {
                    Icon(painterResource(icon), contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                }
                Text(text, style = MaterialTheme.typography.labelLarge, color = content)
            }
        }
    }
}

@Composable
fun QuietButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = Throne.Porcelain,
    enabled: Boolean = true,
) {
    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .widthIn(min = 72.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = if (enabled) color else color.copy(alpha = 0.3f),
        )
    }
}

@Composable
fun ChoicePill(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val background by animateColorAsState(
        if (selected) Throne.Gold else Color.Transparent,
        label = "pillBackground",
    )
    val foreground by animateColorAsState(
        if (selected) Throne.OnGold else Throne.TextMuted,
        label = "pillForeground",
    )
    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .heightIn(min = 40.dp)
            .clip(CircleShape)
            .background(background)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = foreground, maxLines = 1)
    }
}

@Composable
fun ThroneTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    leading: @Composable (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .heightIn(min = 64.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconAction(R.drawable.ic_arrow_back, stringResource(R.string.action_back), onBack)
        } else {
            Spacer(Modifier.width(12.dp))
        }
        if (leading != null) {
            leading()
            Spacer(Modifier.width(10.dp))
        }
        Column(
            Modifier
                .weight(1f)
                .padding(end = 8.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = Throne.Porcelain,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() },
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Throne.TextMuted,
                    maxLines = 1,
                )
            }
        }
        actions()
    }
}

/** Screen content above a footer strip that holds the version label, so nothing scrolls under it. */
@Composable
fun ScreenBody(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Column(modifier.fillMaxSize()) {
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth(),
            content = content,
        )
        VersionTag(Modifier.padding(top = 6.dp))
    }
}

@Composable
fun VersionTag(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(R.string.version_label, BuildConfig.VERSION_NAME),
        style = MaterialTheme.typography.labelSmall.copy(
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.06.em,
        ),
        color = Throne.TextFaint,
        modifier = modifier.padding(start = 16.dp, bottom = 10.dp),
    )
}

@Composable
fun BrandMark(
    modifier: Modifier = Modifier,
    width: Dp = 32.dp,
    glow: Boolean = false,
) {
    val glowModifier = if (glow) {
        Modifier.drawBehind {
            drawCircle(
                Brush.radialGradient(
                    listOf(Throne.Gold.copy(alpha = 0.22f), Color.Transparent),
                    center = center,
                    radius = size.maxDimension * 0.95f,
                ),
                radius = size.maxDimension * 0.95f,
            )
        }
    } else {
        Modifier
    }
    val painter = painterResource(R.drawable.ic_brand_logs)
    val intrinsic = painter.intrinsicSize
    val aspect = if (intrinsic.width > 0f && intrinsic.height > 0f) intrinsic.height / intrinsic.width else 1f
    Image(
        painter = painter,
        contentDescription = null,
        modifier = modifier
            .then(glowModifier)
            .size(width = width, height = width * aspect),
    )
}

@Composable
fun ColorDot(
    color: StoolColor,
    modifier: Modifier = Modifier,
    size: Dp = 14.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(Color(color.swatchArgb))
            .border(1.dp, Throne.Porcelain.copy(alpha = 0.22f), CircleShape),
    )
}

@Composable
fun SmallChip(
    text: String,
    modifier: Modifier = Modifier,
    tone: Color = Throne.TextMuted,
    background: Color = Throne.CardRaised,
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(background)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium, color = tone)
    }
}

@Composable
fun StatTile(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    unit: String? = null,
) {
    ThroneCard(modifier = modifier, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Throne.TextMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                style = MaterialTheme.typography.displaySmall,
                color = Throne.Porcelain,
                maxLines = 1,
            )
            if (unit != null) {
                Spacer(Modifier.width(6.dp))
                Text(
                    text = unit,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Throne.TextMuted,
                    modifier = Modifier.padding(bottom = 7.dp),
                )
            }
        }
    }
}

/** Labelled bar that grows to [fraction] when it first appears. */
@Composable
fun ShareRow(
    label: String,
    value: String,
    fraction: Float,
    modifier: Modifier = Modifier,
    caption: String? = null,
    leading: @Composable (() -> Unit)? = null,
) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(fraction) {
        progress.animateTo(fraction.coerceIn(0f, 1f), tween(700, easing = FastOutSlowInEasing))
    }
    Column(modifier.fillMaxWidth().padding(vertical = 7.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (leading != null) {
                leading()
                Spacer(Modifier.width(10.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.bodyMedium, color = Throne.Porcelain)
                if (caption != null) {
                    Text(caption, style = MaterialTheme.typography.bodySmall, color = Throne.TextFaint)
                }
            }
            Spacer(Modifier.width(8.dp))
            Text(value, style = MaterialTheme.typography.labelMedium, color = Throne.TextMuted)
        }
        Spacer(Modifier.height(7.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(CircleShape)
                .background(Throne.CardRaised),
        ) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(progress.value)
                    .clip(CircleShape)
                    .background(Brush.horizontalGradient(listOf(Throne.GoldDim, Throne.Gold, Throne.GoldLight))),
            )
        }
    }
}

@Composable
fun EmptyState(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    action: @Composable (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BrandMark(width = 76.dp, glow = true)
        Spacer(Modifier.height(20.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = Throne.Porcelain,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = Throne.TextMuted,
            textAlign = TextAlign.Center,
        )
        if (action != null) {
            Spacer(Modifier.height(24.dp))
            action()
        }
    }
}

@Composable
fun CardDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(modifier = modifier, thickness = 1.dp, color = Throne.OutlineSoft.copy(alpha = 0.7f))
}

@Composable
fun CheckBadge(modifier: Modifier = Modifier, size: Dp = 22.dp) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(GoldGradient)
            .border(2.dp, Throne.Card, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_check),
            contentDescription = null,
            tint = Throne.OnGold,
            modifier = Modifier.size(size * 0.62f),
        )
    }
}

val PillShape = RoundedCornerShape(50)
