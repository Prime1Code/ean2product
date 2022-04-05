package com.majaku.ean2product.endpoint

import com.majaku.ean2product.domain.Ean
import com.majaku.ean2product.repository.EansRepository
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/ean")
class EanEndpoint @Autowired constructor(
    val eansRepository: EansRepository
) {
    private val unwantedCharsRegex = Regex("\"|\\?|\\\\|(\\.){3}|\\|")
    private val validEanPattern = Regex("^[0-9]{8}\$|^[0-9]{12}\$|^[0-9]{13}\$|^[0-9]{14}\$")

    @GetMapping("{ean}")
    fun getEan(@PathVariable("ean") ean: String): ResponseEntity<List<String>> {
        if (!isValidEan(ean)) {
            return ResponseEntity.badRequest().body(listOf("Error - EAN not valid"))
        }
        val url = "https://api.qwant.com/v3/search/web?count=10&locale=de_de&offset=0&q=$ean"
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:98.0) Gecko/20100101 Firefox/98.0")
            .build()
        var jsonResult: JSONObject
        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                println(response)
                return ResponseEntity
                    .internalServerError()
                    .body(listOf("ERROR"))
            }
            jsonResult = JSONObject(response.body!!.string())
        }

        val root = jsonResult.getJSONObject("data")
        val result = root.getJSONObject("result")
            .getJSONObject("items")
            .getJSONArray("mainline")
            .getJSONObject(1)
            .getJSONArray("items")
        if (result.isEmpty) {
            return ResponseEntity
                .notFound()
                .build()
        }
        val productHashMap = result.toList() as ArrayList<HashMap<String,String>>
        val products = productHashMap.take(3).map { t -> t["title"]!! }.map(this::removeUnwantedChars).distinct()
        eansRepository.insert(Ean(ean, products))
        return ResponseEntity.ok(products)
    }

    private fun removeUnwantedChars(element: String): String {
        return element
            .replaceAfter(" - ", "")
            .replace(" -", "")
            .replaceAfter(" | ", "")
            .replace(" |", "")
            .replace(unwantedCharsRegex, "")
            .replace("  ", " ")
            .trimEnd('.')
            .trim()
    }

    private fun isValidEan(ean: String): Boolean {
        val data = padEan(ean)
        if (!validEanPattern.matches(data)) {
            return false
        }
        return calculateCheckDigit(data)
    }

    private fun calculateCheckDigit(data: String): Boolean {
        var sum = 0
        for (i in data.indices) {
            sum += if (i % 2 == 0) {
                data[i].digitToInt() * 3
            } else data[i].digitToInt()
        }
        return sum % 10 == 0
    }

    private fun padEan(ean: String): String {
        return ean.padStart(14, '0')
    }

    companion object {
        val httpClient: OkHttpClient = OkHttpClient()
    }
}