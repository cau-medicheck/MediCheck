package com.cau.medicheck.data.system

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * 폰이 지금 인터넷에 연결돼 있는지 알려 준다. (오프라인 상태 화면 S38)
 *
 * 이 앱은 인터넷을 쓰는 기능이 없다(Room 로컬 저장, 로컬 알림). 그래서 이 값은 화면에 보여 주기만 하고
 * 어떤 기능도 이 값에 따라 막지 않는다.
 *
 * "연결됨"의 기준: 기본 네트워크가 있고, 인터넷 기능이 있으며, 실제로 인터넷에 닿는 것이 확인된(VALIDATED) 경우.
 * 와이파이에 붙었지만 인터넷이 안 되는 경우(로그인이 필요한 공용 와이파이 등)는 "연결 없음"으로 본다.
 */
class NetworkMonitor(context: Context) {

    // ConnectivityManager: 안드로이드가 네트워크 상태를 알려 주는 창구
    private val connectivity =
        context.applicationContext.getSystemService(ConnectivityManager::class.java)

    /** 지금 이 순간의 연결 여부 (화면을 처음 그릴 때의 값) */
    fun isOnlineNow(): Boolean =
        connectivity?.getNetworkCapabilities(connectivity.activeNetwork).isUsable()

    /**
     * 연결 여부가 바뀔 때마다 새 값이 흘러나온다.
     * callbackFlow: "바뀌면 불러 주는" 안드로이드 방식(콜백)을 Flow로 바꿔 주는 도구.
     * 듣는 쪽이 없어지면(화면을 떠나면) awaitClose 안의 코드가 실행되어 듣기를 그만둔다.
     */
    val isOnline: Flow<Boolean> = callbackFlow {
        val manager = connectivity
        if (manager == null) {
            trySend(false)
            awaitClose { }
            return@callbackFlow
        }
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                trySend(capabilities.isUsable())
            }

            override fun onLost(network: Network) {
                trySend(false)
            }
        }
        trySend(isOnlineNow())
        // 등록이 거부되는 드문 경우(폰이 허용하는 등록 개수를 넘었을 때 등)에도 화면이 꺼지지 않게 한다.
        // 그때는 처음 값만 보여 주고 바뀌는 것은 듣지 못한다.
        val registered = runCatching { manager.registerDefaultNetworkCallback(callback) }.isSuccess
        awaitClose { if (registered) manager.unregisterNetworkCallback(callback) }
    }.distinctUntilChanged()
}

private fun NetworkCapabilities?.isUsable(): Boolean =
    this != null &&
        hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
        hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
