package io.getstream.webrtc.sample.compose.network

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.util.Log

class OrinServerScanner(context: Context) {
  private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
  private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager

  private var multicastLock: WifiManager.MulticastLock? = null
  private var discoveryListener: NsdManager.DiscoveryListener? = null

  fun startScan(
    onServerFound: (ip: String, port: Int) -> Unit,
    onError: (String) -> Unit
  ) {
    // Acquire multicast lock to allow receiving mDNS packets
    multicastLock = wifiManager.createMulticastLock("mdnsLock").apply {
      setReferenceCounted(true)
      acquire()
    }

    discoveryListener = object : NsdManager.DiscoveryListener {
      override fun onDiscoveryStarted(regType: String) {
        Log.d("OrinScanner", "Service discovery started")
      }

      override fun onServiceFound(service: NsdServiceInfo) {
        Log.d("OrinScanner", "Service found: ${service.serviceName}")
        // Filter by service name
        if (service.serviceName.contains("ORIN Server")) {
          resolveService(service, onServerFound)
        }
      }

      override fun onServiceLost(service: NsdServiceInfo) {
        Log.e("OrinScanner", "Service lost: $service")
      }

      override fun onDiscoveryStopped(serviceType: String) {
        Log.i("OrinScanner", "Discovery stopped: $serviceType")
      }

      override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
        Log.e("OrinScanner", "Discovery failed: Error code:$errorCode")
        onError("Start discovery failed")
        stopScan()
      }

      override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
        Log.e("OrinScanner", "Discovery failed: Error code:$errorCode")
        stopScan()
      }
    }

    // Note: Android NSD requires a trailing dot for the service type
    nsdManager.discoverServices("_http._tcp.", NsdManager.PROTOCOL_DNS_SD, discoveryListener)
  }

  private fun resolveService(service: NsdServiceInfo, onServerFound: (String, Int) -> Unit) {
    nsdManager.resolveService(service, object : NsdManager.ResolveListener {
      override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
        Log.e("OrinScanner", "Resolve failed: $errorCode")
      }

      override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
        Log.d("OrinScanner", "Resolve Succeeded. $serviceInfo")
        val hostAddress = serviceInfo.host.hostAddress
        val port = serviceInfo.port
        if (hostAddress != null) {
          onServerFound(hostAddress, port)
        }
      }
    })
  }

  fun stopScan() {
    discoveryListener?.let {
      try {
        nsdManager.stopServiceDiscovery(it)
      } catch (e: Exception) {
        Log.e("OrinScanner", "Error stopping discovery", e)
      }
    }
    discoveryListener = null

    // Release the multicast lock
    multicastLock?.let {
      if (it.isHeld) {
        it.release()
      }
    }
    multicastLock = null
  }
}