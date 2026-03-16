package com.yamp.domain.usecase.scan

import com.yamp.data.repository.ScannerRepository
import javax.inject.Inject

class ScanDeviceUseCase @Inject constructor(
    private val scannerRepository: ScannerRepository
) {
    suspend operator fun invoke(): Int = scannerRepository.scanAndPersist()
}
