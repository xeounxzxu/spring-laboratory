package com.example.demo.coroutineapp

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CoroutineDemoControllerTests {

    @Autowired
    lateinit var restTemplate: TestRestTemplate

    @Test
    fun `echo endpoint returns payload`() {
        val response = restTemplate.getForEntity("/coroutines/echo?text=test", EchoResponse::class.java)
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body?.message).isEqualTo("test")
    }

    @Test
    fun `parallel endpoint bounds worker count`() {
        val response = restTemplate.exchange(
            "/coroutines/parallel?workers=99",
            HttpMethod.GET,
            null,
            object : ParameterizedTypeReference<ParallelResponse>() {}
        )
        assertThat(response.statusCode).isEqualTo(HttpStatus.OK)
        assertThat(response.body?.results?.size).isEqualTo(16)
        assertThat(response.body?.totalDurationMs).isGreaterThanOrEqualTo(0)
    }
}
