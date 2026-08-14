package com.github.schaka.janitorr.mediaserver

import com.github.schaka.janitorr.cleanup.CleanupType
import com.github.schaka.janitorr.config.ApplicationProperties
import com.github.schaka.janitorr.config.FileSystemProperties
import com.github.schaka.janitorr.mediaserver.jellyfin.JellyfinProperties
import com.github.schaka.janitorr.mediaserver.jellyfin.JellyfinRestService
import com.github.schaka.janitorr.mediaserver.library.LibraryContent
import com.github.schaka.janitorr.mediaserver.library.LibraryType
import com.github.schaka.janitorr.mediaserver.library.ProviderIds
import com.github.schaka.janitorr.mediaserver.library.VirtualFolderResponse
import com.github.schaka.janitorr.mediaserver.library.items.ItemPage
import com.github.schaka.janitorr.mediaserver.library.items.MediaFolderItem
import com.github.schaka.janitorr.mediaserver.lookup.MediaLookup
import com.github.schaka.janitorr.servarr.LibraryItem
import com.github.schaka.janitorr.servarr.bazarr.BazarrService
import io.mockk.every
import io.mockk.just
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.SpyK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.Runs
import io.mockk.verifyOrder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.condition.EnabledOnOs
import org.junit.jupiter.api.condition.OS
import java.nio.file.Path
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNull

@ExtendWith(MockKExtension::class)
internal class MediaRestServiceTest {

    @InjectMockKs
    lateinit var jellyfinRestService: JellyfinRestService

    @MockK
    lateinit var mediaServerClient: MediaServerClient

    @MockK
    lateinit var mediaServerUserClient: MediaServerUserClient

    @MockK
    lateinit var bazarrService: BazarrService

    @SpyK
    var mediaServerLibraryQueryService: MediaServerLibraryQueryService = MediaServerLibraryQueryService()

    @MockK
    lateinit var jellyfinProperties: JellyfinProperties

    @MockK
    lateinit var applicationProperties: ApplicationProperties

    @SpyK
    var fileSystemProperties: FileSystemProperties = FileSystemProperties(false, "/data/media/leaving-soon", "/data/media/leaving-soon", true, true)

    @Test
    fun testMovieStructure() {
        val movie = LibraryItem(
                1,
                LocalDateTime.now().minusDays(14),
                "/data/torrents/movies/movie-folder/movie.mkv",
                "/data/media/movies/movie [imdb-812543]/movie.mkv",

                "/data/media/movies/movie [imdb-812543]",
                "/data/media/movies",
                "/data/media/movies/movie [imdb-812543]/movie.mkv",

                "812543"

        )

        val path = Path.of(fileSystemProperties.leavingSoonDir, "movies")
        val structure = jellyfinRestService.pathStructure(movie, path)

        assertEquals(Path.of("/data/media/movies/movie [imdb-812543]"), structure.sourceFolder)
        assertEquals(Path.of("/data/media/movies/movie [imdb-812543]/movie.mkv"), structure.sourceFile)
        assertEquals(Path.of("/data/media/leaving-soon/movies/movie [imdb-812543]"), structure.targetFolder)
        assertEquals(Path.of("/data/media/leaving-soon/movies/movie [imdb-812543]/movie.mkv"), structure.targetFile)
    }

    @Test
    fun testExtendedMovieStructure() {
        val movie = LibraryItem(
            1,
            LocalDateTime.now().minusDays(14),
            "/data/torrents/movies/movie-folder/movie.mkv",
            "/data/media/movies/m/movie [imdb-812543]/movie.mkv",

            "/data/media/movies/m/movie [imdb-812543]",
            "/data/media/movies",
            "/data/media/movies/m/movie [imdb-812543]/movie.mkv",

            "812543"

        )

        val path = Path.of(fileSystemProperties.leavingSoonDir, "movies")
        val structure = jellyfinRestService.pathStructure(movie, path)

        assertEquals(Path.of("/data/media/movies/m/movie [imdb-812543]"), structure.sourceFolder)
        assertEquals(Path.of("/data/media/movies/m/movie [imdb-812543]/movie.mkv"), structure.sourceFile)
        assertEquals(Path.of("/data/media/leaving-soon/movies/m/movie [imdb-812543]"), structure.targetFolder)
        assertEquals(Path.of("/data/media/leaving-soon/movies/m/movie [imdb-812543]/movie.mkv"), structure.targetFile)
    }

    @Test
    fun testTvStructure() {
        val episode = LibraryItem(
                1,
                LocalDateTime.now().minusDays(14),
                "/data/torrents/tv/tv-show-folder-season 01/ep01.mkv",
                "/data/media/tv/tv-show [imdb-812543]/season 01/ep01.mkv",

                "/data/media/tv/tv-show [imdb-812543]",
                "/data/media/tv",
                "/data/media/tv/tv-show [imdb-812543]/season 01/ep01.mkv",

                "812543"

        )

        val path = Path.of(fileSystemProperties.leavingSoonDir, "tv")
        val structure = jellyfinRestService.pathStructure(episode, path)

        assertEquals(Path.of("/data/media/tv/tv-show [imdb-812543]"), structure.sourceFolder)
        assertEquals(Path.of("/data/media/tv/tv-show [imdb-812543]/season 01"), structure.sourceFile)
        assertEquals(Path.of("/data/media/leaving-soon/tv/tv-show [imdb-812543]"), structure.targetFolder)
        assertEquals(Path.of("/data/media/leaving-soon/tv/tv-show [imdb-812543]/season 01"), structure.targetFile)
    }

    @Test
    fun testExtendedTvStructure() {
        val episode = LibraryItem(
            1,
            LocalDateTime.now().minusDays(14),
            "/data/torrents/tv/tv-show-folder-season 01/ep01.mkv",
            "/data/media/tv/t/tv-show [imdb-812543]/season 01/ep01.mkv",

            "/data/media/tv/t/tv-show [imdb-812543]",
            "/data/media/tv",
            "/data/media/tv/t/tv-show [imdb-812543]/season 01/ep01.mkv",

            "812543"

        )

        val path = Path.of(fileSystemProperties.leavingSoonDir, "tv")
        val structure = jellyfinRestService.pathStructure(episode, path)

        assertEquals(Path.of("/data/media/tv/t/tv-show [imdb-812543]"), structure.sourceFolder)
        assertEquals(Path.of("/data/media/tv/t/tv-show [imdb-812543]/season 01"), structure.sourceFile)
        assertEquals(Path.of("/data/media/leaving-soon/tv/t/tv-show [imdb-812543]"), structure.targetFolder)
        assertEquals(Path.of("/data/media/leaving-soon/tv/t/tv-show [imdb-812543]/season 01"), structure.targetFile)
    }

    @Test
    fun testTvMediaStructure() {
        val episode = LibraryItem(
            1,
            LocalDateTime.now().minusDays(14),
            "/data/torrents/tv/tv-show-folder-season 01/ep01.mkv",
            "/data/media/tv/tv-show [imdb-812543]/season 01/ep01.mkv",

            "/data/media/tv/tv-show [imdb-812543]",
            "/data/media/tv",
            "/data/media/tv/tv-show [imdb-812543]/season 01/ep01.mkv",

            "812543"

        )

        val libraryType = LibraryType.TV_SHOWS
        val cleanupType = CleanupType.MEDIA
        val path = Path.of(fileSystemProperties.leavingSoonDir, libraryType.folderName, cleanupType.folderName)
        val structure = jellyfinRestService.pathStructure(episode, path)

        assertEquals(Path.of("/data/media/tv/tv-show [imdb-812543]"), structure.sourceFolder)
        assertEquals(Path.of("/data/media/tv/tv-show [imdb-812543]/season 01"), structure.sourceFile)
        assertEquals(Path.of("/data/media/leaving-soon/tv/media/tv-show [imdb-812543]"), structure.targetFolder)
        assertEquals(Path.of("/data/media/leaving-soon/tv/media/tv-show [imdb-812543]/season 01"), structure.targetFile)
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    fun testWindowsMovieStructure() {
        val movie = LibraryItem(
            1,
            LocalDateTime.now().minusDays(14),
            "D:\\Torrents\\Movies\\movie-folder\\movie.mkv",
            "D:\\Media\\Movies\\movie [imdb-812543]\\movie.mkv",
            "D:\\Media\\Movies\\movie [imdb-812543]",
            "D:\\Media\\Movies",
            "D:\\Media\\Movies\\movie [imdb-812543]\\movie.mkv",
            "812543"
        )

        val path = Path.of("D:\\Media\\Leaving Soon", "movies")
        val structure = jellyfinRestService.pathStructure(movie, path)

        assertEquals(Path.of("D:\\Media\\Movies\\movie [imdb-812543]"), structure.sourceFolder)
        assertEquals(Path.of("D:\\Media\\Movies\\movie [imdb-812543]\\movie.mkv"), structure.sourceFile)
        assertEquals(Path.of("D:\\Media\\Leaving Soon\\movies\\movie [imdb-812543]"), structure.targetFolder)
        assertEquals(Path.of("D:\\Media\\Leaving Soon\\movies\\movie [imdb-812543]\\movie.mkv"), structure.targetFile)
    }

    @Test
    fun testMediaServerPathsKeepConfiguredDriveUncAndUnixSyntax() {
        assertEquals(
            "D:\\Media\\Leaving Soon\\movies\\media",
            jellyfinRestService.pathForMediaServer("D:\\Media\\Leaving Soon", "movies", "media")
        )
        assertEquals(
            "\\\\media-server\\library\\Leaving Soon\\tv\\media",
            jellyfinRestService.pathForMediaServer("\\\\media-server\\library\\Leaving Soon", "tv", "media")
        )
        assertEquals(
            "D:\\Media\\Leaving Soon\\movies\\media",
            jellyfinRestService.pathForMediaServer("D:/Media/Leaving Soon", "movies", "media")
        )
        assertEquals(
            "/library/Leaving Soon/tv/media",
            jellyfinRestService.pathForMediaServer("/library/Leaving Soon", "tv", "media")
        )
    }

    @Test
    fun testJellyfinWindowsPathSeparatorMigration() {
        val library = VirtualFolderResponse(
            CollectionType = "movies",
            ItemId = "library-id",
            LibraryOptions = null,
            Locations = listOf("C:/Media/leaving-soon/movies/media"),
            Name = "Movies (Leaving Soon)",
            PrimaryImageItemId = null,
            RefreshProgress = 0,
            RefreshStatus = null,
            Id = null,
            Guid = null
        )
        val nativePath = "C:\\Media\\leaving-soon\\movies\\media"

        every {
            mediaServerClient.removePathFromLibrary("Movies (Leaving Soon)", "C:/Media/leaving-soon/movies/media", false)
        } just Runs
        every { mediaServerClient.addPathToLibrary(any(), true) } just Runs

        jellyfinRestService.addPathToLibraryWithMigration(library, nativePath)

        verifyOrder {
            mediaServerClient.removePathFromLibrary("Movies (Leaving Soon)", "C:/Media/leaving-soon/movies/media", false)
            mediaServerClient.addPathToLibrary(
                match { request ->
                    request.Name == "Movies (Leaving Soon)" && request.PathInfo.Path == nativePath
                },
                true
            )
        }
    }

    @Test
    fun testMetadataParsing() {
        assertEquals(4513, jellyfinRestService.parseMetadataId("4513-30-days-of-night"))
        assertEquals(4513, jellyfinRestService.parseMetadataId("4513"))
        assertNull(jellyfinRestService.parseMetadataId(null))
    }

    @Test
    fun testMediaServerIdMapping() {
        val parentFolder = mockk<MediaFolderItem> {
            every { Id } returns "idString"
            every { Type } returns "movie"
            every { Name } returns "movies"
        }

        val mediaServerMovies = listOf(
            LibraryContent(
                Id = "1234",
                Type = "Test",
                IsFolder = false,
                IsMovie = true,
                IsSeries = false,
                Name = "Movie1",
                ProviderIds = ProviderIds(Imdb = "812543")
            ),
            LibraryContent(
                Id = "1235",
                Type = "Test",
                IsFolder = false,
                IsMovie = true,
                IsSeries = false,
                Name = "Movie2",
                ProviderIds = ProviderIds(Imdb = "812543")
            ),
            LibraryContent(
                Id = "1236",
                Type = "Test",
                IsFolder = false,
                IsMovie = true,
                IsSeries = false,
                Name = "Movie3",
                ProviderIds = ProviderIds(Imdb = "812544")
            )
        )


        every { mediaServerClient.getAllItems() } returns ItemPage(listOf(parentFolder), 0, 1)
        every { mediaServerClient.getAllMovies( "idString" )} returns ItemPage(mediaServerMovies, 0, 1)

        val populatedIds = jellyfinRestService.getMediaServerIdsForLibrary(
            listOf(
                LibraryItem(
                    1,
                    LocalDateTime.now().minusDays(14),
                    "/data/torrents/movies/movie-folder/movie.mkv",
                    "/data/media/movies/movie [imdb-812543]/movie.mkv",

                    "/data/media/movies/movie [imdb-812543]",
                    "/data/media/movies",
                    "/data/media/movies/movie [imdb-812543]/movie.mkv",

                    "812543"

                ),
                LibraryItem(
                    2,
                    LocalDateTime.now().minusDays(14),
                    "/data/torrents/movies/movie-folder/movie.mkv",
                    "/data/media/movies/movie [imdb-812543]/movie.mkv",

                    "/data/media/movies/movie [imdb-812543]",
                    "/data/media/movies",
                    "/data/media/movies/movie [imdb-812543]/movie.mkv",

                    "812544"

                ),
                LibraryItem(
                    3,
                    LocalDateTime.now().minusDays(14),
                    "/data/torrents/movies/movie-folder/movie.mkv",
                    "/data/media/movies/movie [imdb-812543]/movie.mkv",

                    "/data/media/movies/movie [imdb-812543]",
                    "/data/media/movies",
                    "/data/media/movies/movie [imdb-812543]/movie.mkv",

                    "812545"

                )

            ),
            LibraryType.MOVIES,
            false
        )

        assertThat(populatedIds[MediaLookup(1)]?.size).isEqualTo(2)
        assertThat(populatedIds[MediaLookup(2)]?.size).isEqualTo(1)
        assertThat(populatedIds[MediaLookup(3)]?.size).isEqualTo(0)
    }

}
