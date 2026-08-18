package com.bdshelf.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BarcodeConfirmerTest {

    private val thorgal = "9782803672172"
    private val asterix = "9782012101333"

    @Test
    fun `confirms after the required number of consecutive reads`() {
        val confirmer = BarcodeConfirmer(requiredReads = 3)
        assertNull(confirmer.offer(thorgal))
        assertNull(confirmer.offer(thorgal))
        assertEquals(thorgal, confirmer.offer(thorgal))
    }

    @Test
    fun `a different read resets the count`() {
        val confirmer = BarcodeConfirmer(requiredReads = 3)
        confirmer.offer(thorgal)
        confirmer.offer(thorgal)
        confirmer.offer(asterix) // lecture parasite
        assertNull(confirmer.offer(thorgal))
        assertNull(confirmer.offer(thorgal))
        assertEquals(thorgal, confirmer.offer(thorgal))
    }

    @Test
    fun `invalid checksum reads are ignored and do not reset the candidate`() {
        val confirmer = BarcodeConfirmer(requiredReads = 3)
        confirmer.offer(thorgal)
        confirmer.offer(thorgal)
        assertNull(confirmer.offer("9782803672173")) // somme de contrôle fausse : ignorée
        assertEquals(thorgal, confirmer.offer(thorgal))
    }

    @Test
    fun `re-arms after each confirmation`() {
        val confirmer = BarcodeConfirmer(requiredReads = 2)
        confirmer.offer(thorgal)
        assertEquals(thorgal, confirmer.offer(thorgal))
        // Le livre reste visé : le même code se re-confirme après un nouveau cycle complet.
        assertNull(confirmer.offer(thorgal))
        assertEquals(thorgal, confirmer.offer(thorgal))
    }

    @Test
    fun `normalizes reads to canonical form`() {
        val confirmer = BarcodeConfirmer(requiredReads = 2)
        confirmer.offer("978-2-8036-7217-2")
        assertEquals(thorgal, confirmer.offer(thorgal))
    }
}
