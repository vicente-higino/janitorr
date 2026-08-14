package com.github.schaka.janitorr.servarr

import com.github.schaka.janitorr.config.ApplicationProperties
import com.github.schaka.janitorr.config.FileSystemProperties
import com.github.schaka.janitorr.servarr.radarr.Radarr
import com.github.schaka.janitorr.servarr.radarr.RadarrClient
import com.github.schaka.janitorr.servarr.radarr.RadarrNoOpService
import com.github.schaka.janitorr.servarr.radarr.RadarrProperties
import com.github.schaka.janitorr.servarr.radarr.RadarrRestService
import com.github.schaka.janitorr.servarr.sonarr.Sonarr
import com.github.schaka.janitorr.servarr.sonarr.SonarrClient
import com.github.schaka.janitorr.servarr.sonarr.SonarrNoOpService
import com.github.schaka.janitorr.servarr.sonarr.SonarrProperties
import com.github.schaka.janitorr.servarr.sonarr.SonarrRestService
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/** Selects enabled/no-op services from runtime properties, including in AOT images. */
@Configuration(proxyBeanMethods = false)
class ServarrServiceConfig {
    private val log = LoggerFactory.getLogger(javaClass)

    @Bean
    @Sonarr
    fun sonarrService(
        sonarrClient: SonarrClient,
        fileSystemProperties: FileSystemProperties,
        applicationProperties: ApplicationProperties,
        sonarrProperties: SonarrProperties,
    ): ServarrService {
        log.info("Sonarr runtime configuration: {}", if (sonarrProperties.enabled) "enabled" else "disabled")
        return if (sonarrProperties.enabled) {
            SonarrRestService(sonarrClient, fileSystemProperties, applicationProperties, sonarrProperties)
        } else {
            SonarrNoOpService()
        }
    }

    @Bean
    @Radarr
    fun radarrService(
        radarrClient: RadarrClient,
        applicationProperties: ApplicationProperties,
        fileSystemProperties: FileSystemProperties,
        radarrProperties: RadarrProperties,
    ): ServarrService {
        log.info("Radarr runtime configuration: {}", if (radarrProperties.enabled) "enabled" else "disabled")
        return if (radarrProperties.enabled) {
            RadarrRestService(radarrClient, applicationProperties, fileSystemProperties, radarrProperties)
        } else {
            RadarrNoOpService()
        }
    }
}
