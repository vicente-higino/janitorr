package com.github.schaka.janitorr.webhook

import com.github.schaka.janitorr.config.ApplicationProperties
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

@RestController
@RequestMapping("/api/webhooks/jellyfin")
class JellyfinWebhookController(
    private val applicationProperties: ApplicationProperties,
    private val handler: JellyfinWebhookHandler,
) {
    companion object {
        const val SECRET_HEADER = "X-Janitorr-Webhook-Secret"
    }

    @PostMapping
    fun receive(
        @RequestHeader(SECRET_HEADER, required = false) suppliedSecret: String?,
        @RequestBody payload: JellyfinPlaybackWebhook,
    ): ResponseEntity<Void> {
        val properties = applicationProperties.unmonitorAfterWatch
        if (!properties.enabled) {
            return ResponseEntity.notFound().build()
        }
        if (!secretsMatch(properties.webhookSecret, suppliedSecret)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }

        handler.handle(payload)
        return ResponseEntity.noContent().build()
    }

    private fun secretsMatch(expected: String, supplied: String?): Boolean {
        if (supplied == null) {
            return false
        }
        return MessageDigest.isEqual(
            expected.toByteArray(StandardCharsets.UTF_8),
            supplied.toByteArray(StandardCharsets.UTF_8),
        )
    }
}
