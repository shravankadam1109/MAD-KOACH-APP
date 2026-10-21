package com.koach.studio.core.di

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.koach.studio.KoachApplication

/** The [AppContainer] owned by [KoachApplication]. */
@Composable
fun appContainer(): AppContainer =
    (LocalContext.current.applicationContext as KoachApplication).container

/**
 * Creates (or reuses) a ViewModel scoped to the current navigation destination,
 * building it from the [AppContainer]. Pass [key] when the same ViewModel type is
 * shown for different arguments, e.g. a project id.
 */
@Composable
inline fun <reified VM : ViewModel> koachViewModel(
    key: String? = null,
    crossinline create: (AppContainer) -> VM
): VM {
    val container = appContainer()
    return viewModel(
        key = key,
        factory = viewModelFactory { initializer { create(container) } }
    )
}
