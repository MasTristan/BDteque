package com.bdshelf.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CrashReportTest {

    @Test
    fun `file name embeds the timestamp`() {
        assertEquals("crash_1234.txt", crashReportFileName(1234L))
    }

    @Test
    fun `timestamp round-trips through the file name`() {
        assertEquals(1234L, crashReportTimestampOrNull(crashReportFileName(1234L)))
    }

    @Test
    fun `unrelated file names yield no timestamp`() {
        assertNull(crashReportTimestampOrNull("room_schema.json"))
        assertNull(crashReportTimestampOrNull("crash_not-a-number.txt"))
    }

    @Test
    fun `formatted report contains context but no secrets`() {
        val report = formatCrashReport(
            CrashReportInput(
                timestampMillis = 1000L,
                stackTrace = "java.lang.IllegalStateException: boom\n\tat Foo.bar(Foo.kt:1)",
                appVersion = "1.0",
                androidVersion = "14",
                deviceModel = "Pixel 6a",
            ),
        )
        assertTrue(report.contains("1.0"))
        assertTrue(report.contains("14"))
        assertTrue(report.contains("Pixel 6a"))
        assertTrue(report.contains("IllegalStateException"))
    }

    @Test
    fun `report younger than retention is not expired`() {
        val now = 40L * 24 * 60 * 60 * 1000
        val tenDaysOld = now - 10L * 24 * 60 * 60 * 1000
        assertFalse(isCrashReportExpired(tenDaysOld, now))
    }

    @Test
    fun `report older than retention is expired`() {
        val now = 40L * 24 * 60 * 60 * 1000
        val fortyDaysOld = now - 40L * 24 * 60 * 60 * 1000
        assertTrue(isCrashReportExpired(fortyDaysOld, now))
    }

    @Test
    fun `report exactly at the retention boundary is not yet expired`() {
        val now = 40L * 24 * 60 * 60 * 1000
        val exactlyThirtyDaysOld = now - 30L * 24 * 60 * 60 * 1000
        assertFalse(isCrashReportExpired(exactlyThirtyDaysOld, now))
    }
}
