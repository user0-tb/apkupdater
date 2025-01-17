package com.apkupdater.repository

import android.os.Build
import android.util.Log
import com.apkupdater.data.fdroid.FdroidData
import com.apkupdater.data.fdroid.FdroidUpdate
import com.apkupdater.data.fdroid.toAppUpdate
import com.apkupdater.data.ui.AppInstalled
import com.apkupdater.data.ui.getVersionCode
import com.apkupdater.prefs.Prefs
import com.apkupdater.service.FdroidService
import com.google.gson.Gson
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import java.io.InputStream
import java.util.jar.JarInputStream

class FdroidRepository(
    private val service: FdroidService,
    private val prefs: Prefs
) {
    private val arch = Build.SUPPORTED_ABIS.toSet()
    private val api = Build.VERSION.SDK_INT

    suspend fun updates(apps: List<AppInstalled>) = flow {
        val response = service.getJar()
        val data = jarToJson(response.byteStream())
        val appNames = apps.map { it.packageName }
        val updates = data.apps
            .asSequence()
            .filter { appNames.contains(it.packageName) }
            .map { FdroidUpdate(data.packages[it.packageName]!![0], it) }
            .filter { it.apk.versionCode > apps.getVersionCode(it.app.packageName) }
            .parseUpdates()
        emit(updates)
    }.catch {
        emit(emptyList())
        Log.e("FdroidRepository", "Error looking for updates.", it)
    }

    suspend fun search(text: String) = flow {
        val response = service.getJar()
        val data = jarToJson(response.byteStream())
        val updates = data.apps
            .asSequence()
            .filter { it.name.contains(text) }
            .map { FdroidUpdate(data.packages[it.packageName]!![0], it) }
            .parseUpdates()
        emit(Result.success(updates))
    }.catch {
        emit(Result.failure(it))
        Log.e("FdroidRepository", "Error searching.", it)
    }

    private fun Sequence<FdroidUpdate>.parseUpdates() = this
        .filter { it.apk.minSdkVersion <= api }
        .filter { filterArch(it) }
        .filter { filterAlpha(it) }
        .filter { filterBeta(it) }
        .map { it.toAppUpdate() }
        .toList()

    private fun filterAlpha(update: FdroidUpdate) = when {
        prefs.ignoreAlpha.get() && update.apk.versionName.contains("alpha") -> false
        else -> true
    }

    private fun filterBeta(update: FdroidUpdate) = when {
        prefs.ignoreBeta.get() && update.apk.versionName.contains("beta") -> false
        else -> true
    }

    private fun filterArch(update: FdroidUpdate) = when {
        update.apk.nativecode.isEmpty() -> true
        update.apk.nativecode.intersect(arch).isNotEmpty() -> true
        else -> false
    }

    private fun jarToJson(stream: InputStream): FdroidData {
        val jar = JarInputStream(stream)
        var entry = jar.nextJarEntry
        while (entry != null) {
            if (entry.name == "index-v1.json") {
                return Gson().fromJson(jar.reader(), FdroidData::class.java)
            }
            entry = jar.nextJarEntry
        }
        return FdroidData()
    }

}
