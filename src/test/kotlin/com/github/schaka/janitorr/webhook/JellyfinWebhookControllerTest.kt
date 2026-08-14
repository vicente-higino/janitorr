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

class JellyfinWebhookControllerTest {
    private val handler = mockk<JellyfinWebhookHandler>(relaxed = true)
    private val payload = JellyfinPlaybackWebhook(notificationType = "PlaybackStop")

    @Test
    fun `disabled endpoint returns not found`() {
        val controller = JellyfinWebhookController(properties(enabled = false), handler)

        val response = controller.receive("secret", payload)

        assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        verify(exactly = 0) { handler.handle(any()) }
    }

    @Test
    fun `invalid secret returns unauthorized`() {
        val controller = JellyfinWebhookController(properties(), handler)

        val response = controller.receive("wrong-secret", payload)

        assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        verify(exactly = 0) { handler.handle(any()) }
    }

    @Test
    fun `valid secret accepts webhook`() {
        val controller = JellyfinWebhookController(properties(), handler)

        val response = controller.receive("secret", payload)

        assertThat(response.statusCode).isEqualTo(HttpStatus.NO_CONTENT)
        verify(exactly = 1) { handler.handle(payload) }
    }

    private fun properties(enabled: Boolean = true) = ApplicationProperties(
        mediaDeletion = MediaDeletion(),
        tagBasedDeletion = TagDeletion(),
        episodeDeletion = EpisodeDeletion(),
        unmonitorAfterWatch = UnmonitorAfterWatch(enabled = enabled, webhookSecret = if (enabled) "secret" else ""),
    )
}
