package com.yamp.data.remote.itunes

import com.yamp.data.remote.itunes.dto.ITunesSearchResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface ITunesSearchApi {

    @GET("search")
    suspend fun search(
        @Query("term") term: String,
        @Query("media") media: String = "music",
        @Query("entity") entity: String = "song",
        @Query("limit") limit: Int = 5
    ): ITunesSearchResponse
}
