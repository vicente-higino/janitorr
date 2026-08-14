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
@RegisterReflectionForBinding(
    classes = [Logger.Level::class],
    classNames = [
        "com.github.schaka.janitorr.mediaserver.api.MediaServerUser",
        "com.github.schaka.janitorr.mediaserver.library.VirtualFolderResponse",
        "com.github.schaka.janitorr.mediaserver.library.AddLibraryRequest",
        "com.github.schaka.janitorr.mediaserver.library.CollectionResponse",
        "com.github.schaka.janitorr.mediaserver.library.AddPathRequest",
        "com.github.schaka.janitorr.mediaserver.library.LibraryContent",
        "com.github.schaka.janitorr.mediaserver.library.items.ItemPage",
        "com.github.schaka.janitorr.mediaserver.library.items.MediaFolderItem",
        "com.github.schaka.janitorr.mediaserver.emby.library.AddVirtualFolder",
        "com.github.schaka.janitorr.mediaserver.emby.library.AddMediaPathRequest",
        "com.github.schaka.janitorr.seerr.paging.SeerrPage",
        "com.github.schaka.janitorr.seerr.requests.RequestResponse",
        "com.github.schaka.janitorr.seerr.servarr.ServarrSettings",
        "com.github.schaka.janitorr.servarr.bazarr.BazarrPage",
        "com.github.schaka.janitorr.servarr.history.HistoryResponse",
        "com.github.schaka.janitorr.servarr.history.SonarrHistoryResponse",
        "com.github.schaka.janitorr.servarr.radarr.movie.MoviePayload",
        "com.github.schaka.janitorr.servarr.radarr.movie.MovieFile",
        "com.github.schaka.janitorr.servarr.data_structures.Tag",
        "com.github.schaka.janitorr.servarr.quality_profile.QualityProfile",
        "com.github.schaka.janitorr.servarr.data_structures.RadarrImportListExclusion",
        "com.github.schaka.janitorr.servarr.sonarr.series.SeriesPayload",
        "com.github.schaka.janitorr.servarr.sonarr.episodes.EpisodeResponse",
        "com.github.schaka.janitorr.servarr.sonarr.episodes.MonitoringRequest",
        "com.github.schaka.janitorr.servarr.data_structures.SonarrImportListExclusion",
        "com.github.schaka.janitorr.stats.janitorrstats.requests.JanitorrStatsPagedResponse",
        "com.github.schaka.janitorr.stats.janitorrstats.requests.JanitorrStatsPlayEvent",
        "com.github.schaka.janitorr.stats.jellystat.requests.JellystatPage",
        "com.github.schaka.janitorr.stats.jellystat.requests.JellyStatHistoryResponse",
        "com.github.schaka.janitorr.stats.jellystat.requests.JellystatItemRequest",
        "com.github.schaka.janitorr.stats.streamystats.requests.StreamystatsHistoryResponse",
    ]
)
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
