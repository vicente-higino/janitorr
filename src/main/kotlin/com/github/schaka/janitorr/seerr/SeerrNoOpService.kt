package com.github.schaka.janitorr.seerr

import com.github.schaka.janitorr.servarr.LibraryItem
import org.slf4j.LoggerFactory

/**
 * Does nothing. Used in case the user does not supply Seerr configuration.
 */
class SeerrNoOpService : SeerrService {

    companion object {
        private val log = LoggerFactory.getLogger(this::class.java.enclosingClass)
    }

    override fun cleanupRequests(items: List<LibraryItem>) {
        log.info("Seerr not in use, no requests found.")
    }

}
