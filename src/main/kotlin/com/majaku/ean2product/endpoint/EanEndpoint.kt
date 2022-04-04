package com.majaku.ean2product.endpoint

import com.majaku.ean2product.domain.Ean
import com.majaku.ean2product.repository.EansRepository
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.lang.Exception

@RestController
@RequestMapping("/ean")
class EanEndpoint @Autowired constructor(
    val eansRepository: EansRepository
) {
    private val unwantedStringsRegex = Regex("[.!?]")
    private val validEanPattern = Regex("^[0-9]{8}\$|^[0-9]{12}\$|^[0-9]{13}\$|^[0-9]{14}\$")

    @GetMapping("{ean}")
    fun getEan(@PathVariable("ean") ean: String): ResponseEntity<List<String>> {
        if (!isValidEan(ean)) {
            return ResponseEntity.badRequest().body(listOf("Error - EAN not valid"))
        }
        val url = "https://www.qwant.com/?l=de&q="
        val doc: Document
        try {
            doc = Jsoup.connect(url + ean)
                .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:98.0) Gecko/20100101 Firefox/98.0")
                .header(
                    "Accept",
                    "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8"
                )
                .get()
        } catch (e: Exception) {
            println(e.message)
            return ResponseEntity.internalServerError().body(listOf("ERROR"))
        }
        val elements = doc.select("a.WebResult-module__title___MOBFg")
        if (elements.size == 0) {
            return ResponseEntity.notFound().build()
        }
        val products = elements.take(3).map(this::removeUnwantedStrings).distinct()
        eansRepository.insert(Ean(ean, products))
        return ResponseEntity.ok(products)
    }

    private fun removeUnwantedStrings(element: Element) : String {
        return element.text().replace(unwantedStringsRegex, "").trim()
    }

    private fun isValidEan(ean: String) : Boolean {
        val data = padEan(ean)
        if (!validEanPattern.matches(data)) {
            return false
        }
        return calculateCheckDigit(data)
    }

    private fun calculateCheckDigit(data: String) : Boolean {
        var sum = 0
        for (i in data.indices) {
            sum += if (i % 2 == 0) {
                data[i].digitToInt()*3
            } else data[i].digitToInt()
        }
        return sum % 10 == 0
    }

    private fun padEan(ean: String) : String {
        return ean.padStart(14, '0')
    }
}