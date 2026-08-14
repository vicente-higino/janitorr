package com.github.schaka.janitorr.webhook

import com.github.schaka.janitorr.config.ApplicationProperties
import com.github.schaka.janitorr.mediaserver.MediaServerClient
import com.github.schaka.janitorr.mediaserver.jellyfin.Jellyfin
import com.github.schaka.janitorr.mediaserver.jellyfin.JellyfinProperties
import com.github.schaka.janitorr.mediaserver.library.ProviderIds
import com.github.schaka.janitorr.servarr.ServarrService
import com.github.schaka.janitorr.servarr.WatchedMedia
import com.github.schaka.janitorr.servarr.radarr.Radarr
import com.github.schaka.janitorr.servarr.sonarr.Sonarr
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class JellyfinWebhookHandler(
    private val applicationProperties: ApplicationProperties,
    private val jellyfinProperties: JellyfinProperties,
    @Jellyfin private val jellyfinClient: MediaServerClient,
    @Sonarr private val sonarrService: ServarrService,
    @Radarr private val radarrService: ServarrService,
) {
    companion object {
        private val log = LoggerFactory.getLogger(JellyfinWebhookHandler::class.java)
    }

    init {
        require(!applicationProperties.unmonitorAfterWatch.enabled || jellyfinProperties.enabled) {
            "clients.jellyfin.enabled must be true when application.unmonitor-after-watch is enabled"
        }
        require(!applicationProperties.unmonitorAfterWatch.enabled || !applicationProperties.runOnce) {
            "application.run-once must be false when application.unmonitor-after-watch is enabled"
        }
    }

    fun handle(payload: JellyfinPlaybackWebhook) {
        if (!payload.notificationType.equals("PlaybackStop", ignoreCase = true)) {
            log.debug("Ignoring Jellyfin webhook notification type {}", payload.notificationType)
            return
        }

        if (payload.playedToCompletion != true) {
            log.debug("Ignoring incomplete Jellyfin PlaybackStop for {}", payload.name)
            return
        }

        when {
            payload.itemType.equals("Movie", ignoreCase = true) -> handleMovie(payload)
            payload.itemType.equals("Episode", ignoreCase = true) -> handleEpisode(payload)
            else -> log.debug("Ignoring Jellyfin PlaybackStop for item type {}", payload.itemType)
        }
    }

    private fun handleMovie(payload: JellyfinPlaybackWebhook) {
        val webhookTmdbId = payload.tmdbId?.toIntOrNull()
        val webhookImdbId = payload.imdbId?.takeIf(String::isNotBlank)
        val providerIds = if (webhookTmdbId == null && webhookImdbId == null) {
            providerIds(payload.itemId)
        } else {
            null
        }
        val media = WatchedMedia.Movie(
            tmdbId = webhookTmdbId ?: providerIds?.Tmdb?.toIntOrNull(),
            imdbId = webhookImdbId ?: providerIds?.Imdb?.takeIf(String::isNotBlank),
            title = payload.name,
        )
        if (media.tmdbId == null && media.imdbId == null) {
            log.warn("Could not resolve TMDB or IMDB id for watched Jellyfin movie {}", payload.name)
            return
        }
        radarrService.unmonitorWatched(media)
    }

    private fun handleEpisode(payload: JellyfinPlaybackWebhook) {
        val seasonNumber = payload.seasonNumber
        val episodeNumber = payload.episodeNumber
        if (seasonNumber == null || episodeNumber == null || payload.seriesId.isNullOrBlank()) {
            log.warn(
                "Could not resolve series, season, or episode number for watched Jellyfin episode {}",
                payload.name,
            )
            return
        }

        val providerIds = providerIds(payload.seriesId)
        val media = WatchedMedia.Episode(
            tvdbId = providerIds?.Tvdb?.toIntOrNull(),
            imdbId = providerIds?.Imdb?.takeIf(String::isNotBlank),
            seasonNumber = seasonNumber,
            episodeNumber = episodeNumber,
            episodeNumberEnd = payload.episodeNumberEnd,
            title = payload.seriesName,
        )
        if (media.tvdbId == null && media.imdbId == null) {
            log.warn("Could not resolve TVDB or IMDB id for watched Jellyfin series {}", payload.seriesName)
            return
        }
        sonarrService.unmonitorWatched(media)
    }

    private fun providerIds(itemId: String?): ProviderIds? {
        if (itemId.isNullOrBlank()) {
            return null
        }

        return try {
            val matches = jellyfinClient.getItem(itemId).Items
            if (matches.size != 1) {
                log.warn("Jellyfin returned {} items while resolving provider IDs for {}", matches.size, itemId)
                null
            } else {
                matches.single().ProviderIds
            }
        } catch (exception: RuntimeException) {
            log.warn("Could not retrieve provider IDs from Jellyfin for {}: {}", itemId, exception.message)
            null
        }
    }
}
