package com.cyberdeck.android.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class AppRecord(
    val label: String, val packageName: String, val versionName: String, val versionCode: Long,
    val targetSdk: Int, val minSdk: Int, val requested: List<String>, val granted: List<String>,
    val firstInstall: Long, val lastUpdate: Long, val system: Boolean
)

object AppInspector {
    fun listApps(context: Context): List<AppRecord> {
        val pm = context.packageManager
        val flags = PackageManager.GET_PERMISSIONS
        val packages = if (Build.VERSION.SDK_INT >= 33) pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(flags.toLong()))
        else @Suppress("DEPRECATION") pm.getInstalledPackages(flags)
        return packages.map { info ->
            val ai = info.applicationInfo
            val requested = info.requestedPermissions?.toList().orEmpty()
            val grantedFlags = info.requestedPermissionsFlags
            val granted = requested.mapIndexedNotNull { i, perm ->
                val flag = grantedFlags?.getOrNull(i) ?: 0
                if (flag and PackageManager.PERMISSION_GRANTED != 0) perm else null
            }
            val vc = if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else @Suppress("DEPRECATION") info.versionCode.toLong()
            AppRecord(
                ai?.loadLabel(pm)?.toString() ?: info.packageName, info.packageName,
                info.versionName ?: "?", vc, ai?.targetSdkVersion ?: -1,
                if (Build.VERSION.SDK_INT >= 24) ai?.minSdkVersion ?: -1 else -1,
                requested, granted, info.firstInstallTime, info.lastUpdateTime,
                (ai?.flags ?: 0) and ApplicationInfo.FLAG_SYSTEM != 0
            )
        }.sortedBy { it.label.lowercase(Locale.US) }
    }

    fun describe(app: AppRecord): String {
        val df = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
        return "${app.label}\n${app.packageName}\nversion  ${app.versionName} (${app.versionCode})\nsdk      min=${app.minSdk} target=${app.targetSdk}\ninstalled ${df.format(Date(app.firstInstall))}\nrequested ${app.requested.size} granted ${app.granted.size}"
    }

    fun permissionGroups(apps: List<AppRecord>): Map<String, List<String>> {
        val buckets = linkedMapOf(
            "Location" to mutableListOf<String>(), "Bluetooth" to mutableListOf(), "Camera" to mutableListOf(),
            "Microphone" to mutableListOf(), "Storage" to mutableListOf(), "Network" to mutableListOf(),
            "Notifications" to mutableListOf(), "Other" to mutableListOf()
        )
        apps.forEach { app ->
            app.requested.forEach { perm ->
                val tag = "${app.label} | $perm | ${if (perm in app.granted) "GRANTED" else "requested"}"
                when {
                    perm.contains("LOCATION", true) -> buckets["Location"]!!.add(tag)
                    perm.contains("BLUETOOTH", true) || perm.contains("NEARBY", true) -> buckets["Bluetooth"]!!.add(tag)
                    perm.contains("CAMERA", true) -> buckets["Camera"]!!.add(tag)
                    perm.contains("RECORD_AUDIO", true) -> buckets["Microphone"]!!.add(tag)
                    perm.contains("STORAGE", true) || perm.contains("MEDIA", true) -> buckets["Storage"]!!.add(tag)
                    perm.contains("INTERNET", true) || perm.contains("NETWORK", true) || perm.contains("WIFI", true) -> buckets["Network"]!!.add(tag)
                    perm.contains("NOTIFICATION", true) -> buckets["Notifications"]!!.add(tag)
                    else -> buckets["Other"]!!.add(tag)
                }
            }
        }
        return buckets
    }
}
