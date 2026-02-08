package com.example.demo

import org.hamcrest.Matchers.notNullValue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@WebMvcTest(GcTrainingController::class)
class GcTrainingControllerTests @Autowired constructor(
    private val mockMvc: MockMvc
) {

    @Test
    fun `young gc endpoint responds with diagnostics`() {
        mockMvc.get("/gc/young") {
            param("payloadSizeMb", "1")
            param("batches", "1")
            param("objectsPerBatch", "2")
        }
            .andExpect {
                status { isOk() }
                jsonPath("$.payloadSizeMb") { value(1) }
                jsonPath("$.gc.deltas[0].name") { value(notNullValue()) }
            }
    }
}
