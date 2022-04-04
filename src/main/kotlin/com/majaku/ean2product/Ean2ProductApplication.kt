package com.majaku.ean2product

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class Ean2ProductApplication

fun main(args: Array<String>) {
	runApplication<RestApiApplication>(*args)
}
