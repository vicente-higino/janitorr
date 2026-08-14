package com.github.schaka.janitorr.seerr

import com.github.schaka.janitorr.config.ApplicationProperties
import com.github.schaka.janitorr.config.DefaultClientProperties
import com.github.schaka.janitorr.servarr.radarr.RadarrProperties
import com.github.schaka.janitorr.servarr.sonarr.SonarrProperties
import feign.Feign
import feign.Request
import feign.jackson3.Jackson3Decoder
import feign.jackson3.Jackson3Encoder
import feign.slf4j.Slf4jLogger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders.CONTENT_TYPE
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import tools.jackson.databind.json.JsonMapper

@Configuration(proxyBeanMethods = false)
class SeerrClientConfig {

    companion object {
        private val log = LoggerFactory.getLogger(this::class.java.enclosingClass)
    }

    @Bean
    fun seerrClient(properties: SeerrProperties, defaults: DefaultClientProperties, mapper: JsonMapper): SeerrClient {
        return Feign.builder()
            .options(Request.Options(defaults.connectTimeout, defaults.readTimeout, true))
                .logLevel(defaults.level)
                .logger(Slf4jLogger())
                .decoder(Jackson3Decoder(mapper))
                .encoder(Jackson3Encoder(mapper))
                .requestInterceptor {
                    it.header("X-Api-Key", properties.apiKey)
                    it.header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
                }
                .target(SeerrClient::class.java, properties.url + "/api/v1")
    }

    @Bean
    fun seerrService(
        seerrClient: SeerrClient,
        seerrProperties: SeerrProperties,
        sonarrProperties: SonarrProperties,
        radarrProperties: RadarrProperties,
        applicationProperties: ApplicationProperties,
    ): SeerrService = if (seerrProperties.enabled) {
        SeerrRestService(seerrClient, seerrProperties, sonarrProperties, radarrProperties, applicationProperties)
    } else {
        SeerrNoOpService()
    }
}
