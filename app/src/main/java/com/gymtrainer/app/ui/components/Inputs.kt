package com.gymtrainer.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.gymtrainer.app.ui.theme.AppTheme

@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    label: String? = null,
    keyboard: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true,
    minLines: Int = 1,
    leading: (@Composable () -> Unit)? = null
) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text(placeholder, color = AppTheme.colors.textFaint) },
        label = label?.let { { Text(it) } },
        singleLine = singleLine, minLines = minLines, leadingIcon = leading,
        textStyle = MaterialTheme.typography.bodyLarge,
        shape = RoundedCornerShape(16.dp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = AppTheme.colors.card, unfocusedContainerColor = AppTheme.colors.card,
            unfocusedBorderColor = AppTheme.colors.border, focusedBorderColor = MaterialTheme.colorScheme.primary
        )
    )
}

/** Big value with −/+ buttons and a slider — friendlier than typing numbers. */
@Composable
fun NumberPicker(
    label: String,
    valueText: String,
    unit: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    onSlide: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    AppCard(modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = AppTheme.colors.textMuted)
        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            CircleIconButton(Icons.Filled.Remove, "Decrease $label", onMinus, background = AppTheme.colors.cardAlt)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(valueText, style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.width(6.dp))
                Text(unit, style = MaterialTheme.typography.titleMedium, color = AppTheme.colors.textMuted, modifier = Modifier.padding(bottom = 6.dp))
            }
            CircleIconButton(Icons.Filled.Add, "Increase $label", onPlus, background = AppTheme.colors.cardAlt)
        }
        Slider(
            value = value.coerceIn(range.start, range.endInclusive), onValueChange = onSlide, valueRange = range,
            colors = SliderDefaults.colors(thumbColor = MaterialTheme.colorScheme.primary, activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = AppTheme.colors.cardAlt),
            modifier = Modifier.height(32.dp)
        )
    }
}
