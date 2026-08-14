package com.github.schaka.janitorr.webhook

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class JellyfinPlaybackWebhook(
    @JsonProperty("NotificationType")
    val notificationType: String? = null,
    @JsonProperty("ItemType")
    val itemType: String? = null,
    @JsonProperty("ItemId")
    val itemId: String? = null,
    @JsonProperty("Name")
    val name: String? = null,
    @JsonProperty("SeriesName")
    val seriesName: String? = null,
    @JsonProperty("SeriesId")
    val seriesId: String? = null,
    @JsonProperty("SeasonNumber")
    val seasonNumber: Int? = null,
    @JsonProperty("EpisodeNumber")
    val episodeNumber: Int? = null,
    @JsonProperty("EpisodeNumberEnd")
    val episodeNumberEnd: Int? = null,
    @JsonProperty("PlayedToCompletion")
    val playedToCompletion: Boolean? = null,
    @JsonProperty("Provider_tmdb")
    val tmdbId: String? = null,
    @JsonProperty("Provider_imdb")
    val imdbId: String? = null,
)
