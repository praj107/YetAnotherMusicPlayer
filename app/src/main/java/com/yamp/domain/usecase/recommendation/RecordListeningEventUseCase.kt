package com.yamp.domain.usecase.recommendation

import com.yamp.data.repository.ListeningHistoryRepository
import com.yamp.domain.model.PlayEvent
import java.util.Calendar
import javax.inject.Inject

class RecordListeningEventUseCase @Inject constructor(
    private val historyRepository: ListeningHistoryRepository
) {
    suspend operator fun invoke(
        trackId: Long,
        durationListened: Long,
        trackDuration: Long
    ) {
        val now = Calendar.getInstance()
        val completed = durationListened >= (trackDuration * 0.90)
        historyRepository.recordPlayEvent(
            PlayEvent(
                trackId = trackId,
                timestamp = System.currentTimeMillis(),
                durationListened = durationListened,
                completed = completed,
                hourOfDay = now.get(Calendar.HOUR_OF_DAY),
                dayOfWeek = now.get(Calendar.DAY_OF_WEEK)
            )
        )
    }
}
