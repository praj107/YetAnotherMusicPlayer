package com.yamp.updater

import com.google.gson.annotations.SerializedName

data class GitHubRelease(
    val id: Long,
    @SerializedName("tag_name")
    val tagName: String,
    val name: String?,
    val body: String?,
    @SerializedName("published_at")
    val publishedAt: String?,
    val draft: Boolean,
    val prerelease: Boolean,
    val assets: List<GitHubAsset>
) {
    val version: String
        get() = tagName.removePrefix("v")

    val apkAsset: GitHubAsset?
        get() = assets.firstOrNull { it.name.endsWith(".apk") }

    val checksumAsset: GitHubAsset?
        get() = assets.firstOrNull { it.name.endsWith(".sha256") }
}

data class GitHubAsset(
    val id: Long,
    val name: String,
    val size: Long,
    @SerializedName("browser_download_url")
    val browserDownloadUrl: String,
    @SerializedName("content_type")
    val contentType: String?
)
