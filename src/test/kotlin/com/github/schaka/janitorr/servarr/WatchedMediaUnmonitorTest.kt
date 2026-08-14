package com.github.schaka.janitorr.servarr

import com.github.schaka.janitorr.config.ApplicationProperties
import com.github.schaka.janitorr.config.EpisodeDeletion
import com.github.schaka.janitorr.config.FileSystemProperties
import com.github.schaka.janitorr.config.MediaDeletion
import com.github.schaka.janitorr.config.TagDeletion
import com.github.schaka.janitorr.servarr.data_structures.Tag
import com.github.schaka.janitorr.servarr.radarr.RadarrClient
import com.github.schaka.janitorr.servarr.radarr.RadarrProperties
import com.github.schaka.janitorr.servarr.radarr.RadarrRestService
import com.github.schaka.janitorr.servarr.radarr.movie.MoviePayload
import com.github.schaka.janitorr.servarr.sonarr.SonarrClient
import com.github.schaka.janitorr.servarr.sonarr.SonarrProperties
import com.github.schaka.janitorr.servarr.sonarr.SonarrRestService
import com.github.schaka.janitorr.servarr.sonarr.episodes.EpisodeResponse
import com.github.schaka.janitorr.servarr.sonarr.episodes.MonitoringRequest
import com.github.schaka.janitorr.servarr.sonarr.series.SeriesPayload
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class WatchedMediaUnmonitorTest {
    private val fileSystemProperties = FileSystemProperties(access = false)

    @Test
    fun `Radarr unmonitors uniquely matched watched movie`() {
        val client = mockk<RadarrClient>(relaxed = true)
        val movie = movie(monitored = true)
        every { client.getAllMovies() } returns listOf(movie)
        every { client.getMovie(movie.id) } returns movie
        val service = radarrService(client)

        service.unmonitorWatched(WatchedMedia.Movie(123, "tt123", "Movie"))

        verify(exactly = 1) { client.updateMovie(movie.id, movie) }
        assertThat(movie.monitored).isFalse()
    }

    @Test
    fun `Radarr honors exclusion tags`() {
        val client = mockk<RadarrClient>(relaxed = true)
        val movie = movie(monitored = true, tags = listOf(99))
        every { client.getAllMovies() } returns listOf(movie)
        every { client.getMovie(movie.id) } returns movie

        radarrService(client, keepTags = listOf(Tag(99, "janitorr_keep")))
            .unmonitorWatched(WatchedMedia.Movie(123, "tt123", "Movie"))

        verify(exactly = 0) { client.updateMovie(any(), any()) }
        assertThat(movie.monitored).isTrue()
    }

    @Test
    fun `Radarr dry run does not update movie`() {
        val client = mockk<RadarrClient>(relaxed = true)
        val movie = movie(monitored = true)
        every { client.getAllMovies() } returns listOf(movie)
        every { client.getMovie(movie.id) } returns movie

        radarrService(client, dryRun = true)
            .unmonitorWatched(WatchedMedia.Movie(123, "tt123", "Movie"))

        verify(exactly = 0) { client.updateMovie(any(), any()) }
        assertThat(movie.monitored).isTrue()
    }

    @Test
    fun `Radarr already unmonitored movie is idempotent`() {
        val client = mockk<RadarrClient>(relaxed = true)
        val movie = movie(monitored = false)
        every { client.getAllMovies() } returns listOf(movie)
        every { client.getMovie(movie.id) } returns movie

        radarrService(client).unmonitorWatched(WatchedMedia.Movie(123, "tt123", "Movie"))

        verify(exactly = 0) { client.updateMovie(any(), any()) }
    }

    @Test
    fun `Sonarr unmonitors watched episode and monitored episodes sharing its file`() {
        val client = mockk<SonarrClient>(relaxed = true)
        every { client.getAllSeries() } returns listOf(series())
        every { client.getAllEpisodes(10, 2) } returns listOf(
            episode(id = 101, number = 4, fileId = 50),
            episode(id = 102, number = 5, fileId = 50),
            episode(id = 103, number = 6, fileId = 60),
        )
        val request = slot<MonitoringRequest>()

        sonarrService(client).unmonitorWatched(
            WatchedMedia.Episode(456, "tt456", 2, 4, null, "Series"),
        )

        verify(exactly = 1) { client.changeMonitoringStatus(capture(request)) }
        assertThat(request.captured.episodeIds).containsExactly(101, 102)
        assertThat(request.captured.monitored).isFalse()
    }

    @Test
    fun `Sonarr honors exclusion tags`() {
        val client = mockk<SonarrClient>(relaxed = true)
        every { client.getAllSeries() } returns listOf(series(tags = listOf(99)))

        sonarrService(client, keepTags = listOf(Tag(99, "janitorr_keep"))).unmonitorWatched(
            WatchedMedia.Episode(456, "tt456", 2, 4, null, "Series"),
        )

        verify(exactly = 0) { client.changeMonitoringStatus(any()) }
    }

    @Test
    fun `Sonarr already unmonitored episode is idempotent`() {
        val client = mockk<SonarrClient>(relaxed = true)
        every { client.getAllSeries() } returns listOf(series())
        every { client.getAllEpisodes(10, 2) } returns listOf(
            episode(id = 101, number = 4, fileId = 50, monitored = false),
        )

        sonarrService(client).unmonitorWatched(
            WatchedMedia.Episode(456, "tt456", 2, 4, null, "Series"),
        )

        verify(exactly = 0) { client.changeMonitoringStatus(any()) }
    }

    @Test
    fun `Sonarr dry run does not change monitoring`() {
        val client = mockk<SonarrClient>(relaxed = true)
        every { client.getAllSeries() } returns listOf(series())
        every { client.getAllEpisodes(10, 2) } returns listOf(
            episode(id = 101, number = 4, fileId = 50),
        )

        sonarrService(client, dryRun = true).unmonitorWatched(
            WatchedMedia.Episode(456, "tt456", 2, 4, null, "Series"),
        )

        verify(exactly = 0) { client.changeMonitoringStatus(any()) }
    }

    private fun applicationProperties(dryRun: Boolean = false) = ApplicationProperties(
        mediaDeletion = MediaDeletion(),
        tagBasedDeletion = TagDeletion(),
        episodeDeletion = EpisodeDeletion(),
        dryRun = dryRun,
        trainingRun = true,
    )

    private fun radarrService(
        client: RadarrClient,
        dryRun: Boolean = false,
        keepTags: List<Tag> = emptyList(),
    ) = RadarrRestService(
        radarrClient = client,
        applicationProperties = applicationProperties(dryRun),
        fileSystemProperties = fileSystemProperties,
        radarrProperties = RadarrProperties(true, "http://radarr", "key"),
        keepTags = keepTags,
    )

    private fun sonarrService(
        client: SonarrClient,
        keepTags: List<Tag> = emptyList(),
        dryRun: Boolean = false,
    ) = SonarrRestService(
        sonarrClient = client,
        fileSystemProperties = fileSystemProperties,
        applicationProperties = applicationProperties(dryRun),
        sonarrProperties = SonarrProperties(true, "http://sonarr", "key"),
        keepTags = keepTags,
    )

    private fun movie(monitored: Boolean, tags: List<Int> = emptyList()) = MoviePayload(
        added = "2026-01-01T00:00:00Z",
        cleanTitle = "movie",
        folder = null,
        folderName = null,
        hasFile = true,
        id = 20,
        imdbId = "tt123",
        inCinemas = null,
        monitored = monitored,
        movieFile = null,
        originalTitle = "Movie",
        path = "/movies/Movie",
        qualityProfileId = 1,
        rootFolderPath = "/movies",
        sortTitle = "movie",
        status = "released",
        tags = tags,
        title = "Movie",
        titleSlug = "movie",
        tmdbId = 123,
        year = 2026,
    )

    private fun series(tags: List<Int> = emptyList()) = SeriesPayload(
        cleanTitle = "series",
        id = 10,
        imdbId = "tt456",
        monitored = true,
        path = "/tv/Series",
        qualityProfileId = 1,
        rootFolderPath = "/tv",
        seasonFolder = true,
        seasons = emptyList(),
        seriesType = "standard",
        tags = tags,
        title = "Series",
        titleSlug = "series",
        tvMazeId = 0,
        tvRageId = 0,
        tvdbId = 456,
        year = 2026,
    )

    private fun episode(
        id: Int,
        number: Int,
        fileId: Int,
        monitored: Boolean = true,
    ) = EpisodeResponse(
        id = id,
        seriesId = 10,
        tvdbId = null,
        episodeFileId = fileId,
        seasonNumber = 2,
        episodeNumber = number,
        folder = null,
        episodeFile = null,
        path = null,
        airDate = null,
        hasFile = true,
        monitored = monitored,
    )
}
