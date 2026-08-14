package com.github.schaka.janitorr

import org.springframework.aot.hint.RuntimeHints
import org.springframework.aot.hint.RuntimeHintsRegistrar
import org.springframework.aot.hint.TypeReference

/** Native-image registrations for the dynamic proxies created by OpenFeign. */
class JanitorrRuntimeHints : RuntimeHintsRegistrar {
    override fun registerHints(hints: RuntimeHints, classLoader: ClassLoader?) {
        feignClientInterfaces.forEach { interfaceName ->
            hints.proxies().registerJdkProxy(TypeReference.of(interfaceName))
        }
    }

    private companion object {
        val feignClientInterfaces = listOf(
            "com.github.schaka.janitorr.mediaserver.MediaServerClient",
            "com.github.schaka.janitorr.mediaserver.MediaServerUserClient",
            "com.github.schaka.janitorr.mediaserver.emby.EmbyMediaServerClient",
            "com.github.schaka.janitorr.seerr.SeerrClient",
            "com.github.schaka.janitorr.servarr.bazarr.BazarrClient",
            "com.github.schaka.janitorr.servarr.radarr.RadarrClient",
            "com.github.schaka.janitorr.servarr.sonarr.SonarrClient",
            "com.github.schaka.janitorr.stats.janitorrstats.JanitorrStatsClient",
            "com.github.schaka.janitorr.stats.jellystat.JellystatClient",
            "com.github.schaka.janitorr.stats.streamystats.StreamystatsClient",
        )
    }
}
