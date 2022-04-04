package com.majaku.ean2product.repository

import com.majaku.ean2product.domain.Ean
import org.springframework.data.mongodb.repository.MongoRepository

interface EansRepository : MongoRepository<Ean, String>