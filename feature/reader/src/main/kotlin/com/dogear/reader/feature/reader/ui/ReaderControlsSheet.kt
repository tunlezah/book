package com.dogear.reader.feature.reader.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dogear.reader.core.model.BrightnessMode
import com.dogear.reader.core.model.FontChoice
import com.dogear.reader.core.model.KeepAwakeMode
import com.dogear.reader.core.model.OrientationLock
import com.dogear.reader.core.model.PageAnimation
import com.dogear.reader.core.model.ReaderSettings
import com.dogear.reader.core.model.ReaderTextAlign
import com.dogear.reader.core.model.ReadingThemeId
import kotlin.math.roundToInt

/**
 * Bottom-sheet reading controls. Every change calls back immediately and is persisted, so the
 * open book re-renders live (Phase 4). Less-common controls sit lower to keep the top compact.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReaderControlsSheet(
    settings: ReaderSettings,
    font: FontChoice,
    onUpdate: ((ReaderSettings) -> ReaderSettings) -> Unit,
    onFontChange: (FontChoice) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
        ) {
            Section("Theme")
            ChipRow(
                items = ReadingThemeId.entries,
                selected = settings.themeId,
                label = { it.displayName },
                onSelect = { id -> onUpdate { it.copy(themeId = id) } },
            )

            Section("Font")
            ChipRow(
                items = FontChoice.entries.filter { it != FontChoice.SYSTEM },
                selected = font,
                label = { it.displayName },
                onSelect = onFontChange,
            )

            Stepper(
                label = "Font size",
                value = "${settings.fontSizeSp} sp",
                onDecrease = {
                    onUpdate { it.copy(fontSizeSp = (it.fontSizeSp - 1).coerceAtLeast(ReaderSettings.MIN_FONT_SP)) }
                },
                onIncrease = {
                    onUpdate { it.copy(fontSizeSp = (it.fontSizeSp + 1).coerceAtMost(ReaderSettings.MAX_FONT_SP)) }
                },
            )

            Section("Weight")
            ChipRow(
                items = WEIGHTS,
                selected = WEIGHTS.firstOrNull { it.second == settings.fontWeight } ?: WEIGHTS[1],
                label = { it.first },
                onSelect = { weight -> onUpdate { it.copy(fontWeight = weight.second) } },
            )

            LabeledSlider(
                label = "Line spacing",
                value = settings.lineHeight,
                range = 1.0f..2.2f,
                valueText = "%.1f".format(settings.lineHeight),
                onChange = { v -> onUpdate { it.copy(lineHeight = v) } },
            )
            LabeledSlider(
                label = "Paragraph spacing",
                value = settings.paragraphSpacingEm,
                range = 0f..2f,
                valueText = "%.1f".format(settings.paragraphSpacingEm),
                onChange = { v -> onUpdate { it.copy(paragraphSpacingEm = v) } },
            )
            LabeledSlider(
                label = "Margins",
                value = settings.marginHorizontalDp.toFloat(),
                range = 0f..64f,
                valueText = "${settings.marginHorizontalDp} dp",
                onChange = { v -> onUpdate { it.copy(marginHorizontalDp = v.roundToInt()) } },
            )

            Section("Alignment")
            ChipRow(
                items = ReaderTextAlign.entries,
                selected = settings.textAlign,
                label = { it.displayName },
                onSelect = { align -> onUpdate { it.copy(textAlign = align) } },
            )

            ToggleRow(
                label = "Hyphenation",
                checked = settings.hyphenation,
                onChange = { on -> onUpdate { it.copy(hyphenation = on) } },
            )

            Section("Page animation")
            ChipRow(
                items = PageAnimation.entries,
                selected = settings.pageAnimation,
                label = { it.displayName },
                onSelect = { anim -> onUpdate { it.copy(pageAnimation = anim) } },
            )

            Section("Brightness")
            ChipRow(
                items = BrightnessMode.entries,
                selected = settings.brightnessMode,
                label = { it.displayName },
                onSelect = { mode -> onUpdate { it.copy(brightnessMode = mode) } },
            )
            if (settings.brightnessMode == BrightnessMode.READER) {
                LabeledSlider(
                    label = "Level",
                    value = settings.brightnessLevel,
                    range = 0.05f..1f,
                    valueText = "${(settings.brightnessLevel * 100).roundToInt()}%",
                    onChange = { v -> onUpdate { it.copy(brightnessLevel = v) } },
                )
            }

            Section("Keep screen awake")
            ChipRow(
                items = KeepAwakeMode.entries,
                selected = settings.keepAwake,
                label = { it.displayName },
                onSelect = { mode -> onUpdate { it.copy(keepAwake = mode) } },
            )

            Section("Orientation")
            ChipRow(
                items = OrientationLock.entries,
                selected = settings.orientation,
                label = { it.displayName },
                onSelect = { lock -> onUpdate { it.copy(orientation = lock) } },
            )

            ToggleRow(
                label = "Volume keys turn pages",
                checked = settings.volumeKeyPaging,
                onChange = { on -> onUpdate { it.copy(volumeKeyPaging = on) } },
            )
        }
    }
}

private val WEIGHTS = listOf("Light" to 300, "Regular" to 400, "Medium" to 500, "Bold" to 700)

@Composable
private fun Section(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(top = 16.dp, bottom = 6.dp),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> ChipRow(
    items: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items.forEach { item ->
            FilterChip(
                selected = item == selected,
                onClick = { onSelect(item) },
                label = { Text(label(item)) },
            )
        }
    }
}

@Composable
private fun Stepper(label: String, value: String, onDecrease: () -> Unit, onIncrease: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        IconButton(onClick = onDecrease) { Icon(Icons.Filled.Remove, contentDescription = "Decrease") }
        Text(value, style = MaterialTheme.typography.bodyMedium)
        IconButton(onClick = onIncrease) { Icon(Icons.Filled.Add, contentDescription = "Increase") }
    }
}

@Composable
private fun LabeledSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    valueText: String,
    onChange: (Float) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text(valueText, style = MaterialTheme.typography.bodyMedium)
        }
        Slider(value = value, onValueChange = onChange, valueRange = range)
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(8.dp))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
