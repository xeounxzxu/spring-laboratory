package com.example.demo.coroutineapp

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.http.HttpStatus
import java.time.Instant

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CoroutineDemoControllerTests {

    @Autowired
    lateinit var restTemplate: TestRestTemplate

    @MockBean
    lateinit var coroutineService: CoroutineService

    @Test
    fun `external-echo endpoint returns payload`() {
        val expected = ExternalEchoResponse(
            payload = "external:test",
            upstreamTimestamp = Instant.parse("2024-01-01T00:00:00Z"),
            traceId = "trace-1",
        )
        given(coroutineService.externalHttpService("test")).willReturn(expected)

        val response = restTemplate.getForEntity("/coroutines/external-echo?text=test", ExternalEchoResponse::class.java)
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body?.payload).isEqualTo("external:test")
    }

    @Test
    fun `pub-ping endpoint returns response`() {
        val expected = PingRelayResponse(
            status = 200,
            body = "pong",
        )
        given(coroutineService.pingPubApp()).willReturn(expected)

        val response = restTemplate.getForEntity("/coroutines/pub-ping", PingRelayResponse::class.java)
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body?.status).isEqualTo(200)
        assertThat(response.body?.body).isEqualTo("pong")
    }
}
