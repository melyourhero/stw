package org.gravetti.stw.main.ext
import mu.KotlinLogging
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Response
import java.io.File


private val log = KotlinLogging.logger {}

fun <T>Call<T>.execRetry(delay: Int = 20000): Response<T> {
    val request = this.request()
    while (true) {
        try {
            log.debug("Executing call [${request.method()}]: ${request.url()}")
            val response = this.clone().execute()
            if (response.isSuccessful) {
                return response
            }
        } catch (e: Exception) {
        }
        log.debug("Failed call [${request.method()}]: ${request.url()} retrying...")
        Thread.sleep(delay.toLong())
    }
}

fun createPartFromFile(partName: String, file: File, type: MediaType): MultipartBody.Part {
    val requestFile = RequestBody.create(
        type,
        file
    )
    return MultipartBody.Part.createFormData(partName, file.name, requestFile)
}
