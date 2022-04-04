package com.majaku.ean2product

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.runApplication

@SpringBootApplication
class RestApiApplication

fun main(args: Array<String>) {
	runApplication<RestApiApplication>(*args)
}
