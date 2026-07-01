package com.taleson2wheels.app.ui.moderation

import com.taleson2wheels.app.data.remote.ApiResult
import com.taleson2wheels.app.data.remote.dto.Page
import com.taleson2wheels.app.data.remote.dto.RegistrationModeration
import com.taleson2wheels.app.data.repository.AdminRepository

/**
 * The core-member registration queue: pending ride registrations to
 * approve/reject (`GET`/`POST /api/v1/admin/registrations`). All queue behaviour
 * (pagination, optimistic actions) lives in [ModerationQueueViewModel].
 */
class ModerationViewModel(
    private val adminRepository: AdminRepository,
) : ModerationQueueViewModel<RegistrationModeration>() {

    override suspend fun fetchPage(cursor: String?): ApiResult<Page<RegistrationModeration>> =
        adminRepository.registrations(status = STATUS, cursor = cursor, limit = PAGE_SIZE)

    override fun idOf(item: RegistrationModeration): String = item.id

    override suspend fun applyModeration(id: String, approve: Boolean): ApiResult<*> =
        adminRepository.moderateRegistration(id, approve)

    init {
        refresh()
    }

    private companion object {
        const val PAGE_SIZE = 20
        const val STATUS = "pending"
    }
}
