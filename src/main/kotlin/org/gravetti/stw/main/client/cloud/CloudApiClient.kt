package org.gravetti.stw.main.client.cloud

import com.fasterxml.jackson.databind.ObjectMapper
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import retrofit2.http.GET

interface CloudApiClient {
    @GET("/genericinfo/keycloak/")
    fun healthCheck(): Call<Unit>
}

fun getCloudApiClient(cloudPath: String, mapper: ObjectMapper): CloudApiClient {
    val retrofit = Retrofit.Builder()
        .baseUrl(cloudPath)
        .addConverterFactory(JacksonConverterFactory.create(mapper))
        .build()
    return retrofit.create(CloudApiClient::class.java)
}
