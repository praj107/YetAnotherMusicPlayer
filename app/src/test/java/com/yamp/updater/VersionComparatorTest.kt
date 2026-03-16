package com.yamp.updater

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionComparatorTest {

    @Test
    fun `same version returns 0`() {
        assertEquals(0, VersionComparator.compare("1.0.0", "1.0.0"))
    }

    @Test
    fun `remote patch is newer`() {
        assertTrue(VersionComparator.isNewer("1.0.0", "1.0.1"))
    }

    @Test
    fun `remote minor is newer`() {
        assertTrue(VersionComparator.isNewer("1.0.5", "1.1.0"))
    }

    @Test
    fun `remote major is newer`() {
        assertTrue(VersionComparator.isNewer("1.9.9", "2.0.0"))
    }

    @Test
    fun `current is newer returns false`() {
        assertFalse(VersionComparator.isNewer("2.0.0", "1.5.0"))
    }

    @Test
    fun `handles v prefix`() {
        assertTrue(VersionComparator.isNewer("v1.0.0", "v1.0.1"))
        assertEquals(0, VersionComparator.compare("v1.2.3", "1.2.3"))
    }

    @Test
    fun `handles uneven version parts`() {
        assertTrue(VersionComparator.isNewer("1.0", "1.0.1"))
        assertFalse(VersionComparator.isNewer("1.0.1", "1.0"))
    }

    @Test
    fun `compare returns positive when remote is newer`() {
        assertTrue(VersionComparator.compare("1.0.0", "1.0.2") > 0)
    }

    @Test
    fun `compare returns negative when current is newer`() {
        assertTrue(VersionComparator.compare("2.0.0", "1.0.0") < 0)
    }

    @Test
    fun `handles large version numbers`() {
        assertTrue(VersionComparator.isNewer("1.99.100", "1.99.101"))
        assertFalse(VersionComparator.isNewer("1.99.101", "1.99.100"))
    }
}
