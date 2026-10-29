package org.santayn.testing.mobile.core.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.santayn.testing.mobile.core.network.NetworkState
import org.santayn.testing.mobile.core.network.NetworkStatus
import org.santayn.testing.mobile.core.util.formatDateTime

/** Состояние связи для UI; null в превью и тестах компонентов. */
val LocalNetworkStatus = staticCompositionLocalOf<NetworkStatus?> { null }

@Composable
fun currentNetworkState(): NetworkState {
    val network = LocalNetworkStatus.current ?: return NetworkState.ONLINE
    val state by network.state.collectAsState()
    return state
}

/** Заголовок, пояснение и иконка для состояния без связи. */
data class OfflineCopy(val title: String, val hint: String, val icon: ImageVector)

fun offlineCopy(state: NetworkState): OfflineCopy? = when (state) {
    NetworkState.ONLINE -> null
    NetworkState.NO_INTERNET -> OfflineCopy(
        title = "Нет подключения к интернету",
        hint = "Проверьте Wi-Fi или мобильный интернет. Данные обновятся сами, когда связь появится.",
        icon = Icons.Default.WifiOff,
    )
    NetworkState.SERVER_UNAVAILABLE -> OfflineCopy(
        title = "Сервер недоступен",
        hint = "Сервер не отвечает. Данные обновятся сами, когда он снова заработает.",
        icon = Icons.Default.CloudOff,
    )
}

/**
 * Плашка «нет связи» под верхней панелью экрана.
 * Объявляется программой чтения с экрана (live region) при появлении.
 */
@Composable
fun NetworkBanner(modifier: Modifier = Modifier) {
    val network = LocalNetworkStatus.current ?: return
    val state by network.state.collectAsState()
    val cachedSince by network.cachedDataSince.collectAsState()
    val scope = rememberCoroutineScope()
    var checking by remember { mutableStateOf(false) }
    val copy = offlineCopy(state)

    AnimatedVisibility(copy != null, enter = expandVertically(), exit = shrinkVertically()) {
        val shown = copy ?: return@AnimatedVisibility
        val s = AppTheme.status
        Surface(
            color = s.warningSoft,
            contentColor = s.warningText,
            modifier = modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
        ) {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(shown.icon, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(shown.title, style = MaterialTheme.typography.titleSmall)
                    Text(
                        cachedSince?.let { "Показаны сохранённые данные от ${formatDateTime(it.toString())}" }
                            ?: "Изменения не сохранятся, пока нет связи.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                if (state == NetworkState.SERVER_UNAVAILABLE) {
                    if (checking) {
                        CircularProgressIndicator(Modifier.padding(horizontal = 12.dp).size(20.dp), strokeWidth = 2.dp)
                    } else {
                        TextButton(onClick = {
                            checking = true
                            scope.launch {
                                network.checkServer()
                                checking = false
                            }
                        }) { Text("Проверить") }
                    }
                }
            }
        }
    }
}

/** Вызывает [onReconnect], когда связь с сервером восстановилась. */
@Composable
fun ReloadOnReconnect(onReconnect: () -> Unit) {
    val network = LocalNetworkStatus.current ?: return
    val callback by rememberUpdatedState(onReconnect)
    LaunchedEffect(network) {
        network.reconnected.collect { callback() }
    }
}
