package com.apkupdater.data.github


data class GitHubRelease(
    val name: String,
    val prerelease: Boolean,
    val assets: List<GitHubReleaseAsset>
)

