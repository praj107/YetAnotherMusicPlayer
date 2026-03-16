package com.yamp.updater

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class GitHubReleaseTest {

    private fun makeAsset(name: String, url: String = "https://example.com/$name") =
        GitHubAsset(
            id = 1,
            name = name,
            size = 1024,
            browserDownloadUrl = url,
            contentType = null
        )

    private fun makeRelease(
        tagName: String = "v1.0.0",
        assets: List<GitHubAsset> = emptyList(),
        draft: Boolean = false,
        prerelease: Boolean = false
    ) = GitHubRelease(
        id = 1,
        tagName = tagName,
        name = "Release $tagName",
        body = "Release notes",
        publishedAt = "2025-01-01T00:00:00Z",
        draft = draft,
        prerelease = prerelease,
        assets = assets
    )

    @Test
    fun `version strips v prefix`() {
        assertEquals("1.2.3", makeRelease(tagName = "v1.2.3").version)
    }

    @Test
    fun `version without prefix unchanged`() {
        assertEquals("1.2.3", makeRelease(tagName = "1.2.3").version)
    }

    @Test
    fun `apkAsset finds apk file`() {
        val release = makeRelease(assets = listOf(
            makeAsset("checksums.sha256"),
            makeAsset("yamp-v1.0.0.apk")
        ))
        assertNotNull(release.apkAsset)
        assertEquals("yamp-v1.0.0.apk", release.apkAsset?.name)
    }

    @Test
    fun `apkAsset null when no apk`() {
        val release = makeRelease(assets = listOf(makeAsset("checksums.sha256")))
        assertNull(release.apkAsset)
    }

    @Test
    fun `checksumAsset finds sha256 file`() {
        val release = makeRelease(assets = listOf(
            makeAsset("yamp-v1.0.0.apk"),
            makeAsset("yamp-v1.0.0.apk.sha256")
        ))
        assertNotNull(release.checksumAsset)
        assertEquals("yamp-v1.0.0.apk.sha256", release.checksumAsset?.name)
    }

    @Test
    fun `checksumAsset null when no sha256`() {
        val release = makeRelease(assets = listOf(makeAsset("yamp-v1.0.0.apk")))
        assertNull(release.checksumAsset)
    }
}
