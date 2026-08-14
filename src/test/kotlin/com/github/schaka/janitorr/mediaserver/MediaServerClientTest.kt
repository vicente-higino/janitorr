package com.github.schaka.janitorr.mediaserver

import feign.Client
import feign.Feign
import feign.Response
import feign.jackson3.Jackson3Decoder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import tools.jackson.databind.json.JsonMapper
import java.nio.charset.StandardCharsets

class MediaServerClientTest {

    @Test
    fun `single item lookup uses supported Jellyfin collection query`() {
        var requestedUrl: String? = null
        val responseBody = """{"Items":[],"StartIndex":0,"TotalRecordCount":0}"""
        val client = Client { request, _ ->
            requestedUrl = request.url()
            Response.builder()
                .status(200)
                .reason("OK")
                .request(request)
                .headers(emptyMap())
                .body(responseBody, StandardCharsets.UTF_8)
                .build()
        }
        val mediaServerClient = Feign.builder()
            .client(client)
            .decoder(Jackson3Decoder(JsonMapper.builder().findAndAddModules().build()))
            .target(MediaServerClient::class.java, "http://jellyfin")

        mediaServerClient.getItem("series-id")

        assertThat(requestedUrl)
            .isEqualTo("http://jellyfin/Items?ids=series-id&fields=ProviderIds&limit=1")
    }
}
