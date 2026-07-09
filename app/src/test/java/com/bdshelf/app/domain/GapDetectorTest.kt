package com.bdshelf.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class GapDetectorTest {

    @Test
    fun `no gaps when all tomes owned`() {
        val albums = listOf(
            album("s", 1, owned = true),
            album("s", 2, owned = true),
            album("s", 3, owned = true),
        )
        assertEquals(emptyList<Int>(), GapDetector.gaps(albums))
    }

    @Test
    fun `internal missing tomes are gaps`() {
        val albums = listOf(
            album("s", 1, owned = true),
            album("s", 2, owned = false),
            album("s", 4, owned = true),
        )
        // Trous : 2 (non possédé) et 3 (absent), bornés par le plus grand tome connu (4).
        assertEquals(listOf(2, 3), GapDetector.gaps(albums))
    }

    @Test
    fun `empty albums yield no gaps`() {
        assertEquals(emptyList<Int>(), GapDetector.gaps(emptyList()))
    }

    @Test
    fun `unnumbered albums are ignored`() {
        val albums = listOf(
            album("s", 1, owned = true),
            album("s", null, owned = true),
        )
        assertEquals(emptyList<Int>(), GapDetector.gaps(albums))
    }

    @Test
    fun `next tome number is max plus one`() {
        val albums = listOf(
            album("s", 1, owned = true),
            album("s", 5, owned = false),
        )
        assertEquals(6, GapDetector.nextTomeNumber(albums))
    }

    @Test
    fun `next tome number is one when empty`() {
        assertEquals(1, GapDetector.nextTomeNumber(emptyList()))
    }
}
