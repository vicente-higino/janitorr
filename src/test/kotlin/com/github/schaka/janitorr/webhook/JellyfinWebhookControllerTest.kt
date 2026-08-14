package com.github.schaka.janitorr.webhook

import com.github.schaka.janitorr.config.ApplicationProperties
import com.github.schaka.janitorr.config.EpisodeDeletion
import com.github.schaka.janitorr.config.MediaDeletion
import com.github.schaka.janitorr.config.TagDeletion
import com.github.schaka.janitorr.config.UnmonitorAfterWatch
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import tools.jackson.databind.json.JsonMapper

class JellyfinWebhookControllerTest {
    private val handler = mockk<JellyfinWebhookHandler>(relaxed = true)
    private val payload = JellyfinPlaybackWebhook(notificationType = "PlaybackStop")
    private val rawPayload = """{"NotificationType":"PlaybackStop"}""".toByteArray()
    private val jsonMapper = JsonMapper.builder().findAndAddModules().build()

    @Test
    fun `disabled endpoint returns not found`() {
        val controller = controller(enabled = false)

        val response = controller.receive("secret", rawPayload)

        assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        verify(exactly = 0) { handler.handle(any()) }
    }

    @Test
    fun `invalid secret returns unauthorized`() {
        val controller = controller()

        val response = controller.receive("wrong-secret", rawPayload)

        assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        verify(exactly = 0) { handler.handle(any()) }
    }

    @Test
    fun `valid secret accepts webhook`() {
        val controller = controller()

        val response = controller.receive("secret", rawPayload)

        assertThat(response.statusCode).isEqualTo(HttpStatus.NO_CONTENT)
        verify(exactly = 1) { handler.handle(payload) }
    }

    @Test
    fun `application json and Jellyfin text plain content types are accepted`() {
        val mockMvc = MockMvcBuilders.standaloneSetup(controller()).build()

        listOf(MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN).forEach { contentType ->
            mockMvc.perform(
                post("/api/webhooks/jellyfin")
                    .header(JellyfinWebhookController.SECRET_HEADER, "secret")
                    .contentType(contentType)
                    .content(rawPayload),
            ).andExpect(status().isNoContent)
        }

        verify(exactly = 2) { handler.handle(payload) }
    }

    @Test
    fun `invalid json returns bad request`() {
        val controller = controller()

        val response = controller.receive("secret", "not-json".toByteArray())

        assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        verify(exactly = 0) { handler.handle(any()) }
    }

    private fun controller(enabled: Boolean = true) =
        JellyfinWebhookController(properties(enabled), handler, jsonMapper)

    private fun properties(enabled: Boolean = true) = ApplicationProperties(
        mediaDeletion = MediaDeletion(),
        tagBasedDeletion = TagDeletion(),
        episodeDeletion = EpisodeDeletion(),
        unmonitorAfterWatch = UnmonitorAfterWatch(enabled = enabled, webhookSecret = if (enabled) "secret" else ""),
    )
}
