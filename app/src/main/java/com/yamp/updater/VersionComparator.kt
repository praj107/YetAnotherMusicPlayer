package com.yamp.updater

object VersionComparator {

    /**
     * Compares two semantic version strings (e.g., "1.2.3" vs "1.3.0").
     * Returns positive if remote > current, negative if remote < current, 0 if equal.
     */
    fun compare(current: String, remote: String): Int {
        val currentParts = parseParts(current)
        val remoteParts = parseParts(remote)

        for (i in 0 until maxOf(currentParts.size, remoteParts.size)) {
            val c = currentParts.getOrElse(i) { 0 }
            val r = remoteParts.getOrElse(i) { 0 }
            if (r != c) return r - c
        }
        return 0
    }

    fun isNewer(current: String, remote: String): Boolean = compare(current, remote) > 0

    private fun parseParts(version: String): List<Int> =
        version.removePrefix("v")
            .split(".")
            .mapNotNull { it.toIntOrNull() }
}
