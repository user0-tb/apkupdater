package com.apkupdater.data.fdroid

import androidx.core.net.toUri
import com.apkupdater.data.ui.AppUpdate
import com.apkupdater.data.ui.FdroidSource

data class FdroidUpdate(
    val apk: FdroidPackage,
    val app: FdroidApp
)

fun FdroidUpdate.toAppUpdate() = AppUpdate(
    app.name,
    app.packageName,
    apk.versionName,
    apk.versionCode,
    FdroidSource,
    if(app.icon.isEmpty())
        "https://f-droid.org/assets/ic_repo_app_default.png".toUri()
    else
        "https://f-droid.org/repo/icons-640/${app.icon}".toUri(),
    "https://f-droid.org/repo/${apk.apkName}"
)
