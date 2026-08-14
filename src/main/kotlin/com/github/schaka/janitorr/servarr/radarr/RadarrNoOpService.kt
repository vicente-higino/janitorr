package com.github.schaka.janitorr.servarr.radarr

import com.github.schaka.janitorr.servarr.LibraryItem
import com.github.schaka.janitorr.servarr.ServarrService
import com.github.schaka.janitorr.servarr.WatchedMedia
import org.slf4j.LoggerFactory

class RadarrNoOpService : ServarrService {

    companion object {
        private val log = LoggerFactory.getLogger(this::class.java.enclosingClass)
    }

    override fun getEntries(): List<LibraryItem> {
        log.info("Radarr is disabled, not getting any movies")
        return listOf()
    }

    override fun removeEntries(items: List<LibraryItem>) {
        log.info("Radarr is disabled, not deleting any movies")
    }

    override fun unmonitorWatched(media: WatchedMedia) {
        log.debug("Radarr is disabled, not unmonitoring watched media")
    }

}
