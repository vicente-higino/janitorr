package com.github.schaka.janitorr.servarr

sealed interface WatchedMedia {
    val title: String?

    data class Movie(
        val tmdbId: Int?,
        val imdbId: String?,
        override val title: String?,
    ) : WatchedMedia

    data class Episode(
        val tvdbId: Int?,
        val imdbId: String?,
        val seasonNumber: Int,
        val episodeNumber: Int,
        val episodeNumberEnd: Int?,
        override val title: String?,
    ) : WatchedMedia {
        val episodeNumbers: IntRange
            get() = if (episodeNumberEnd != null && episodeNumberEnd >= episodeNumber) {
                episodeNumber..episodeNumberEnd
            } else {
                episodeNumber..episodeNumber
            }
    }
}
