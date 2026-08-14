package com.github.schaka.janitorr.webhook

import com.github.schaka.janitorr.config.ApplicationProperties
import com.github.schaka.janitorr.config.EpisodeDeletion
import com.github.schaka.janitorr.config.MediaDeletion
import com.github.schaka.janitorr.config.TagDeletion
import com.github.schaka.janitorr.config.UnmonitorAfterWatch
import com.github.schaka.janitorr.mediaserver.MediaServerClient
import com.github.schaka.janitorr.mediaserver.jellyfin.JellyfinProperties
import com.github.schaka.janitorr.mediaserver.library.LibraryContent
import com.github.schaka.janitorr.mediaserver.library.ProviderIds
import com.github.schaka.janitorr.mediaserver.library.items.ItemPage
import com.github.schaka.janitorr.servarr.ServarrService
import com.github.schaka.janitorr.servarr.WatchedMedia
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test

class JellyfinWebhookHandlerTest {
    private val jellyfinClient = mockk<MediaServerClient>(relaxed = true)
    private val sonarrService = mockk<ServarrService>(relaxed = true)
    private val radarrService = mockk<ServarrService>(relaxed = true)
    private val handler = JellyfinWebhookHandler(
        applicationProperties(),
        jellyfinProperties(),
        jellyfinClient,
        sonarrService,
        radarrService,
    )

    @Test
    fun `incomplete playback is ignored`() {
        handler.handle(
            JellyfinPlaybackWebhook(
                notificationType = "PlaybackStop",
                itemType = "Movie",
                playedToCompletion = false,
                tmdbId = "123",
            ),
        )

        verify(exactly = 0) { radarrService.unmonitorWatched(any()) }
        verify(exactly = 0) { sonarrService.unmonitorWatched(any()) }
    }

    @Test
    fun `completed movie uses provider ids from webhook without Jellyfin lookup`() {
        handler.handle(
            JellyfinPlaybackWebhook(
                notificationType = "PlaybackStop",
                itemType = "Movie",
                itemId = "movie-id",
                name = "Movie",
                playedToCompletion = true,
                tmdbId = "123",
                imdbId = "tt123",
            ),
        )

        verify(exactly = 0) { jellyfinClient.getItem(any()) }
        verify(exactly = 1) {
            radarrService.unmonitorWatched(WatchedMedia.Movie(123, "tt123", "Movie"))
        }
    }

    @Test
    fun `completed episode resolves series provider ids through Jellyfin`() {
        every { jellyfinClient.getItem("series-id") } returns itemPage(
            libraryItem(ProviderIds(Tvdb = "456", Imdb = "tt456")),
        )

        handler.handle(
            JellyfinPlaybackWebhook(
                notificationType = "PlaybackStop",
                itemType = "Episode",
                name = "Episode",
                seriesName = "Series",
                seriesId = "series-id",
                seasonNumber = 2,
                episodeNumber = 4,
                episodeNumberEnd = 5,
                playedToCompletion = true,
            ),
        )

        verify(exactly = 1) {
            sonarrService.unmonitorWatched(
                WatchedMedia.Episode(456, "tt456", 2, 4, 5, "Series"),
            )
        }
    }

    @Test
    fun `missing Jellyfin series is skipped`() {
        every { jellyfinClient.getItem("series-id") } returns itemPage()

        handler.handle(completedEpisode())

        verify(exactly = 0) { sonarrService.unmonitorWatched(any()) }
    }

    @Test
    fun `Jellyfin lookup failure is skipped`() {
        every { jellyfinClient.getItem("series-id") } throws RuntimeException("Jellyfin unavailable")

        handler.handle(completedEpisode())

        verify(exactly = 0) { sonarrService.unmonitorWatched(any()) }
    }

    private fun applicationProperties() = ApplicationProperties(
        mediaDeletion = MediaDeletion(),
        tagBasedDeletion = TagDeletion(),
        episodeDeletion = EpisodeDeletion(),
        unmonitorAfterWatch = UnmonitorAfterWatch(enabled = true, webhookSecret = "secret"),
    )

    private fun jellyfinProperties() = JellyfinProperties(
        enabled = true,
        url = "http://jellyfin",
        apiKey = "key",
        username = "user",
        password = "password",
    )

    private fun completedEpisode() = JellyfinPlaybackWebhook(
        notificationType = "PlaybackStop",
        itemType = "Episode",
        name = "Episode",
        seriesName = "Series",
        seriesId = "series-id",
        seasonNumber = 2,
        episodeNumber = 4,
        playedToCompletion = true,
    )

    private fun itemPage(vararg items: LibraryContent) = ItemPage(
        Items = items.toList(),
        StartIndex = 0,
        TotalRecordCount = items.size,
    )

    private fun libraryItem(providerIds: ProviderIds) = LibraryContent(
        Id = "series-id",
        IsFolder = true,
        IsMovie = false,
        IsSeries = true,
        Name = "Series",
        Type = "Series",
        ProviderIds = providerIds,
    )
}
