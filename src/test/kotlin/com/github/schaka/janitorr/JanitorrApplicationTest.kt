package com.github.schaka.janitorr

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.test.assertEquals

class JanitorrApplicationTest {

    @Test
    fun `native executable loads configuration and logs beside itself`(@TempDir applicationHome: Path) {
        val originalHome = System.getProperty("JANITORR_HOME")
        val originalConfig = System.getProperty("spring.config.additional-location")

        try {
            System.setProperty("JANITORR_HOME", applicationHome.toString())
            System.clearProperty("spring.config.additional-location")

            configureNativeWindowsHome()

            assertEquals(
                applicationHome.toAbsolutePath().normalize().toString().replace('\\', '/'),
                System.getProperty("JANITORR_HOME")
            )
            assertEquals(
                "optional:${applicationHome.resolve("application.yml").toAbsolutePath().normalize().toUri()}",
                System.getProperty("spring.config.additional-location")
            )
        } finally {
            restoreProperty("JANITORR_HOME", originalHome)
            restoreProperty("spring.config.additional-location", originalConfig)
        }
    }

    private fun restoreProperty(name: String, value: String?) {
        if (value == null) {
            System.clearProperty(name)
        } else {
            System.setProperty(name, value)
        }
    }
}
