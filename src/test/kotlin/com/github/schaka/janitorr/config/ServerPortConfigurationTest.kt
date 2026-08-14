package com.github.schaka.janitorr.config

import com.github.schaka.janitorr.JanitorrApplication
import org.junit.jupiter.api.Test
import org.springframework.boot.env.YamlPropertySourceLoader
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.core.env.MapPropertySource
import org.springframework.core.env.MutablePropertySources
import org.springframework.core.env.PropertySourcesPropertyResolver
import org.springframework.core.io.ClassPathResource
import org.springframework.test.context.ActiveProfiles
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ServerPortConfigurationTest {

    @Test
    fun `http port defaults to 9797`() {
        val resolver = applicationPropertyResolver()

        assertEquals("9797", resolver.getProperty("server.port"))
    }

    @Test
    fun `http port can be overridden with SERVER_PORT`() {
        val propertySources = applicationPropertySources()
        propertySources.addFirst(MapPropertySource("environment", mapOf("SERVER_PORT" to "9898")))

        assertEquals("9898", PropertySourcesPropertyResolver(propertySources).getProperty("server.port"))
    }

    private fun applicationPropertyResolver() = PropertySourcesPropertyResolver(applicationPropertySources())

    private fun applicationPropertySources() = MutablePropertySources().apply {
        val applicationConfig = YamlPropertySourceLoader()
            .load("application", ClassPathResource("application.yml"))
            .single()
        addLast(applicationConfig)
    }
}

@ActiveProfiles("test")
@SpringBootTest(
    classes = [JanitorrApplication::class],
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
)
class HttpServerConfigurationTest {

    @LocalServerPort
    private var port: Int = 0

    @Test
    fun `embedded http server accepts a configured port`() {
        assertTrue(port > 0)
    }
}
