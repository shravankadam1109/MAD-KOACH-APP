package com.koach.studio.core.designsystem.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Snackbar host that shows failures in the error colours, so a failed save or delete can't be mistaken
 * for a success message. Pass [isError] for the message currently being shown.
 */
@Composable
fun KoachSnackbarHost(hostState: SnackbarHostState, isError: Boolean, modifier: Modifier = Modifier) {
    SnackbarHost(hostState, modifier) { data ->
        if (isError) {
            Snackbar(
                snackbarData = data,
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            )
        } else {
            Snackbar(snackbarData = data)
        }
    }
}