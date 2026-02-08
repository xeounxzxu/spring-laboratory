package com.example.demo.subapp

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class SubApplication

fun main(args: Array<String>) {
    runApplication<SubApplication>(*args)
}
