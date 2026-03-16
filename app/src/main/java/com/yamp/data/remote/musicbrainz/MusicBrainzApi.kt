package com.yamp.data.remote.musicbrainz

import com.yamp.data.remote.musicbrainz.dto.MBRecording
import com.yamp.data.remote.musicbrainz.dto.MBRecordingResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface MusicBrainzApi {

    @GET("recording/")
    suspend fun searchRecordings(
        @Query("query") query: String,
        @Query("fmt") format: String = "json",
        @Query("limit") limit: Int = 5
    ): MBRecordingResponse

    @GET("recording/{mbid}")
    suspend fun getRecording(
        @Path("mbid") mbid: String,
        @Query("inc") includes: String = "artists+releases",
        @Query("fmt") format: String = "json"
    ): MBRecording
}
