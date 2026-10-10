package org.santayn.testing.mobile.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Сеть устройства через ConnectivityManager.
 *
 * - Требуется NET_CAPABILITY_INTERNET, без VALIDATED: сервер может быть в локальной сети
 *   без выхода в интернет, и такую сеть нельзя считать отсутствующей.
 * - VPN не считается: VPN-клиент остаётся «подключённым» и в авиарежиме, когда под ним нет
 *   ни Wi-Fi, ни мобильной сети (проверено на телефоне с VPN). Нужна реальная сеть (NOT_VPN).
 */
class AndroidConnectivityObserver(context: Context) : ConnectivityObserver {
    private val manager = context.getSystemService(ConnectivityManager::class.java)
    private val networks = mutableSetOf<Network>()
    private val _isConnected = MutableStateFlow(currentlyConnected())
    override val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    init {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
            .build()
        manager.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = update { add(network) }
            override fun onLost(network: Network) = update { remove(network) }
        })
    }

    private fun update(change: MutableSet<Network>.() -> Unit) {
        _isConnected.value = synchronized(networks) {
            networks.change()
            networks.isNotEmpty()
        }
        // Соединения, открытые в прежней сети, больше не годятся.
        sharedConnectionPool.evictAll()
    }

    /** Начальное значение: колбэк приходит только для подходящих сетей, «нет сети» он не сообщает. */
    @Suppress("DEPRECATION") // allNetworks: нужен разовый снимок всех сетей, а не только default (он может быть VPN)
    private fun currentlyConnected(): Boolean = manager.allNetworks.any { network ->
        val caps = manager.getNetworkCapabilities(network) ?: return@any false
        caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN)
    }
}
