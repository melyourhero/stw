package org.github.mamoru1234.stw.ext
import mu.KotlinLogging
import retrofit2.Call
import retrofit2.Response

private val log = KotlinLogging.logger {}

fun <T>Call<T>.execRetry(delay: Int): Response<T> {
    while (true) {
        try {
            val response = this.clone().execute()
            if (response.isSuccessful) {
                return response
            }
        } catch (e: Exception) {
        }
        Thread.sleep(delay.toLong())
        log.debug("Retrying call")
    }
}
