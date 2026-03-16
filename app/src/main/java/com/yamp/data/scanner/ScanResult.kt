package com.yamp.data.scanner

import com.yamp.data.local.db.entity.TrackEntity

data class ScanResult(
    val tracks: List<TrackEntity>,
    val folderPaths: Set<String>
)
