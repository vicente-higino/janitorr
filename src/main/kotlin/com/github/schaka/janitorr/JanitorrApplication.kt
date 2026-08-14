package com.github.schaka.janitorr

import feign.Logger
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.cache.annotation.EnableCaching
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.annotation.EnableScheduling
import java.nio.file.Path


@EnableConfigurationProperties
@EnableAsync
@EnableCaching
@EnableScheduling
@ConfigurationPropertiesScan
@SpringBootApplication
@RegisterReflectionForBinding(Logger.Level::class)
class JanitorrApplication {

}

fun main(args: Array<String>) {
    configureNativeWindowsHome()
    runApplication<JanitorrApplication>(*args)
}

internal fun configureNativeWindowsHome() {
    val configuredHome = System.getProperty("JANITORR_HOME")
        ?: System.getenv("JANITORR_HOME")
    val executable = ProcessHandle.current().info().command().orElse(null)
    val applicationHome = configuredHome?.let(Path::of)
        ?: executable?.let(Path::of)?.parent
        ?: Path.of("").toAbsolutePath()

    val normalizedHome = applicationHome.toAbsolutePath().normalize().toString().replace('\\', '/')
    System.setProperty("JANITORR_HOME", normalizedHome)

    if (System.getProperty("spring.config.additional-location") == null &&
        System.getenv("SPRING_CONFIG_ADDITIONAL_LOCATION") == null
    ) {
        val configUri = applicationHome.resolve("application.yml").toAbsolutePath().normalize().toUri()
        System.setProperty("spring.config.additional-location", "optional:$configUri")
    }
}
