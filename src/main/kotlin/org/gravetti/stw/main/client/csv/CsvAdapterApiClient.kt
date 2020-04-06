package org.gravetti.stw.main.client.csv

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import retrofit2.http.*

interface CsvAdapterApiClient {
    companion object {
        fun getClient(adapterURl: String, mapper: ObjectMapper): CsvAdapterApiClient {
            val retrofit = Retrofit.Builder()
                .baseUrl(adapterURl)
                .addConverterFactory(JacksonConverterFactory.create(mapper))
                .build()
            return retrofit.create(CsvAdapterApiClient::class.java)
        }
    }
    @GET("/files")
    fun getAllFiles(): Call<List<JsonNode>>

    @POST("/files")
    @Multipart
    @Streaming
    fun uploadFile(
        @Part data: MultipartBody.Part
    ): Call<String>

    @POST("/devices")
    fun attachDevice(@Body body: AttachDeviceRequest): Call<Unit>
}
