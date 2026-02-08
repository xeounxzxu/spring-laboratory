package com.example.demo.pubapp

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class PubApplication

fun main(args: Array<String>) {
    runApplication<PubApplication>(*args)
}
