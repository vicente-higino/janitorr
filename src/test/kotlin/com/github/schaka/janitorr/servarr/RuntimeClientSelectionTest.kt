package com.github.schaka.janitorr.servarr

import com.github.schaka.janitorr.JanitorrApplication
import com.github.schaka.janitorr.seerr.SeerrRestService
import com.github.schaka.janitorr.seerr.SeerrService
import com.github.schaka.janitorr.servarr.radarr.Radarr
import com.github.schaka.janitorr.servarr.radarr.RadarrRestService
import com.github.schaka.janitorr.servarr.sonarr.Sonarr
import com.github.schaka.janitorr.servarr.sonarr.SonarrRestService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource

@SpringBootTest(classes = [JanitorrApplication::class], webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@TestPropertySource(properties = [
    "application.training-run=true",
    "clients.sonarr.enabled=true",
    "clients.radarr.enabled=true",
    "clients.jellyseerr.enabled=true",
])
class RuntimeClientSelectionTest {
    @Autowired
    @Sonarr
    lateinit var sonarrService: ServarrService

    @Autowired
    @Radarr
    lateinit var radarrService: ServarrService

    @Autowired
    lateinit var seerrService: SeerrService

    @Test
    fun `runtime properties select enabled client implementations`() {
        assertThat(sonarrService).isInstanceOf(SonarrRestService::class.java)
        assertThat(radarrService).isInstanceOf(RadarrRestService::class.java)
        assertThat(seerrService).isInstanceOf(SeerrRestService::class.java)
    }
}
