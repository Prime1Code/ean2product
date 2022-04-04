package com.majaku.ean2product.domain

import org.springframework.data.mongodb.core.mapping.Document

@Document
data class Ean constructor(
    val ean: String,
    val products: List<String>
)