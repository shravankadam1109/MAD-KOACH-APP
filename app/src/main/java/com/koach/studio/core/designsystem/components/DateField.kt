package com.koach.studio.core.designsystem.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.koach.studio.core.designsystem.theme.KoachTheme
import com.koach.studio.core.ui.formatDate
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Read-only field showing a date; tapping it opens a branded date picker
 * limited to [earliest]..[latest] when given.
 */
@Composable
fun KoachDateField(
    label: String,
    value: LocalDate?,
    onValueChange: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null,
    helper: String? = null,
    earliest: LocalDate? = null,
    latest: LocalDate? = null,
    fieldModifier: Modifier = Modifier
) {
    var open by rememberSaveable { mutableStateOf(false) }
    Box(modifier) {
        KoachTextField(
            value = value?.let { formatDate(it, LocalDate.MIN) }.orEmpty(),
            onValueChange = {},
            label = label,
            placeholder = "Choose a date",
            leadingIcon = Icons.Outlined.CalendarMonth,
            error = error,
            helper = helper
        )
        Box(
            Modifier
                .matchParentSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClickLabel = "Choose $label"
                ) { open = true }
                .then(fieldModifier)
        )
    }
    if (open) {
        KoachDatePickerDialog(
            initial = value ?: earliest ?: LocalDate.now(),
            earliest = earliest,
            latest = latest,
            onDismiss = { open = false },
            onConfirm = {
                onValueChange(it)
                open = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KoachDatePickerDialog(
    initial: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit,
    earliest: LocalDate? = null,
    latest: LocalDate? = null
) {
    val c = KoachTheme.colors
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial.toUtcMillis(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                val date = utcTimeMillis.toUtcLocalDate()
                return (earliest == null || !date.isBefore(earliest)) && (latest == null || !date.isAfter(latest))
            }
        }
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { state.selectedDateMillis?.let { onConfirm(it.toUtcLocalDate()) } },
                modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            ) {
                Text("Select", color = c.brand)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            ) {
                Text("Cancel", color = c.textSecondary)
            }
        },
        colors = DatePickerDefaults.colors(containerColor = c.surfaceRaised)
    ) {
        DatePicker(
            state = state,
            colors = DatePickerDefaults.colors(
                containerColor = c.surfaceRaised,
                selectedDayContainerColor = c.brand,
                selectedDayContentColor = c.onBrand,
                todayDateBorderColor = c.brand,
                todayContentColor = c.brand
            )
        )
    }
}

private fun LocalDate.toUtcMillis(): Long = atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
private fun Long.toUtcLocalDate(): LocalDate = Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
