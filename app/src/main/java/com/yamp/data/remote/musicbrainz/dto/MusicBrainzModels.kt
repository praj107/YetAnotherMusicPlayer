package com.yamp.data.remote.musicbrainz.dto

import com.google.gson.annotations.SerializedName

data class MBRecordingResponse(
    val recordings: List<MBRecording>? = null
)

data class MBRecording(
    val id: String,
    val title: String,
    val score: Int = 0,
    @SerializedName("artist-credit")
    val artistCredit: List<MBArtistCredit> = emptyList(),
    val releases: List<MBRelease>? = null
)

data class MBArtistCredit(
    val artist: MBArtist
)

data class MBArtist(
    val id: String,
    val name: String
)

data class MBRelease(
    val id: String,
    val title: String,
    val date: String? = null,
    @SerializedName("release-group")
    val releaseGroup: MBReleaseGroup? = null
)

data class MBReleaseGroup(
    val id: String,
    @SerializedName("primary-type")
    val primaryType: String? = null
)
