package com.koach.studio.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.koach.studio.core.designsystem.theme.KoachRadius
import com.koach.studio.core.designsystem.theme.KoachTheme

@Composable
fun koachTextFieldColors(): TextFieldColors {
    val c = KoachTheme.colors
    return OutlinedTextFieldDefaults.colors(
        focusedTextColor = c.textPrimary,
        unfocusedTextColor = c.textPrimary,
        disabledTextColor = c.textTertiary,
        focusedContainerColor = c.surface,
        unfocusedContainerColor = c.surface,
        disabledContainerColor = c.surface,
        errorContainerColor = c.surface,
        cursorColor = c.brand,
        focusedBorderColor = c.brand,
        unfocusedBorderColor = Color.White.copy(alpha = 0.09f),
        disabledBorderColor = Color.White.copy(alpha = 0.05f),
        errorBorderColor = c.danger,
        focusedLabelColor = c.brand,
        unfocusedLabelColor = c.textSecondary,
        errorLabelColor = c.danger,
        focusedLeadingIconColor = c.brand,
        unfocusedLeadingIconColor = c.textTertiary,
        focusedTrailingIconColor = c.textSecondary,
        unfocusedTrailingIconColor = c.textTertiary,
        focusedPlaceholderColor = c.textTertiary,
        unfocusedPlaceholderColor = c.textTertiary,
        errorSupportingTextColor = c.danger,
        focusedSupportingTextColor = c.textTertiary,
        unfocusedSupportingTextColor = c.textTertiary
    )
}

/**
 * Form field with label, inline error and optional character counter.
 * Errors are announced to accessibility services via semantics.
 */
@Composable
fun KoachTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    error: String? = null,
    helper: String? = null,
    maxLength: Int? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailing: (@Composable () -> Unit)? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = { if (maxLength == null || it.length <= maxLength) onValueChange(it) },
        label = { Text(label) },
        placeholder = placeholder?.let { { Text(it) } },
        leadingIcon = leadingIcon?.let { { Icon(it, contentDescription = null) } },
        trailingIcon = trailing,
        isError = error != null,
        supportingText = when {
            error != null -> { { Text(error) } }
            helper != null || maxLength != null -> {
                {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(helper.orEmpty())
                        if (maxLength != null) Text("${value.length}/$maxLength")
                    }
                }
            }
            else -> null
        },
        singleLine = singleLine,
        minLines = minLines,
        enabled = enabled,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        visualTransformation = visualTransformation,
        shape = RoundedCornerShape(KoachRadius.md),
        colors = koachTextFieldColors(),
        textStyle = MaterialTheme.typography.bodyLarge,
        modifier = modifier
            .fillMaxWidth()
            .then(if (error != null) Modifier.semantics { this.error(error) } else Modifier)
    )
}

@Composable
fun KoachSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(placeholder) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = if (query.isNotEmpty()) {
            {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = "Clear search")
                }
            }
        } else null,
        singleLine = true,
        shape = RoundedCornerShape(50),
        colors = koachTextFieldColors(),
        modifier = modifier.fillMaxWidth()
    )
}

/** Read-only dropdown for picking one option from an enum or list. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> KoachDropdownField(
    label: String,
    options: List<T>,
    selected: T?,
    onSelected: (T) -> Unit,
    optionLabel: (T) -> String,
    modifier: Modifier = Modifier,
    error: String? = null,
    placeholder: String = "Select"
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selected?.let(optionLabel) ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            isError = error != null,
            supportingText = error?.let { { Text(it) } },
            shape = RoundedCornerShape(KoachRadius.md),
            colors = koachTextFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = KoachTheme.colors.surfaceRaised
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

/** Horizontally scrolling single-select chips. A null option represents "All". */
@Composable
fun <T> KoachFilterChips(
    options: List<T>,
    selected: T?,
    onSelected: (T?) -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier,
    allLabel: String? = "All"
) {
    val c = KoachTheme.colors
    val chipColors = FilterChipDefaults.filterChipColors(
        containerColor = Color.Transparent,
        labelColor = c.textSecondary,
        selectedContainerColor = c.brand,
        selectedLabelColor = c.onBrand
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = KoachTheme.spacing.gutter)
    ) {
        if (allLabel != null) {
            FilterChip(
                selected = selected == null,
                onClick = { onSelected(null) },
                label = { Text(allLabel) },
                shape = RoundedCornerShape(50),
                colors = chipColors,
                modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true, selected = selected == null,
                    borderColor = Color.White.copy(alpha = 0.12f), selectedBorderColor = c.brand
                )
            )
        }
        options.forEach { option ->
            val isSelected = option == selected
            FilterChip(
                selected = isSelected,
                onClick = { onSelected(if (isSelected && allLabel != null) null else option) },
                label = { Text(label(option)) },
                shape = RoundedCornerShape(50),
                colors = chipColors,
                modifier = Modifier.defaultMinSize(minHeight = 48.dp),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true, selected = isSelected,
                    borderColor = Color.White.copy(alpha = 0.12f), selectedBorderColor = c.brand
                )
            )
        }
    }
}

/** Vertical stack of form fields with consistent spacing. */
@Composable
fun FormSection(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) { content() }
}

/** Pill-shaped segmented switch for 2–3 peer views, e.g. Services | Work. */
@Composable
fun KoachSegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val c = KoachTheme.colors
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(c.surface)
            .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(50))
            .padding(4.dp)
    ) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 48.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (selected) c.brand else Color.Transparent)
                    .selectable(selected = selected, onClick = { onSelect(index) }, role = Role.Tab)
                    .padding(vertical = 10.dp)
                    .testTag("segment_$index")
            ) {
                Text(label, style = MaterialTheme.typography.labelLarge, color = if (selected) c.onBrand else c.textSecondary)
            }
        }
    }
}
