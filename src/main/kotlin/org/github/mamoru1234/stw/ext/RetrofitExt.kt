package org.github.mamoru1234.stw.ext
import mu.KotlinLogging
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Response
import java.io.File


private val log = KotlinLogging.logger {}

fun <T>Call<T>.execRetry(delay: Int = 20000): Response<T> {
    while (true) {
        try {
            val response = this.clone().execute()
            if (response.isSuccessful) {
                return response
            }
        } catch (e: Exception) {
        }
        Thread.sleep(delay.toLong())
        val request = this.request()
        log.debug("Retrying call [${request.method()}]: ${request.url()}")
    }
}

fun createPartFromFile(partName: String, file: File, type: MediaType): MultipartBody.Part {
    val requestFile = RequestBody.create(
        type,
        file
    )
    return MultipartBody.Part.createFormData(partName, file.name, requestFile)
}
